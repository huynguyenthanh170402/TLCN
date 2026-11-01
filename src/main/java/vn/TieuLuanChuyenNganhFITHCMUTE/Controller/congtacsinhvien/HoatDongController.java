package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.congtacsinhvien;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HoatDongCTXH;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.RenLuyenService;

/** Phòng Công tác Sinh viên mở hoạt động công tác xã hội và chấm điểm rèn luyện */
@Controller
@RequestMapping("/ctsv/hoatdong")
@RequiredArgsConstructor
public class HoatDongController {

	private final RenLuyenService renLuyenService;
	private final HocKyService hocKyService;

	/** Nạp danh sách học kỳ và chọn sẵn học kỳ đang diễn ra */
	private Integer napHocKy(Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa();
		model.addAttribute("dsHocKy", dsHocKy);

		Integer maHocKy = hocKy;
		if (maHocKy == null) {
			HocKy hienTai = renLuyenService.hocKyHienTai();
			maHocKy = hienTai != null ? hienTai.getMaHocKy()
					: (dsHocKy.isEmpty() ? null : dsHocKy.get(0).getMaHocKy());
		}
		model.addAttribute("hocKyChon", maHocKy);
		return maHocKy;
	}

	// ===================== DANH SÁCH =====================

	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer hocKy, Model model) {
		model.addAttribute("menu", "hoatdong");
		Integer maHocKy = napHocKy(hocKy, model);

		if (maHocKy != null) {
			List<HoatDongCTXH> ds = renLuyenService.dsHoatDong(maHocKy);
			model.addAttribute("ds", ds);
			model.addAttribute("soDaDangKy", renLuyenService.soDaDangKyCacHoatDong(ds));
		}
		return "congtacsinhvien/hoatdong";
	}

	// ===================== TẠO HOẠT ĐỘNG =====================

	@GetMapping("/them")
	public String formThem(@RequestParam(required = false) Integer hocKy, Model model) {
		model.addAttribute("menu", "hoatdong");
		napHocKy(hocKy, model);
		model.addAttribute("diemMacDinh", 5);
		return "congtacsinhvien/hoatdong-them";
	}

	@PostMapping("/them")
	public String them(@RequestParam Integer maHocKy,
	                   @RequestParam String tenHoatDong,
	                   @RequestParam(required = false) String moTa,
	                   @RequestParam(defaultValue = "1") Integer loaiHoatDong,
	                   @RequestParam Integer diemCong,
	                   @RequestParam(required = false) String donViToChuc,
	                   @RequestParam(required = false) String diaDiem,
	                   @RequestParam(required = false)
	                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime thoiGianBatDau,
	                   @RequestParam(required = false)
	                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime thoiGianKetThuc,
	                   @RequestParam(required = false)
	                   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hanDangKy,
	                   @RequestParam(required = false) Integer soLuongToiDa,
	                   Principal principal,
	                   RedirectAttributes ra) {
		try {
			HoatDongCTXH hd = renLuyenService.tao(maHocKy, tenHoatDong, moTa, loaiHoatDong,
					diemCong, donViToChuc, diaDiem, thoiGianBatDau, thoiGianKetThuc,
					hanDangKy, soLuongToiDa,
					principal == null ? null : principal.getName());

			ra.addFlashAttribute("thongBao", "Đã tạo hoạt động. Hoạt động đang ở trạng thái "
					+ "Nháp, bấm \"Mở đăng ký\" để sinh viên thấy và đăng ký được.");
			return "redirect:/ctsv/hoatdong/" + hd.getMaHoatDong();

		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/ctsv/hoatdong/them?hocKy=" + maHocKy;
		}
	}

	// ===================== CHI TIẾT =====================

	@GetMapping("/{maHoatDong}")
	public String chiTiet(@PathVariable Integer maHoatDong, Model model, RedirectAttributes ra) {
		try {
			HoatDongCTXH hd = renLuyenService.timHoatDong(maHoatDong);
			model.addAttribute("menu", "hoatdong");
			model.addAttribute("hd", hd);
			model.addAttribute("ds", renLuyenService.dsThamGia(maHoatDong));
			model.addAttribute("soDaDangKy", renLuyenService.soDaDangKy(maHoatDong));
			return "congtacsinhvien/hoatdong-chitiet";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/ctsv/hoatdong";
		}
	}

	// ===================== THAO TÁC =====================

	@PostMapping("/{maHoatDong}/trangthai")
	public String doiTrangThai(@PathVariable Integer maHoatDong,
	                           @RequestParam int trangThai,
	                           RedirectAttributes ra) {
		try {
			renLuyenService.doiTrangThai(maHoatDong, trangThai);
			ra.addFlashAttribute("thongBao", "Đã cập nhật trạng thái hoạt động.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/hoatdong/" + maHoatDong;
	}

	@PostMapping("/{maHoatDong}/them-sinhvien")
	public String themSinhVien(@PathVariable Integer maHoatDong,
	                           @RequestParam String maSoSinhVien,
	                           RedirectAttributes ra) {
		try {
			renLuyenService.themSinhVien(maHoatDong, maSoSinhVien);
			ra.addFlashAttribute("thongBao", "Đã thêm sinh viên vào danh sách.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/hoatdong/" + maHoatDong;
	}

	@PostMapping("/{maHoatDong}/diemdanh")
	public String diemDanh(@PathVariable Integer maHoatDong,
	                       @RequestParam(required = false) List<Integer> coMat,
	                       Principal principal,
	                       RedirectAttributes ra) {
		try {
			int soDoi = renLuyenService.diemDanhHangLoat(maHoatDong, coMat,
					principal == null ? null : principal.getName());

			// Nói rõ người không tick bị ghi vắng, tránh hiểu nhầm là chưa chấm
			ra.addFlashAttribute("thongBao", "Đã điểm danh " + soDoi + " sinh viên. "
					+ "Người không được đánh dấu bị ghi là vắng mặt và không được cộng điểm.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/hoatdong/" + maHoatDong;
	}
}
