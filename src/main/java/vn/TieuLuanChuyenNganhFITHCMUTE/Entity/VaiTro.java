package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.util.Arrays;

/**
 * Bảng tra giữa mã quyền trong cơ sở dữ liệu và ý nghĩa của nó.
 *
 * Cột Quyen.MaQuyen chỉ là con số. Lớp này là nơi duy nhất trong toàn
 * bộ mã nguồn biết số đó nghĩa là gì. Mọi chỗ khác dùng hằng số ở đây
 * chứ không viết số trần.
 *
 * Thêm quyền mới: thêm một dòng vào enum, thêm một dòng vào bảng Quyen
 * với đúng mã đó, rồi thêm requestMatchers tương ứng trong SecurityConfig.
 */
public enum VaiTro {

    SINH_VIEN               (1, "Sinh viên"),
    GIANG_VIEN              (2, "Giảng viên"),
    PHONG_DAO_TAO           (3, "Phòng Đào tạo"),
    PHONG_KE_HOACH_TAI_CHINH(4, "Phòng Kế hoạch Tài chính"),
    PHONG_CONG_TAC_SINH_VIEN(5, "Phòng Công tác Sinh viên"),
    BAN_GIAM_HIEU           (6, "Ban Giám hiệu");

    private final int ma;
    private final String tenHienThi;

    VaiTro(int ma, String tenHienThi) {
        this.ma = ma;
        this.tenHienThi = tenHienThi;
    }

    public int getMa() {
        return ma;
    }

    public String getTenHienThi() {
        return tenHienThi;
    }

    /**
     * Chuỗi quyền mà Spring Security cần, ví dụ ROLE_PHONG_DAO_TAO.
     * Trong SecurityConfig viết hasRole("PHONG_DAO_TAO") không kèm tiền tố,
     * Spring tự thêm ROLE_ vào khi so sánh.
     */
    public String authority() {
        return "ROLE_" + name();
    }

    /**
     * Dịch mã trong cơ sở dữ liệu ra vai trò.
     * Trả về null nếu gặp mã lạ, chỗ gọi tự quyết định xử lý thế nào.
     */
    public static VaiTro tuMa(Integer ma) {
        if (ma == null) return null;
        return Arrays.stream(values())
                .filter(v -> v.ma == ma)
                .findFirst()
                .orElse(null);
    }

    /** Dùng khi cần kiểm tra nhanh một mã có hợp lệ không */
    public static boolean hopLe(Integer ma) {
        return tuMa(ma) != null;
    }
}