package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DangKyHocPhan",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaSinhVien", "MaLopHocPhan"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DangKyHocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDangKyHocPhan")
	private Integer maDangKyHocPhan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaLopHocPhan", nullable = false)
	private LopHocPhan lopHocPhan;

	/** 1 học lần đầu, 2 học lại, 3 học cải thiện, 4 học vượt */
	@Column(name = "LoaiDangKy", nullable = false)
	private Integer loaiDangKy = 1;

	@Column(name = "NgayDangKy", nullable = false)
	private LocalDateTime ngayDangKy = LocalDateTime.now();

	/** 0 chờ duyệt, 1 đã đăng ký, 2 đã hủy, 3 bị từ chối */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "NgayHuy")
	private LocalDateTime ngayHuy;

	@Column(name = "LyDoHuy", length = 1000)
	private String lyDoHuy;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** Kết quả gắn với lượt đăng ký này (có thể chưa có) */
	@OneToOne(mappedBy = "dangKyHocPhan", fetch = FetchType.LAZY)
	private KetQuaHocPhan ketQua;

	@Transient
	public String getTenLoaiDangKy() {
		if (loaiDangKy == null) return "";
		return switch (loaiDangKy) {
			case 1 -> "Học lần đầu";
			case 2 -> "Học lại";
			case 3 -> "Cải thiện";
			case 4 -> "Học vượt";
			default -> "";
		};
	}
}