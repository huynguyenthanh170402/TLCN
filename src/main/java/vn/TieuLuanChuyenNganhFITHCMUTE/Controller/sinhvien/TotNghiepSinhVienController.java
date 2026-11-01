package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.sinhvien;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThongBaoService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.XetTotNghiepService;

/** Sinh viên tra điều kiện tốt nghiệp và nộp đơn xét */
@Controller
@RequestMapping("/sinhvien/totnghiep")
@RequiredArgsConstructor
public class TotNghiepSinhVienController {

	private final XetTotNghiepService xetService;
	private final SinhVienRepository sinhVienRepository;
	private final ThongBaoService thongBaoService;

	@ModelAttribute("sinhVien")
	public SinhVien sinhVienHienTai(Principal principal) {
		if (principal == null) return null;
		return sinhVienRepository.timTheoTenDangNhap(principal.getName()).orElse(null);
	}

	@ModelAttribute("soThongBaoChuaDoc")
	public long soThongBaoChuaDoc(@ModelAttribute("sinhVien") SinhVien sv) {
		if (sv == null) return 0;
		return thongBaoService.soChuaDoc(sv.getTaiKhoan());
	}

	@GetMapping
	public String trang(@ModelAttribute("sinhVien") SinhVien sv, Model model) {
		model.addAttribute("menu", "totnghiep");
		if (sv == null) return "sinhvien/totnghiep";

		model.addAttribute("ketQua", xetService.xet(sv));
		model.addAttribute("dsDot", xetService.dsDotDangNhanDon());
		model.addAttribute("dsDon", xetService.donCuaSinhVien(sv));
		return "sinhvien/totnghiep";
	}

	@PostMapping("/nopdon/{maDot}")
	public String nopDon(@ModelAttribute("sinhVien") SinhVien sv,
	                     @PathVariable Integer maDot,
	                     RedirectAttributes ra) {
		try {
			xetService.nopDon(sv, maDot);
			ra.addFlashAttribute("thongBao",
					"Đã nộp đơn xét tốt nghiệp. Chờ Phòng Đào tạo xét và công bố kết quả.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/sinhvien/totnghiep";
	}
}
