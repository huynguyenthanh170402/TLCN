package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.util.Set;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {

	@GetMapping("/login")
	public String login(Model model) {
		model.addAttribute("page", "login");
		return "guest/login";
	}

	/** Sau khi đăng nhập, chuyển tới khu vực riêng theo quyền */
	@GetMapping("/dashboard")
	public String dashboard(Authentication auth) {
		if (auth == null) return "redirect:/login";

		Set<String> roles = AuthorityUtils.authorityListToSet(auth.getAuthorities());

		if (roles.contains("ROLE_SINH_VIEN"))     return "redirect:/sinhvien";
		if (roles.contains("ROLE_GIANG_VIEN"))    return "redirect:/giangvien";
		if (roles.contains("ROLE_CHUYEN_VIEN")
		 || roles.contains("ROLE_PHONG_BAN"))     return "redirect:/quanly";
		if (roles.contains("ROLE_BAN_GIAM_HIEU")) return "redirect:/baocao";

		return "redirect:/";
	}
}