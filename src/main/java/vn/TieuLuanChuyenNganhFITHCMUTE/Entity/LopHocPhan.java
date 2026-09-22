package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LopHocPhan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LopHocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLopHocPhan")
	private Integer maLopHocPhan;

	/** Ví dụ: WEPR330479_01 */
	@Column(name = "MaLop", length = 50, nullable = false, unique = true)
	private String maLop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocPhanId", nullable = false)
	private HocPhan hocPhan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	@Column(name = "SiSoToiDa")
	private Integer siSoToiDa;

	@Column(name = "SiSoToiThieu")
	private Integer siSoToiThieu;

	/** 0 chưa mở, 1 đang mở đăng ký, 2 đang học, 3 đã kết thúc, 4 hủy lớp */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}