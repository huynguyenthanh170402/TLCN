package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
			.authorizeHttpRequests(auth -> auth
				// Trang guest, ai cũng vào được
				.requestMatchers("/", "/home", "/about", "/faculty", "/major",
				                 "/center", "/news", "/research").permitAll()
				.requestMatchers("/css/**", "/js/**", "/images/**",
				                 "/uploads/**", "/favicon.ico").permitAll()
				.requestMatchers("/login", "/logout", "/403").permitAll()

				// Khu vực riêng theo quyền
				.requestMatchers("/sinhvien/**").hasRole("SINH_VIEN")
				.requestMatchers("/giangvien/**").hasRole("GIANG_VIEN")
				.requestMatchers("/quanly/**").hasAnyRole("CHUYEN_VIEN", "PHONG_BAN")
				.requestMatchers("/baocao/**").hasAnyRole("BAN_GIAM_HIEU", "PHONG_BAN")

				.anyRequest().authenticated()
			)
			.formLogin(login -> login
				.loginPage("/login")
				.loginProcessingUrl("/login")
				.usernameParameter("username")
				.passwordParameter("password")
				.defaultSuccessUrl("/dashboard", true)
				.failureUrl("/login?error")
				.permitAll()
			)
			.logout(logout -> logout
				.logoutUrl("/logout")
				.logoutSuccessUrl("/login?logout")
				.invalidateHttpSession(true)
				.deleteCookies("JSESSIONID")
				.permitAll()
			)
			.exceptionHandling(ex -> ex.accessDeniedPage("/403"));

		return http.build();
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}