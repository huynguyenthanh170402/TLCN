package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.congtacsinhvien;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MienGiamService;

/** Trang chủ của Phòng Công tác Sinh viên */
@Controller
@RequestMapping("/ctsv")
@RequiredArgsConstructor
public class CongTacSinhVienController {

	private final MienGiamService mienGiamService;

	@GetMapping
	public String trangChu(Model model) {
		model.addAttribute("menu", "tongquan");
		model.addAttribute("homNay", LocalDate.now());
		model.addAttribute("soChoDuyet", mienGiamService.soHoSoChoDuyet());
		return "congtacsinhvien/trangchu";
	}
}
