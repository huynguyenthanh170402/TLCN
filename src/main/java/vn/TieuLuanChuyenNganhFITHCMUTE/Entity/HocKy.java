package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "HocKy")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HocKy {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaHocKy")
	private Integer maHocKy;

	/** Ví dụ: HK1-2024-2025 */
	@Column(name = "MaHocKyCode", length = 50, nullable = false, unique = true)
	private String maHocKyCode;

	@Column(name = "TenHocKy", length = 200, nullable = false)
	private String tenHocKy;

	/** Ví dụ: 2024-2025 */
	@Column(name = "NamHoc", length = 20, nullable = false)
	private String namHoc;

	/** 1, 2 hoặc 3 (học kỳ hè) */
	@Column(name = "HocKySo", nullable = false)
	private Integer hocKySo;

	@Column(name = "NgayBatDau")
	private LocalDate ngayBatDau;

	@Column(name = "NgayKetThuc")
	private LocalDate ngayKetThuc;

	/** Phòng Đào tạo đặt khung giờ mở đăng ký học phần */
	@Column(name = "NgayMoDangKy")
	private LocalDateTime ngayMoDangKy;

	@Column(name = "NgayDongDangKy")
	private LocalDateTime ngayDongDangKy;

	/** 0 chưa bắt đầu, 1 đang diễn ra, 2 đã kết thúc */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** Đang trong khung giờ đăng ký học phần hay không */
	@Transient
	public boolean isDangMoDangKy() {
		if (ngayMoDangKy == null || ngayDongDangKy == null) return false;
		LocalDateTime now = LocalDateTime.now();
		return !now.isBefore(ngayMoDangKy) && !now.isAfter(ngayDongDangKy);
	}
}