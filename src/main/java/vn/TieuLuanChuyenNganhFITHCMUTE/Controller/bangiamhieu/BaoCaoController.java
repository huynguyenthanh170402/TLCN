package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.bangiamhieu;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.BaoCaoService;

/** Ban Giám hiệu: trang báo cáo thống kê, chỉ xem */
@Controller
@RequestMapping("/baocao")
@RequiredArgsConstructor
public class BaoCaoController {

	private final BaoCaoService baoCaoService;

	@GetMapping
	public String tongQuan(Principal principal, Model model) {
		model.addAttribute("menu", "tongquan");
		model.addAttribute("tenDangNhap", principal != null ? principal.getName() : "");
		model.addAttribute("bc", baoCaoService.tongQuan());
		return "bangiamhieu/tongquan";
	}
}
