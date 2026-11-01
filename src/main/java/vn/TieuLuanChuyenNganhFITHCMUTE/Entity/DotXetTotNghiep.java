package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Một đợt xét tốt nghiệp do Phòng Đào tạo mở.
 *
 * Trường không xét tốt nghiệp quanh năm mà gom thành đợt, thường mỗi học kỳ
 * một lần. Có đợt thì mới biết danh sách nào đi kèm quyết định tốt nghiệp
 * nào, và sinh viên biết khi nào phải nộp đơn.
 */
@Entity
@Table(name = "DotXetTotNghiep")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DotXetTotNghiep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDot")
	private Integer maDot;

	@Column(name = "TenDot", length = 300, nullable = false)
	private String tenDot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	@Column(name = "NgayMoDangKy")
	private LocalDateTime ngayMoDangKy;

	@Column(name = "NgayDongDangKy")
	private LocalDateTime ngayDongDangKy;

	/** Ngày hội đồng họp xét, chỉ để hiển thị */
	@Column(name = "NgayXet")
	private LocalDate ngayXet;

	/** 0 nháp, 1 đang nhận đơn, 2 đã chốt danh sách, 3 đã công bố kết quả */
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
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 0 -> "Nháp";
			case 1 -> "Đang nhận đơn";
			case 2 -> "Đã chốt danh sách";
			case 3 -> "Đã công bố kết quả";
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
			default -> "bg-secondary";
		};
	}

	/**
	 * Sinh viên còn nộp đơn được không.
	 *
	 * Xét cả trạng thái lẫn mốc thời gian. Chỉ dựa vào trạng thái là chưa đủ
	 * vì người quản lý hay quên bấm chốt, lúc đó đơn vẫn chui vào sau khi
	 * hội đồng đã họp.
	 */
	@Transient
	public boolean isDangNhanDon() {
		if (trangThai == null || trangThai != 1) return false;
		LocalDateTime now = LocalDateTime.now();
		if (ngayMoDangKy != null && now.isBefore(ngayMoDangKy)) return false;
		return ngayDongDangKy == null || !now.isAfter(ngayDongDangKy);
	}

	@Transient
	public boolean isHetHanNopDon() {
		return Integer.valueOf(1).equals(trangThai)
				&& ngayDongDangKy != null && LocalDateTime.now().isAfter(ngayDongDangKy);
	}
}
