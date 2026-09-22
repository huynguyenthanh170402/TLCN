package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HocPhan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaHocPhanId")
	private Integer maHocPhanId;

	/** Mã môn, ví dụ: WEPR330479 */
	@Column(name = "MaHocPhan", length = 50, nullable = false, unique = true)
	private String maHocPhan;

	@Column(name = "TenHocPhan", length = 300, nullable = false)
	private String tenHocPhan;

	@Column(name = "SoTinChi", nullable = false)
	private Integer soTinChi;

	@Column(name = "SoTietLyThuyet")
	private Integer soTietLyThuyet;

	@Column(name = "SoTietThucHanh")
	private Integer soTietThucHanh;

	/** Khoa phụ trách giảng dạy */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDonVi", nullable = false)
	private DonVi donVi;

	/** 0 ngừng giảng dạy, 1 đang giảng dạy */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}