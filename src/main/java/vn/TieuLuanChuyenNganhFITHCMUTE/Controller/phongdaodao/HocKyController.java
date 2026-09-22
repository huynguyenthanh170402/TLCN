package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;
import java.time.LocalDateTime;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;

/** Phòng Đào tạo: quản lý học kỳ và mở/đóng đăng ký học phần */
@Controller
@RequestMapping("/quanly")
@RequiredArgsConstructor
public class HocKyController {

	/** Định dạng của ô input type="datetime-local" */
	private static final String DINH_DANG_NGAY_GIO = "yyyy-MM-dd'T'HH:mm";

	private final HocKyService hocKyService;

	/** /quanly → trang học kỳ (trang mặc định của Phòng Đào tạo) */
	@GetMapping
	public String trangChu() {
		return "redirect:/quanly/hocky";
	}

	@GetMapping("/hocky")
	public String dsHocKy(Model model) {
		model.addAttribute("menu", "hocky");
		model.addAttribute("dsHocKy", hocKyService.tatCa());
		return "phongdaotao/hocky";
	}

	@PostMapping("/hocky/{id}/thoigian")
	public String datThoiGian(@PathVariable Integer id,
	                          @RequestParam @DateTimeFormat(pattern = DINH_DANG_NGAY_GIO) LocalDateTime mo,
	                          @RequestParam @DateTimeFormat(pattern = DINH_DANG_NGAY_GIO) LocalDateTime dong,
	                          RedirectAttributes ra) {
		try {
			hocKyService.datThoiGianDangKy(id, mo, dong);
			ra.addFlashAttribute("thongBao", "Đã cập nhật thời gian đăng ký.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/hocky";
	}

	@PostMapping("/hocky/{id}/mo")
	public String moDangKy(@PathVariable Integer id, RedirectAttributes ra) {
		try {
			hocKyService.moNgay(id);
			ra.addFlashAttribute("thongBao", "Đã mở đăng ký học phần.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/hocky";
	}

	@PostMapping("/hocky/{id}/dong")
	public String dongDangKy(@PathVariable Integer id, RedirectAttributes ra) {
		try {
			hocKyService.dongNgay(id);
			ra.addFlashAttribute("thongBao", "Đã đóng đăng ký học phần.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/hocky";
	}
}