package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Controller.DoiMatKhauTamController;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/**
 * Chặn mọi yêu cầu của người đang dùng mật khẩu tạm, buộc họ đổi mật khẩu trước.
 *
 * Giấu nút trên giao diện là chưa đủ: chỉ cần gõ thẳng đường dẫn là vào được.
 * Chặn ở tầng này thì mọi lối vào đều đi qua một chỗ duy nhất.
 */
@Configuration
@RequiredArgsConstructor
public class MatKhauTamConfig implements WebMvcConfigurer {

	private final TaiKhoanRepository taiKhoanRepository;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(new ChanMatKhauTam(taiKhoanRepository))
				.addPathPatterns("/**")
				.excludePathPatterns(
						// Chính trang đổi mật khẩu, nếu chặn luôn thì thành vòng lặp
						"/doimatkhautam",
						// Trang khách và đăng nhập, đăng xuất
						"/", "/home", "/about", "/faculty", "/major", "/center",
						"/news", "/research", "/login", "/logout",
						"/quenmatkhau", "/403", "/error",
						// Tài nguyên tĩnh
						"/css/**", "/js/**", "/images/**", "/uploads/**",
						"/webjars/**", "/favicon.ico");
	}

	@RequiredArgsConstructor
	static class ChanMatKhauTam implements HandlerInterceptor {

		private final TaiKhoanRepository taiKhoanRepository;

		@Override
		public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler) {

			Authentication auth = SecurityContextHolder.getContext().getAuthentication();
			if (auth == null || !auth.isAuthenticated()
					|| "anonymousUser".equals(auth.getPrincipal())) {
				return true;
			}

			// Đã đổi trong phiên này rồi thì khỏi hỏi lại cơ sở dữ liệu
			var session = req.getSession(false);
			if (session != null
					&& Boolean.TRUE.equals(session.getAttribute(DoiMatKhauTamController.CO_DA_DOI))) {
				return true;
			}

			TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(auth.getName()).orElse(null);
			if (tk == null || !tk.dangDungMatKhauTam()) {
				if (session != null) {
					session.setAttribute(DoiMatKhauTamController.CO_DA_DOI, Boolean.TRUE);
				}
				return true;
			}

			try {
				res.sendRedirect(req.getContextPath() + "/doimatkhautam");
			} catch (Exception boQua) {
				// Trình duyệt đã ngắt kết nối, không cần làm gì thêm
			}
			return false;
		}
	}
}
