package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Quyen")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Quyen {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaQuyen")
    private Integer maQuyen;

    @Column(name = "TenQuyen", length = 200)
    private String tenQuyen;

    @Column(name = "MoTa", length = 1000)
    private String moTa;

    @Column(name = "TrangThai")
    private Integer trangThai;   // 0 ngừng, 1 đang dùng
}