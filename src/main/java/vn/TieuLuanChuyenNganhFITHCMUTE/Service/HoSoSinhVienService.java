package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.regex.Pattern;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/**
 * Sinh viên tự cập nhật thông tin liên hệ và đổi mật khẩu.
 * Các thông tin học vụ (mã số, họ tên, ngành, ngày nhập học...) do Phòng
 * Công tác Sinh viên quản lý, sinh viên không sửa được.
 */
@Service
@RequiredArgsConstructor
public class HoSoSinhVienService {

	private static final Pattern MAU_EMAIL =
			Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[\\w.-]+$");
	private static final Pattern MAU_DIEN_THOAI =
			Pattern.compile("^0\\d{8,10}$");

	private static final int DAI_TOI_THIEU_MAT_KHAU = 6;

	private final SinhVienRepository sinhVienRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final PasswordEncoder passwordEncoder;

	/** Dữ liệu nhận từ form, gom lại cho gọn tham số */
	public record ThongTinLienHe(
			String emailCaNhan,
			String soDienThoai,
			String diaChiLienHe,
			String hoTenNguoiLienHe,
			String quanHeVoiSinhVien,
			String soDienThoaiNguoiLienHe,
			String diaChiNguoiLienHe) {}

	// ===================== CẬP NHẬT LIÊN HỆ =====================

	@Transactional
	public void capNhatLienHe(SinhVien sv, ThongTinLienHe tt) {
		if (sv == null)
			throw new IllegalStateException("Không tìm thấy hồ sơ sinh viên.");

		String email = rong(tt.emailCaNhan());
		String dienThoai = rong(tt.soDienThoai());
		String dienThoaiNguoiThan = rong(tt.soDienThoaiNguoiLienHe());

		if (email != null && !MAU_EMAIL.matcher(email).matches())
			throw new IllegalStateException("Email cá nhân không đúng định dạng.");
		if (dienThoai != null && !MAU_DIEN_THOAI.matcher(dienThoai).matches())
			throw new IllegalStateException("Số điện thoại phải bắt đầu bằng 0 và có 9 đến 11 chữ số.");
		if (dienThoaiNguoiThan != null && !MAU_DIEN_THOAI.matcher(dienThoaiNguoiThan).matches())
			throw new IllegalStateException("Số điện thoại người liên hệ không hợp lệ.");

		sv.setEmailCaNhan(email);
		sv.setSoDienThoai(dienThoai);
		sv.setDiaChiLienHe(rong(tt.diaChiLienHe()));
		sv.setHoTenNguoiLienHe(rong(tt.hoTenNguoiLienHe()));
		sv.setQuanHeVoiSinhVien(rong(tt.quanHeVoiSinhVien()));
		sv.setSoDienThoaiNguoiLienHe(dienThoaiNguoiThan);
		sv.setDiaChiNguoiLienHe(rong(tt.diaChiNguoiLienHe()));
		sinhVienRepository.save(sv);
	}

	// ===================== ĐỔI MẬT KHẨU =====================

	@Transactional
	public void doiMatKhau(TaiKhoan tk, String matKhauCu, String matKhauMoi, String xacNhan) {
		if (tk == null)
			throw new IllegalStateException("Không tìm thấy tài khoản.");
		if (matKhauCu == null || !passwordEncoder.matches(matKhauCu, tk.getMatKhauBam()))
			throw new IllegalStateException("Mật khẩu hiện tại không đúng.");
		if (matKhauMoi == null || matKhauMoi.length() < DAI_TOI_THIEU_MAT_KHAU)
			throw new IllegalStateException("Mật khẩu mới phải có ít nhất "
					+ DAI_TOI_THIEU_MAT_KHAU + " ký tự.");
		if (!matKhauMoi.equals(xacNhan))
			throw new IllegalStateException("Xác nhận mật khẩu không khớp.");
		if (matKhauMoi.equals(matKhauCu))
			throw new IllegalStateException("Mật khẩu mới phải khác mật khẩu hiện tại.");

		tk.setMatKhauBam(passwordEncoder.encode(matKhauMoi));
		taiKhoanRepository.save(tk);
	}

	/** Chuỗi trống coi như không nhập */
	private String rong(String s) {
		return (s == null || s.isBlank()) ? null : s.trim();
	}
}