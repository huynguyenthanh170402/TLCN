package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "KetQuaHocPhan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class KetQuaHocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaKetQuaHocPhan")
	private Integer maKetQuaHocPhan;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDangKyHocPhan", nullable = false, unique = true)
	private DangKyHocPhan dangKyHocPhan;

	@Column(name = "DiemQuaTrinh", precision = 4, scale = 2)
	private BigDecimal diemQuaTrinh;

	@Column(name = "DiemGiuaKy", precision = 4, scale = 2)
	private BigDecimal diemGiuaKy;

	@Column(name = "DiemCuoiKy", precision = 4, scale = 2)
	private BigDecimal diemCuoiKy;

	/** Thang 10 */
	@Column(name = "DiemTongKet", precision = 4, scale = 2)
	private BigDecimal diemTongKet;

	/** Thang 4, dùng để tính GPA */
	@Column(name = "DiemHe4", precision = 4, scale = 2)
	private BigDecimal diemHe4;

	/** A, B+, B, C+, C, D+, D, F */
	@Column(name = "DiemChu", length = 5)
	private String diemChu;

	/** 1 đạt, 0 không đạt, null chưa có */
	@Column(name = "KetQua")
	private Integer ketQua;

	/** 0 chưa nhập, 1 đã nhập, 2 đã duyệt, 3 đã công bố */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	/** Tài khoản giảng viên/chuyên viên nhập điểm */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiNhap")
	private TaiKhoan nguoiNhap;

	@Column(name = "NgayNhap")
	private LocalDateTime ngayNhap;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;
}