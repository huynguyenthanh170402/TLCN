package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Một lần sinh viên nộp tiền.
 *
 * Mỗi phiếu gắn với một dòng công nợ. Sinh viên đóng làm nhiều đợt thì có
 * nhiều phiếu cùng trỏ về một công nợ, tổng các phiếu bằng SoTienDaDong.
 *
 * Phiếu đã lập thì không sửa, chỉ hủy — kế toán không tẩy xóa chứng từ.
 * Hủy phiếu sẽ trừ lại số tiền khỏi công nợ và tính lại trạng thái.
 */
@Entity
@Table(name = "PhieuThu")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PhieuThu {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaPhieuThu")
	private Integer maPhieuThu;

	/** Số in trên biên lai, ví dụ PT20261026-0007 */
	@Column(name = "SoPhieu", length = 50, nullable = false, unique = true)
	private String soPhieu;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaCongNoHocPhi", nullable = false)
	private CongNoHocPhi congNo;

	@Column(name = "SoTien", precision = 18, scale = 2, nullable = false)
	private BigDecimal soTien = BigDecimal.ZERO;

	/** 1 tiền mặt, 2 chuyển khoản */
	@Column(name = "HinhThuc", nullable = false)
	private Integer hinhThuc = 1;

	@Column(name = "NgayThu", nullable = false)
	private LocalDateTime ngayThu = LocalDateTime.now();

	/** Tên đăng nhập của cán bộ lập phiếu */
	@Column(name = "NguoiThu", length = 100)
	private String nguoiThu;

	@Column(name = "GhiChu", length = 500)
	private String ghiChu;

	/** 1 hiệu lực, 0 đã hủy */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Transient
	public String getTenHinhThuc() {
		if (hinhThuc == null) return "";
		return hinhThuc == 2 ? "Chuyển khoản" : "Tiền mặt";
	}

	@Transient
	public boolean isDaHuy() {
		return trangThai != null && trangThai == 0;
	}
}
