package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "CongNoHocPhi",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaSinhVien", "MaHocKy"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CongNoHocPhi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaCongNoHocPhi")
	private Integer maCongNoHocPhi;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	@Column(name = "NoiDung", length = 500)
	private String noiDung;

	@Column(name = "SoTienPhaiDong", precision = 18, scale = 2, nullable = false)
	private BigDecimal soTienPhaiDong = BigDecimal.ZERO;

	@Column(name = "SoTienMienGiam", precision = 18, scale = 2, nullable = false)
	private BigDecimal soTienMienGiam = BigDecimal.ZERO;

	@Column(name = "SoTienDaDong", precision = 18, scale = 2, nullable = false)
	private BigDecimal soTienDaDong = BigDecimal.ZERO;

	@Column(name = "HanThanhToan")
	private LocalDate hanThanhToan;

	@Column(name = "NgayTao", nullable = false)
	private LocalDateTime ngayTao = LocalDateTime.now();

	/** 0 chưa đóng, 1 đóng một phần, 2 đã đóng đủ, 3 không phát sinh */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** Còn phải đóng = phải đóng − miễn giảm − đã đóng (không âm) */
	@Transient
	public BigDecimal getSoTienConLai() {
		BigDecimal con = soTienPhaiDong.subtract(soTienMienGiam).subtract(soTienDaDong);
		return con.signum() < 0 ? BigDecimal.ZERO : con;
	}

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 0 -> "Chưa đóng";
			case 1 -> "Đóng một phần";
			case 2 -> "Đã đóng đủ";
			case 3 -> "Không phát sinh";
			default -> "";
		};
	}
}