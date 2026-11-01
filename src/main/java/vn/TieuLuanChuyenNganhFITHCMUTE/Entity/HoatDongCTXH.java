package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Một hoạt động công tác xã hội do Phòng Công tác Sinh viên mở.
 *
 * Sinh viên tham gia thì được cộng điểm rèn luyện của học kỳ đó. Gắn hoạt
 * động vào một học kỳ cụ thể vì điểm rèn luyện chấm theo từng học kỳ, tham
 * gia ở kỳ nào thì cộng vào kỳ ấy.
 */
@Entity
@Table(name = "HoatDongCTXH")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HoatDongCTXH {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaHoatDong")
	private Integer maHoatDong;

	@Column(name = "TenHoatDong", length = 300, nullable = false)
	private String tenHoatDong;

	@Column(name = "MoTa", length = 2000)
	private String moTa;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	/** 1 tình nguyện, 2 văn hóa văn nghệ, 3 thể thao, 4 hội thảo và tập huấn, 5 khác */
	@Column(name = "LoaiHoatDong", nullable = false)
	private Integer loaiHoatDong = 1;

	/** Số điểm rèn luyện được cộng khi tham gia đầy đủ */
	@Column(name = "DiemCong", nullable = false)
	private Integer diemCong = 5;

	@Column(name = "DonViToChuc", length = 200)
	private String donViToChuc;

	@Column(name = "DiaDiem", length = 300)
	private String diaDiem;

	@Column(name = "ThoiGianBatDau")
	private LocalDateTime thoiGianBatDau;

	@Column(name = "ThoiGianKetThuc")
	private LocalDateTime thoiGianKetThuc;

	@Column(name = "HanDangKy")
	private LocalDateTime hanDangKy;

	/** Để trống nghĩa là không giới hạn số lượng */
	@Column(name = "SoLuongToiDa")
	private Integer soLuongToiDa;

	/** 0 nháp, 1 đang mở đăng ký, 2 đã chốt danh sách, 3 đã kết thúc, 4 đã hủy */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "NguoiTao", length = 100)
	private String nguoiTao;

	@Column(name = "NgayTao", nullable = false)
	private LocalDateTime ngayTao = LocalDateTime.now();

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	// ===================== HIỂN THỊ =====================

	@Transient
	public String getTenLoai() {
		if (loaiHoatDong == null) return "";
		return switch (loaiHoatDong) {
			case 1 -> "Tình nguyện";
			case 2 -> "Văn hóa văn nghệ";
			case 3 -> "Thể thao";
			case 4 -> "Hội thảo, tập huấn";
			default -> "Khác";
		};
	}

	@Transient
	public String getMauLoai() {
		if (loaiHoatDong == null) return "bg-secondary";
		return switch (loaiHoatDong) {
			case 1 -> "bg-success";
			case 2 -> "bg-info text-dark";
			case 3 -> "bg-warning text-dark";
			case 4 -> "bg-primary";
			default -> "bg-secondary";
		};
	}

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 0 -> "Nháp";
			case 1 -> "Đang mở đăng ký";
			case 2 -> "Đã chốt danh sách";
			case 3 -> "Đã kết thúc";
			case 4 -> "Đã hủy";
			default -> "";
		};
	}

	@Transient
	public String getMauTrangThai() {
		if (trangThai == null) return "bg-secondary";
		return switch (trangThai) {
			case 0 -> "bg-secondary";
			case 1 -> "bg-success";
			case 2 -> "bg-primary";
			case 3 -> "bg-dark";
			case 4 -> "bg-danger";
			default -> "bg-secondary";
		};
	}

	/**
	 * Sinh viên còn đăng ký được không.
	 *
	 * Phải thỏa cả hai: hoạt động đang ở trạng thái mở, và chưa quá hạn đăng
	 * ký. Chỉ nhìn trạng thái là chưa đủ — người tổ chức thường quên bấm
	 * chốt danh sách, nếu không chặn theo hạn thì sinh viên vẫn đăng ký được
	 * sau khi hoạt động đã diễn ra xong.
	 */
	@Transient
	public boolean isDangMoDangKy() {
		if (trangThai == null || trangThai != 1) return false;
		return hanDangKy == null || !LocalDateTime.now().isAfter(hanDangKy);
	}

	/** Đã mở nhưng hết hạn đăng ký, dùng để giải thích cho sinh viên */
	@Transient
	public boolean isHetHanDangKy() {
		return Integer.valueOf(1).equals(trangThai)
				&& hanDangKy != null && LocalDateTime.now().isAfter(hanDangKy);
	}

	/** Đã qua giờ kết thúc thì mới điểm danh được */
	@Transient
	public boolean isDaDienRa() {
		return thoiGianKetThuc != null && LocalDateTime.now().isAfter(thoiGianKetThuc);
	}
}
