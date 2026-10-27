package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Nhóm quyền của tài khoản.
 *
 * Tách làm hai cột vì chúng phục vụ hai việc khác nhau:
 *
 *   MaCode    mã kỹ thuật, chữ HOA không dấu, Spring Security dùng để
 *             dựng authority "ROLE_" + MaCode. Không bao giờ đổi.
 *   TenQuyen  tên tiếng Việt hiển thị cho người dùng đọc. Đổi lúc nào
 *             cũng được mà không ảnh hưởng tới phân quyền.
 *
 * Gộp chung một cột như trước thì hoặc màn hình hiện ra chữ
 * "PHONG_DAO_TAO", hoặc đổi tên hiển thị một cái là sập phân quyền.
 */
@Entity
@Table(name = "Quyen")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Quyen {

    /** Sáu mã quyền của hệ thống. Thêm quyền mới thì thêm hằng số ở đây. */
    public static final String SINH_VIEN     = "SINH_VIEN";
    public static final String GIANG_VIEN    = "GIANG_VIEN";
    public static final String DAO_TAO       = "PHONG_DAO_TAO";
    public static final String TAI_CHINH     = "PHONG_KE_HOACH_TAI_CHINH";
    public static final String CTSV          = "PHONG_CONG_TAC_SINH_VIEN";
    public static final String BAN_GIAM_HIEU = "BAN_GIAM_HIEU";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaQuyen")
    private Integer maQuyen;

    /** Mã kỹ thuật, ví dụ PHONG_DAO_TAO. Dùng cho hasRole trong SecurityConfig. */
    @Column(name = "MaCode", length = 50, unique = true)
    private String maCode;

    /** Tên hiển thị, ví dụ "Phòng Đào tạo" */
    @Column(name = "TenQuyen", length = 200)
    private String tenQuyen;

    @Column(name = "MoTa", length = 1000)
    private String moTa;

    @Column(name = "TrangThai")
    private Integer trangThai;   // 0 ngừng, 1 đang dùng

    /** Chuỗi authority mà Spring Security cần: ROLE_PHONG_DAO_TAO */
    @Transient
    public String getAuthority() {
        return maCode == null ? null : "ROLE_" + maCode;
    }
}