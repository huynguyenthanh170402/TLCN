package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DonVi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DonVi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDonVi")
	private Integer maDonVi;

	@Column(name = "TenDonVi", length = 200)
	private String tenDonVi;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaLoaiDonVi")
	private LoaiDonVi loaiDonVi;

	@Column(name = "MoTa", length = 1000)
	private String moTa;

	@Column(name = "Email", length = 200)
	private String email;

	@Column(name = "SoDienThoai", length = 20)
	private String soDienThoai;

	@Column(name = "DiaChiVanPhong", length = 300)
	private String diaChiVanPhong;

	@Column(name = "NgayThanhLap")
	private LocalDate ngayThanhLap;

	/** 0 ngừng hoạt động, 1 đang hoạt động */
	@Column(name = "TrangThai")
	private Integer trangThai;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}