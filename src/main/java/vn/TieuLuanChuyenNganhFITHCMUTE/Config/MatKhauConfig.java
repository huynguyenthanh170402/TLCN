package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Khai báo bộ băm mật khẩu dùng chung cho toàn dự án.
 *
 * Đặt riêng một lớp thay vì để trong SecurityConfig vì có nhiều nơi cần
 * PasswordEncoder: DataSeeder khi tạo dữ liệu mẫu, QuenMatKhauService khi
 * đặt lại mật khẩu, và chính Spring Security khi kiểm tra đăng nhập.
 * Tách ra thì mỗi lần sửa SecurityConfig không làm mất bean này nữa.
 *
 * BCrypt tự sinh salt ngẫu nhiên cho từng mật khẩu, nên hai người đặt
 * cùng mật khẩu "123456" vẫn cho ra hai chuỗi băm khác nhau. Chuỗi kết quả
 * dài đúng 60 ký tự, bắt đầu bằng $2a$10$ — đúng với mật khẩu mẫu đã nạp
 * vào bảng TaiKhoan bằng script SQL.
 */
@Configuration
public class MatKhauConfig {

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}