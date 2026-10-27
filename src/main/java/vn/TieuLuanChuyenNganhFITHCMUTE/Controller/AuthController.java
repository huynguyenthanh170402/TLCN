package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.security.Principal;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.VaiTro;

/**
 * Trang đăng nhập và ngã ba điều hướng sau khi đăng nhập.
 *
 * Gộp chung một lớp vì hai việc này đi liền nhau: người dùng đăng nhập ở
 * /login, Spring Security đưa họ sang /dashboard, chỗ này nhìn vai trò rồi
 * chuyển tiếp về trang chủ tương ứng.
 *
 * Nhờ vậy cấu hình chỉ cần một địa chỉ sau đăng nhập
 * (defaultSuccessUrl("/dashboard", true)), không phải viết sáu nhánh rải rác.
 */
@Controller
@Slf4j
public class AuthController {

	@GetMapping("/login")
	public String login(Model model) {
		model.addAttribute("page", "login");
		return "guest/login";
	}

	@GetMapping("/dashboard")
	public String dieuHuong(Authentication auth, Principal principal) {

		if (auth == null) {
			return "redirect:/login";
		}

		VaiTro vaiTro = layVaiTro(auth);

		if (vaiTro == null) {
			log.warn("Tài khoản {} không có vai trò hợp lệ", principal.getName());
			return "redirect:/403";
		}

		return switch (vaiTro) {
			case SINH_VIEN                -> "redirect:/sinhvien/hoso";
			case GIANG_VIEN               -> "redirect:/giangvien/lop";
			case PHONG_DAO_TAO            -> "redirect:/quanly/sinhvien";
			case PHONG_KE_HOACH_TAI_CHINH -> "redirect:/taichinh";
			case PHONG_CONG_TAC_SINH_VIEN -> "redirect:/ctsv";
			case BAN_GIAM_HIEU            -> "redirect:/baocao/tongquan";
		};
	}

	/**
	 * Đọc vai trò từ authority. Authority có dạng ROLE_PHONG_DAO_TAO,
	 * cắt bỏ tiền tố ROLE_ rồi tra trong enum.
	 */
	private VaiTro layVaiTro(Authentication auth) {
		for (GrantedAuthority q : auth.getAuthorities()) {
			String ten = q.getAuthority();
			if (ten == null || !ten.startsWith("ROLE_")) continue;
			try {
				return VaiTro.valueOf(ten.substring(5));
			} catch (IllegalArgumentException boQua) {
				// Authority lạ, thử cái tiếp theo
			}
		}
		return null;
	}
}