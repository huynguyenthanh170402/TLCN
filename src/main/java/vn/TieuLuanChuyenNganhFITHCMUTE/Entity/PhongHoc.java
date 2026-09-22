package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PhongHoc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PhongHoc {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaPhongHoc")
	private Integer maPhongHoc;

	/** Ví dụ: A1-101 */
	@Column(name = "MaPhong", length = 50, nullable = false, unique = true)
	private String maPhong;

	@Column(name = "TenPhong", length = 200)
	private String tenPhong;

	@Column(name = "ToaNha", length = 100)
	private String toaNha;

	@Column(name = "Tang", length = 50)
	private String tang;

	@Column(name = "SucChua", nullable = false)
	private Integer sucChua;

	/** 1 lý thuyết, 2 thực hành máy tính, 3 phòng thí nghiệm, 4 hội trường, 5 khác */
	@Column(name = "LoaiPhong", nullable = false)
	private Integer loaiPhong;

	/** 0 ngừng sử dụng, 1 đang sử dụng, 2 bảo trì */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}