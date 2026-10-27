package vn.TieuLuanChuyenNganhFITHCMUTE.Security;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.VaiTro;

/**
 * Đổi MaQuyen (số) sang tên role của Spring Security.
 *
 * Lớp này từng tự giữ một bảng tra riêng, lệch với enum VaiTro sau khi
 * hệ thống rút xuống còn 6 vai trò: mã 4 bị dịch thành BAN_GIAM_HIEU
 * thay vì PHONG_KE_HOACH_TAI_CHINH, còn mã 6 rơi vào UNKNOWN. Hậu quả là
 * bốn tài khoản phòng ban đăng nhập xong nhận sai quyền.
 *
 * Nay chỉ còn một nguồn sự thật duy nhất là enum VaiTro. Thêm vai trò mới
 * thì sửa ở đó, chỗ này tự theo.
 */
public final class QuyenCode {

	private QuyenCode() { }

	public static String toRole(Integer maQuyen) {
		VaiTro vaiTro = VaiTro.tuMa(maQuyen);
		return (vaiTro == null) ? "UNKNOWN" : vaiTro.name();
	}
}
