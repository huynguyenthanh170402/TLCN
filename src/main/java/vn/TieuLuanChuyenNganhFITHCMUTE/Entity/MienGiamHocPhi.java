package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Quyết định miễn giảm học phí hoặc học bổng cho một sinh viên trong một học kỳ.
 *
 * Phòng Công tác Sinh viên lập và duyệt. Phòng Kế hoạch Tài chính chỉ đọc,
 * rồi quy tỉ lệ ra số tiền khi tính lại công nợ. Nhờ tách như vậy, số tiền
 * miễn giảm trong bảng CongNoHocPhi luôn có một quyết định đứng sau, và
 * hai phòng không cùng sửa một con số.
 *
 * Lưu tỉ lệ phần trăm chứ không lưu số tiền: sinh viên đăng ký thêm hay bớt
 * học phần thì số tiền được giảm tự thay đổi theo, không phải sửa quyết định.
 */
@Entity
@Table(name = "MienGiamHocPhi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MienGiamHocPhi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaMienGiam")
	private Integer maMienGiam;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	/** 1 miễn giảm theo chính sách, 2 học bổng khuyến khích học tập */
	@Column(name = "Loai", nullable = false)
	private Integer loai = 1;

	/** Diện được hưởng, ví dụ "Con thương binh", "Học bổng loại Giỏi" */
	@Column(name = "DienHuong", length = 300)
	private String dienHuong;

	/** Phần trăm học phí được giảm, từ 0 đến 100 */
	@Column(name = "TyLePhanTram", precision = 5, scale = 2, nullable = false)
	private BigDecimal tyLePhanTram = BigDecimal.ZERO;

	/** 0 chờ duyệt, 1 đã duyệt, 2 từ chối */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "NguoiDeNghi", length = 100)
	private String nguoiDeNghi;

	@Column(name = "NgayDeNghi", nullable = false)
	private LocalDateTime ngayDeNghi = LocalDateTime.now();

	@Column(name = "NguoiDuyet", length = 100)
	private String nguoiDuyet;

	@Column(name = "NgayDuyet")
	private LocalDateTime ngayDuyet;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public String getTenLoai() {
		if (loai == null) return "";
		return loai == 2 ? "Học bổng" : "Miễn giảm chính sách";
	}

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 0 -> "Chờ duyệt";
			case 1 -> "Đã duyệt";
			case 2 -> "Từ chối";
			default -> "";
		};
	}

	@Transient
	public boolean isDaDuyet() {
		return trangThai != null && trangThai == 1;
	}

	@Transient
	public boolean isChoDuyet() {
		return trangThai != null && trangThai == 0;
	}
}
