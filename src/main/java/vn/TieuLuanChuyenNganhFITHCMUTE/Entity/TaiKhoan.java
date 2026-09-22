package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

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
}