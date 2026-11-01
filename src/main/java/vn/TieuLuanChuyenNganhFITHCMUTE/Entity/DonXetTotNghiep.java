package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Đơn đăng ký xét tốt nghiệp của một sinh viên trong một đợt.
 *
 * Các cột điểm và tín chỉ là bản chụp tại lúc nộp đơn, không phải số liệu
 * sống. Hội đồng xét trên số liệu của thời điểm nộp; sau đó sinh viên có
 * học thêm hay đóng thêm học phí thì hồ sơ đã nộp vẫn phải giữ nguyên để
 * đối chiếu với quyết định.
 */
@Entity
@Table(name = "DonXetTotNghiep",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaDot", "MaSinhVien"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DonXetTotNghiep {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDon")
	private Integer maDon;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDot", nullable = false)
	private DotXetTotNghiep dot;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@Column(name = "NgayNop", nullable = false)
	private LocalDateTime ngayNop = LocalDateTime.now();

	// --- Bản chụp số liệu lúc nộp ---

	@Column(name = "GpaLucNop", precision = 4, scale = 2)
	private BigDecimal gpaLucNop;

	@Column(name = "TinChiLucNop")
	private Integer tinChiLucNop;

	@Column(name = "TinChiYeuCau")
	private Integer tinChiYeuCau;

	@Column(name = "DiemRenLuyenLucNop")
	private Integer diemRenLuyenLucNop;

	@Column(name = "SoNamDaHoc")
	private Integer soNamDaHoc;

	/** 1 chờ xét, 2 đủ điều kiện tốt nghiệp, 3 không đủ điều kiện */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "LyDoTuChoi", length = 1000)
	private String lyDoTuChoi;

	@Column(name = "NguoiXet", length = 100)
	private String nguoiXet;

	@Column(name = "NgayXet")
	private LocalDateTime ngayXet;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	// ===================== HIỂN THỊ =====================

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 1 -> "Chờ xét";
			case 2 -> "Đủ điều kiện tốt nghiệp";
			case 3 -> "Không đủ điều kiện";
			default -> "";
		};
	}

	@Transient
	public String getMauTrangThai() {
		if (trangThai == null) return "bg-secondary";
		return switch (trangThai) {
			case 1 -> "bg-warning text-dark";
			case 2 -> "bg-success";
			case 3 -> "bg-danger";
			default -> "bg-secondary";
		};
	}

	@Transient
	public boolean isChoXet() { return Integer.valueOf(1).equals(trangThai); }
}
