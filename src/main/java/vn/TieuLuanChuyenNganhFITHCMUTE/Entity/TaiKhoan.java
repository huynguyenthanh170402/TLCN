package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "TaiKhoan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class TaiKhoan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaTaiKhoan")
    private Integer maTaiKhoan;

    @Column(name = "TenDangNhap", length = 100, unique = true)
    private String tenDangNhap;

    @Column(name = "MatKhauBam", length = 500)
    private String matKhauBam;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "MaQuyen")
    private Quyen quyen;

    @Column(name = "TrangThai")
    private Integer trangThai;   // 0 khóa, 1 hoạt động

    /**
     * Mật khẩu hiện tại là mật khẩu tạm do hệ thống cấp khi quên mật khẩu.
     * Đăng nhập bằng mật khẩu này thì bị buộc đổi ngay, không vào được
     * trang nào khác cho tới khi đổi xong.
     */
    @Column(name = "MatKhauTamThoi")
    private Boolean matKhauTamThoi = Boolean.FALSE;

    /** Thời điểm mật khẩu tạm hết hiệu lực. Quá hạn thì không đăng nhập được nữa. */
    @Column(name = "HanMatKhauTam")
    private LocalDateTime hanMatKhauTam;

    /** Null coi như false, vì các dòng cũ trong cơ sở dữ liệu chưa có cột này */
    @Transient
    public boolean dangDungMatKhauTam() {
        return Boolean.TRUE.equals(matKhauTamThoi);
    }

    @Transient
    public boolean matKhauTamConHieuLuc() {
        return dangDungMatKhauTam()
                && hanMatKhauTam != null
                && LocalDateTime.now().isBefore(hanMatKhauTam);
    }
}
