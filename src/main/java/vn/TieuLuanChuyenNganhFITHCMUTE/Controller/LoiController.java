package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.servlet.http.HttpSession;
import vn.TieuLuanChuyenNganhFITHCMUTE.Config.SecurityConfig;

/**
 * Trang báo lỗi phân quyền.
 *
 * Trước đây bị chặn thì rơi vào Whitelabel Error Page trắng trơn, chỉ có dòng
 * "status=403". Người dùng không biết nên làm gì, còn người lập trình thì
 * phải mò nhật ký mới biết đường dẫn nào bị chặn.
 *
 * Trang này nói thẳng ba điều: đã mở đường dẫn nào, đang đăng nhập bằng tài
 * khoản gì, và tài khoản đó đang có những quyền nào. Nhìn ba dòng đó là đủ
 * biết bị chặn vì sai vai trò hay vì chưa đăng nhập.
 */
@Controller
public class LoiController {

	@GetMapping("/403")
	public String khongCoQuyen(Authentication auth, HttpSession session, Model model) {

		// Đọc xong thì xóa, nếu không lần sau vào thẳng /403 sẽ thấy
		// đường dẫn cũ của lần bị chặn trước và tưởng vừa bị chặn lại.
		Object duongDan = session.getAttribute(SecurityConfig.DUONG_DAN_BI_CHAN);
		session.removeAttribute(SecurityConfig.DUONG_DAN_BI_CHAN);
		model.addAttribute("duongDan", duongDan);

		if (auth != null && auth.isAuthenticated()
				&& !"anonymousUser".equals(auth.getPrincipal())) {
			model.addAttribute("tenDangNhap", auth.getName());

			// Bỏ tiền tố ROLE_ cho dễ đọc: ROLE_SINH_VIEN -> SINH_VIEN
			List<String> quyen = auth.getAuthorities().stream()
					.map(GrantedAuthority::getAuthority)
					.map(q -> q.startsWith("ROLE_") ? q.substring(5) : q)
					.sorted()
					.toList();
			model.addAttribute("dsQuyen", quyen);
		}

		return "guest/403";
	}
}
