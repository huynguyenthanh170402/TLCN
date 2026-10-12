package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Chương trình đào tạo: một học phần nằm trong chương trình của một ngành,
 * dự kiến học ở học kỳ thứ mấy, bắt buộc hay tự chọn.
 * Một học phần có thể thuộc nhiều ngành (ví dụ Anh văn, Triết học).
 */
@Entity
@Table(name = "ChuongTrinhDaoTao",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaNganh", "MaHocPhanId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ChuongTrinhDaoTao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaChuongTrinh")
	private Integer maChuongTrinh;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNganh", nullable = false)
	private Nganh nganh;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocPhanId", nullable = false)
	private HocPhan hocPhan;

	/** Học kỳ dự kiến trong chương trình, 1 đến 8 */
	@Column(name = "HocKyDuKien", nullable = false)
	private Integer hocKyDuKien = 1;

	/** 1 đại cương, 2 cơ sở ngành, 3 chuyên ngành, 4 tốt nghiệp */
	@Column(name = "KhoiKienThuc", nullable = false)
	private Integer khoiKienThuc = 1;

	/** 1 bắt buộc, 2 tự chọn */
	@Column(name = "LoaiHocPhan", nullable = false)
	private Integer loaiHocPhan = 1;

	/** Khóa bắt đầu áp dụng, null là áp dụng cho mọi khóa */
	@Column(name = "KhoaApDung", length = 20)
	private String khoaApDung;

	/** 0 ngừng áp dụng, 1 đang áp dụng */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public String getTenKhoiKienThuc() {
		if (khoiKienThuc == null) return "";
		return switch (khoiKienThuc) {
			case 1 -> "Đại cương";
			case 2 -> "Cơ sở ngành";
			case 3 -> "Chuyên ngành";
			case 4 -> "Tốt nghiệp";
			default -> "";
		};
	}

	@Transient
	public String getTenLoaiHocPhan() {
		return Integer.valueOf(2).equals(loaiHocPhan) ? "Tự chọn" : "Bắt buộc";
	}
}