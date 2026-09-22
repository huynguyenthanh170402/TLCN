package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

/** Thông báo gửi tới nhóm quyền nào (dùng khi CheDoNguoiNhan = 2) */
@Entity
@Table(name = "ThongBaoQuyen",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaThongBao", "MaQuyen"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ThongBaoQuyen {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaThongBaoQuyen")
	private Integer maThongBaoQuyen;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaThongBao", nullable = false)
	private ThongBao thongBao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaQuyen", nullable = false)
	private Quyen quyen;
}
