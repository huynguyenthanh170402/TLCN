package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.sinhvien;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHoatDong;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HoatDongCTXH;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.RenLuyenService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThongBaoService;

/**
 * Sinh viên xem điểm rèn luyện và đăng ký hoạt động công tác xã hội.
 *
 * Controller này tự tra hồ sơ sinh viên thay vì dựa vào lớp dùng chung, vì
 * @ModelAttribute("sinhVien") của SinhVienController chỉ có hiệu lực trong
 * chính lớp đó, không lan sang các controller khác cùng gói.
 */
@Controller
@RequestMapping("/sinhvien/renluyen")
@RequiredArgsConstructor
public class RenLuyenSinhVienController {

	private final RenLuyenService renLuyenService;
	private final SinhVienRepository sinhVienRepository;
	private final ThongBaoService thongBaoService;

	@ModelAttribute("sinhVien")
	public SinhVien sinhVienHienTai(Principal principal) {
		if (principal == null) return null;
		return sinhVienRepository.timTheoTenDangNhap(principal.getName()).orElse(null);
	}

	/** Badge số thông báo chưa đọc trên sidebar, giống các trang khác */
	@ModelAttribute("soThongBaoChuaDoc")
	public long soThongBaoChuaDoc(@ModelAttribute("sinhVien") SinhVien sv) {
		if (sv == null) return 0;
		return thongBaoService.soChuaDoc(sv.getTaiKhoan());
	}

	// ===================== TRANG CHÍNH =====================

	@GetMapping
	public String trang(@ModelAttribute("sinhVien") SinhVien sv, Model model) {
		model.addAttribute("menu", "renluyen");
		if (sv == null) return "sinhvien/renluyen";

		HocKy hk = renLuyenService.hocKyHienTai();
		model.addAttribute("hocKy", hk);

		// Lịch sử các học kỳ trước, để sinh viên thấy cả quá trình
		model.addAttribute("lichSu", renLuyenService.ketQuaMoiHocKy(sv));

		if (hk != null) {
			// Tính một lần rồi dùng lại cho cả ô điểm lẫn phần lọc bên dưới
			var ketQua = renLuyenService.ketQua(sv, hk);
			model.addAttribute("ketQua", ketQua);

			/*
			 * Hoạt động còn đăng ký được: chỉ những cái đang mở và sinh viên
			 * chưa có tên. Hoạt động đã đăng ký nằm ở bảng riêng phía trên,
			 * để ở cả hai nơi thì sinh viên tưởng mình đăng ký hụt.
			 */
			List<DangKyHoatDong> daCoTen = ketQua.getDsThamGia();
			List<Integer> maDaCo = daCoTen.stream()
					.map(dk -> dk.getHoatDong().getMaHoatDong()).toList();

			List<HoatDongCTXH> conMo = new ArrayList<>();
			for (HoatDongCTXH hd : renLuyenService.dsHoatDong(hk.getMaHocKy())) {
				if (hd.isDangMoDangKy() && !maDaCo.contains(hd.getMaHoatDong())) conMo.add(hd);
			}
			model.addAttribute("dsMo", conMo);
			model.addAttribute("soDaDangKy", renLuyenService.soDaDangKyCacHoatDong(conMo));
		}

		return "sinhvien/renluyen";
	}

	// ===================== ĐĂNG KÝ =====================

	@PostMapping("/dangky/{maHoatDong}")
	public String dangKy(@ModelAttribute("sinhVien") SinhVien sv,
	                     @PathVariable Integer maHoatDong,
	                     RedirectAttributes ra) {
		try {
			renLuyenService.sinhVienDangKy(maHoatDong, sv);
			ra.addFlashAttribute("thongBao",
					"Đã đăng ký. Nhớ tham gia đúng giờ, vắng mặt thì không được cộng điểm.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/sinhvien/renluyen";
	}

	@PostMapping("/huy/{maDangKy}")
	public String huy(@ModelAttribute("sinhVien") SinhVien sv,
	                  @PathVariable Integer maDangKy,
	                  RedirectAttributes ra) {
		try {
			renLuyenService.sinhVienHuyDangKy(maDangKy, sv);
			ra.addFlashAttribute("thongBao", "Đã hủy đăng ký hoạt động.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/sinhvien/renluyen";
	}
}
