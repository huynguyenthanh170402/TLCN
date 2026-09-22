package vn.TieuLuanChuyenNganhFITHCMUTE.Security;

/**
 * Đổi MaQuyen (số) sang tên role của Spring Security.
 * Thứ tự 1-5 khớp với thứ tự DataSeeder chèn vào bảng Quyen.
 */
public final class QuyenCode {

	private QuyenCode() { }

	public static String toRole(Integer maQuyen) {
		if (maQuyen == null) return "UNKNOWN";
		return switch (maQuyen) {
			case 1 -> "SINH_VIEN";
			case 2 -> "GIANG_VIEN";
			case 3 -> "CHUYEN_VIEN";
			case 4 -> "BAN_GIAM_HIEU";
			case 5 -> "PHONG_BAN";
			default -> "UNKNOWN";
		};
	}
}