package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "Nganh")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Nganh {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaNganh")
	private Integer maNganh;

	@Column(name = "TenNganh", length = 200)
	private String tenNganh;

	/** Khoa hoặc viện quản lý ngành */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDonVi")
	private DonVi donVi;

	@Column(name = "MoTa", length = 1000)
	private String moTa;

	@Column(name = "NgayMoNganh")
	private LocalDate ngayMoNganh;

	/** 0 ngừng tuyển sinh, 1 đang tuyển sinh */
	@Column(name = "TrangThai")
	private Integer trangThai;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}