package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.session.HttpSessionEventPublisher;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.VaiTro;

/**
 * Cấu hình bảo mật của toàn hệ thống.
 *
 * Ba việc chính:
 *   1. Trang nào ai xem được (authorizeHttpRequests)
 *   2. Đăng nhập ở đâu, xong thì đi đâu (formLogin)
 *   3. Phiên làm việc sống bao lâu (sessionManagement)
 *
 * Lưu ý: bean PasswordEncoder nằm ở MatKhauConfig, không khai báo lại ở đây
 * để tránh lỗi trùng bean.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	/** Khóa lưu tạm đường dẫn bị chặn trong phiên, để trang /403 đọc lại */
	public static final String DUONG_DAN_BI_CHAN = "duongDanBiChan";

	// Lấy tên vai trò từ enum thay vì gõ chuỗi, sửa enum là đổi theo.
	private static final String SINH_VIEN   = VaiTro.SINH_VIEN.name();
	private static final String GIANG_VIEN  = VaiTro.GIANG_VIEN.name();
	private static final String DAO_TAO     = VaiTro.PHONG_DAO_TAO.name();
	private static final String TAI_CHINH   = VaiTro.PHONG_KE_HOACH_TAI_CHINH.name();
	private static final String CTSV        = VaiTro.PHONG_CONG_TAC_SINH_VIEN.name();
	private static final String BAN_GIAM_HIEU = VaiTro.BAN_GIAM_HIEU.name();

	@Bean
	public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

		http
			.authorizeHttpRequests(quyen -> quyen

				// ---------- Trang khách: ai cũng xem được ----------
				.requestMatchers(
						"/", "/home", "/about",
						// Khoa và ngành có trang chi tiết, nên mở cả nhánh con.
						// Thiếu /** thì bấm vào một khoa sẽ bị đá về đăng nhập.
						"/faculty", "/faculty/**", "/major", "/major/**",
						"/center", "/news", "/research",
						"/login", "/logout",
						"/quenmatkhau",
						"/403", "/error"
				).permitAll()

				// Tài nguyên tĩnh. Thiếu dòng này thì trang đăng nhập mất sạch CSS.
				.requestMatchers(
						"/css/**", "/js/**", "/images/**", "/uploads/**",
						"/webjars/**", "/favicon.ico"
				).permitAll()

				// ---------- Khu riêng từng vai trò ----------
				// Thứ tự rất quan trọng: Spring xét từ trên xuống, khớp dòng
				// nào thì dừng ở đó. Đường dẫn hẹp phải đặt trước đường dẫn rộng.
				.requestMatchers("/sinhvien/**").hasRole(SINH_VIEN)
				.requestMatchers("/giangvien/**").hasRole(GIANG_VIEN)

				// /quanly/hocphi là của Tài chính, đặt TRƯỚC /quanly/**
				// nếu không sẽ bị dòng /quanly/** nuốt mất.
				.requestMatchers("/quanly/hocphi/**").hasRole(TAI_CHINH)
				.requestMatchers("/taichinh/**").hasRole(TAI_CHINH)

				.requestMatchers("/ctsv/**").hasRole(CTSV)
				.requestMatchers("/quanly/**").hasRole(DAO_TAO)

				// Ban Giám hiệu CHỈ vào khu báo cáo. Những khu trên là màn hình
				// nghiệp vụ có nút thêm, sửa, xóa, duyệt; cho cấp trên vào đó
				// là phá nguyên tắc mỗi dữ liệu chỉ một nơi chịu trách nhiệm.
				.requestMatchers("/baocao/**").hasRole(BAN_GIAM_HIEU)

				// Còn lại phải đăng nhập: /dashboard và /doimatkhautam
				.anyRequest().authenticated()
			)

			// ---------- Đăng nhập ----------
			.formLogin(form -> form
				// Dòng này bảo Spring Security đừng tự vẽ form nữa.
				// Thiếu nó là hiện trang trắng "Please sign in".
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("username")
				.passwordParameter("password")
				// true: luôn về /dashboard, kể cả khi trước đó đang mở trang khác.
				// AuthController nhìn vai trò rồi chuyển tiếp đúng khu.
				.defaultSuccessUrl("/dashboard", true)
				/*
				 * Phân biệt hai kiểu đăng nhập hỏng:
				 *   - Mật khẩu tạm đúng nhưng quá 5 phút  → CredentialsExpiredException
				 *   - Sai tài khoản hoặc sai mật khẩu     → BadCredentialsException
				 * Gộp chung một thông báo thì người dùng gõ đúng mật khẩu trong
				 * thư mà vẫn bị báo "sai mật khẩu", không hiểu vì sao.
				 */
				.failureHandler((req, res, ex) -> res.sendRedirect(
						(ex instanceof CredentialsExpiredException)
								? "/login?hethanmk"
								: "/login?error"))
				.permitAll()
			)

			// ---------- Đăng xuất ----------
			.logout(out -> out
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll()
			)

			/*
			 * ---------- Bị từ chối truy cập ----------
			 *
			 * Mặc định Spring Security chỉ trả mã 403 rỗng, Spring Boot thấy
			 * không có trang nào nhận nên đổ ra Whitelabel Error Page trắng
			 * trơn. Người dùng không biết chuyện gì, người lập trình cũng
			 * không biết vì sao bị chặn.
			 *
			 * Tách làm hai nhánh vì hai nguyên nhân cần hai cách xử lý khác
			 * hẳn nhau:
			 *
			 *   - CsrfException: thẻ bài trong biểu mẫu không khớp với phiên,
			 *     thường do máy chủ vừa khởi động lại trong khi trình duyệt
			 *     còn giữ trang cũ. Đăng nhập lại là xong, không phải chuyện
			 *     phân quyền.
			 *
			 *   - Còn lại: tài khoản thật sự không có quyền. Đưa sang trang
			 *     /403 nói rõ đã mở đường dẫn nào và đang có quyền gì.
			 */
			.exceptionHandling(loi -> loi
				.accessDeniedHandler((req, res, ex) -> {
					if (ex instanceof CsrfException) {
						res.sendRedirect(req.getContextPath() + "/login?phienhet");
						return;
					}
					// Giữ lại đường dẫn bị chặn để trang /403 hiển thị,
					// vì sau khi chuyển hướng thì không còn đọc được nữa.
					req.getSession().setAttribute(DUONG_DAN_BI_CHAN, req.getRequestURI());
					res.sendRedirect(req.getContextPath() + "/403");
				})
			)

			// ---------- Phiên làm việc ----------
			.sessionManagement(phien -> phien
				.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
				// Đổi mã phiên ngay sau khi đăng nhập, chặn kiểu tấn công
				// gán sẵn mã phiên cho nạn nhân (session fixation).
				.sessionFixation(sf -> sf.newSession())
				// Hết 30 phút thì quay về trang đăng nhập kèm thông báo.
				.invalidSessionUrl("/login?expired")
				// Một tài khoản chỉ giữ một phiên. Đăng nhập chỗ mới thì
				// chỗ cũ bị đá ra.
				.maximumSessions(1)
				.expiredUrl("/login?expired")
			);

		return http.build();
	}

	/**
	 * Cần bean này thì maximumSessions mới đếm đúng: nó báo cho Spring
	 * Security biết khi nào một phiên bị hủy để xóa khỏi sổ theo dõi.
	 */
	@Bean
	public HttpSessionEventPublisher httpSessionEventPublisher() {
		return new HttpSessionEventPublisher();
	}
}
