package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.congtacsinhvien;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MienGiamService;

/** Lập và duyệt hồ sơ miễn giảm học phí, học bổng */
@Controller
@RequestMapping("/ctsv/miengiam")
@RequiredArgsConstructor
public class MienGiamController {

	private static final int SO_DONG_MOI_TRANG = 10;

	private final MienGiamService mienGiamService;
	private final HocKyRepository hocKyRepository;

	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer maHocKy,
	                       @RequestParam(required = false) Integer trangThai,
	                       @RequestParam(required = false) String tuKhoa,
	                       @RequestParam(defaultValue = "0") int trang,
	                       Model model) {

		model.addAttribute("menu", "miengiam");
		model.addAttribute("dsHocKy",
				hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau")));

		// Giữ lại điều kiện lọc để biểu mẫu hiện đúng cái người dùng vừa chọn
		model.addAttribute("maHocKy", maHocKy);
		model.addAttribute("trangThai", trangThai);
		model.addAttribute("tuKhoa", tuKhoa);

		model.addAttribute("ds", mienGiamService.traCuu(
				maHocKy, trangThai, tuKhoa,
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG)));
		model.addAttribute("soChoDuyet", mienGiamService.soHoSoChoDuyet());

		return "congtacsinhvien/miengiam";
	}

	@PostMapping("/denghi")
	public String deNghi(@RequestParam String maSoSinhVien,
	                     @RequestParam Integer maHocKy,
	                     @RequestParam(defaultValue = "1") Integer loai,
	                     @RequestParam(required = false) String dienHuong,
	                     @RequestParam BigDecimal tyLePhanTram,
	                     @RequestParam(required = false) String ghiChu,
	                     Principal principal,
	                     RedirectAttributes ra) {
		try {
			mienGiamService.deNghi(maSoSinhVien, maHocKy, loai, dienHuong,
					tyLePhanTram, ghiChu, principal.getName());
			ra.addFlashAttribute("thongBao",
					"Đã lập hồ sơ cho sinh viên " + maSoSinhVien + ", đang chờ duyệt.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/miengiam";
	}

	@PostMapping("/duyet/{maMienGiam}")
	public String duyet(@PathVariable Integer maMienGiam,
	                    Principal principal, RedirectAttributes ra) {
		try {
			mienGiamService.duyet(maMienGiam, principal.getName());
			ra.addFlashAttribute("thongBao",
					"Đã duyệt. Phòng Kế hoạch Tài chính sẽ thấy khi tính lại công nợ.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/miengiam";
	}

	@PostMapping("/tuchoi/{maMienGiam}")
	public String tuChoi(@PathVariable Integer maMienGiam,
	                     @RequestParam(required = false) String lyDo,
	                     Principal principal, RedirectAttributes ra) {
		try {
			mienGiamService.tuChoi(maMienGiam, principal.getName(), lyDo);
			ra.addFlashAttribute("thongBao", "Đã từ chối hồ sơ.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/miengiam";
	}

	@PostMapping("/thuhoi/{maMienGiam}")
	public String thuHoi(@PathVariable Integer maMienGiam,
	                     Principal principal, RedirectAttributes ra) {
		try {
			mienGiamService.thuHoi(maMienGiam, principal.getName());
			ra.addFlashAttribute("thongBao",
					"Đã thu hồi quyết định. Nhắc Phòng Kế hoạch Tài chính tính lại công nợ.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/miengiam";
	}
}
