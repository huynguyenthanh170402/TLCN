package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Một sinh viên đăng ký tham gia một hoạt động công tác xã hội.
 *
 * Điểm rèn luyện chỉ cộng khi trạng thái là "đã tham gia". Đăng ký mà vắng
 * mặt thì không được cộng, nhưng dòng đăng ký vẫn giữ lại: đó là căn cứ khi
 * sinh viên thắc mắc vì sao mình không có điểm.
 */
@Entity
@Table(name = "DangKyHoatDong",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaHoatDong", "MaSinhVien"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DangKyHoatDong {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDangKyHoatDong")
	private Integer maDangKyHoatDong;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHoatDong", nullable = false)
	private HoatDongCTXH hoatDong;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@Column(name = "NgayDangKy", nullable = false)
	private LocalDateTime ngayDangKy = LocalDateTime.now();

	/** 1 đã đăng ký, 2 đã tham gia, 3 vắng mặt, 4 đã hủy đăng ký */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	/** Ghi lại lúc điểm danh, để biết ai chấm và chấm khi nào */
	@Column(name = "NguoiDiemDanh", length = 100)
	private String nguoiDiemDanh;

	@Column(name = "NgayDiemDanh")
	private LocalDateTime ngayDiemDanh;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	// ===================== HIỂN THỊ =====================

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 1 -> "Đã đăng ký";
			case 2 -> "Đã tham gia";
			case 3 -> "Vắng mặt";
			case 4 -> "Đã hủy đăng ký";
			default -> "";
		};
	}

	@Transient
	public String getMauTrangThai() {
		if (trangThai == null) return "bg-secondary";
		return switch (trangThai) {
			case 1 -> "bg-info text-dark";
			case 2 -> "bg-success";
			case 3 -> "bg-danger";
			case 4 -> "bg-secondary";
			default -> "bg-secondary";
		};
	}

	/** Có được cộng điểm rèn luyện cho lượt đăng ký này không */
	@Transient
	public boolean isDuocCongDiem() {
		return Integer.valueOf(2).equals(trangThai);
	}
}
