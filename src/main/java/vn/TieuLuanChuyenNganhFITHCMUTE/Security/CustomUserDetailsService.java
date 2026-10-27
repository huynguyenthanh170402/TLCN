package vn.TieuLuanChuyenNganhFITHCMUTE.Security;

import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

	private final TaiKhoanRepository taiKhoanRepository;

	@Override
	public UserDetails loadUserByUsername(String tenDangNhap)
			throws UsernameNotFoundException {

		TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(tenDangNhap)
				.orElseThrow(() -> new UsernameNotFoundException(
						"Không tìm thấy tài khoản: " + tenDangNhap));

		Integer maQuyen = (tk.getQuyen() != null) ? tk.getQuyen().getMaQuyen() : null;

		// Phải tự ghi ROLE_ vì hasRole("X") so sánh với "ROLE_X"
		List<GrantedAuthority> authorities =
				List.of(new SimpleGrantedAuthority("ROLE_" + QuyenCode.toRole(maQuyen)));

		boolean hoatDong = tk.getTrangThai() != null && tk.getTrangThai() == 1;

		/*
		 * Mật khẩu tạm quá 5 phút thì coi như hết hiệu lực.
		 *
		 * Đánh dấu ở đây chứ không kiểm tra trong controller: Spring Security
		 * sẽ ném CredentialsExpiredException ngay trong lúc xác thực, nên
		 * không có đường nào lọt qua được. SecurityConfig bắt ngoại lệ đó rồi
		 * đưa về trang đăng nhập kèm lời nhắn riêng.
		 */
		boolean matKhauConHan = !tk.dangDungMatKhauTam() || tk.matKhauTamConHieuLuc();

		return User.builder()
				.username(tk.getTenDangNhap())
				.password(tk.getMatKhauBam())
				.authorities(authorities)
				.disabled(!hoatDong)
				.credentialsExpired(!matKhauConHan)
				.build();
	}
}
