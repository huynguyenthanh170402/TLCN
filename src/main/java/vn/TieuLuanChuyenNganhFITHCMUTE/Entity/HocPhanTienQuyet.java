package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Điều kiện học phần: muốn đăng ký hocPhan thì phải thỏa điều kiện với hocPhanDieuKien.
 * Ví dụ muốn học "Anh văn 2" thì phải đạt "Anh văn 1".
 */
@Entity
@Table(name = "HocPhanTienQuyet",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaHocPhanId", "MaHocPhanDieuKien"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HocPhanTienQuyet {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaTienQuyet")
	private Integer maTienQuyet;

	/** Học phần muốn đăng ký */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocPhanId", nullable = false)
	private HocPhan hocPhan;

	/** Học phần phải học trước */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocPhanDieuKien", nullable = false)
	private HocPhan hocPhanDieuKien;

	/** 1 tiên quyết (phải đạt), 2 học trước (chỉ cần đã học), 3 song hành */
	@Column(name = "LoaiDieuKien", nullable = false)
	private Integer loaiDieuKien = 1;

	/** 0 ngừng áp dụng, 1 đang áp dụng */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "GhiChu", length = 500)
	private String ghiChu;

	@Transient
	public String getTenLoaiDieuKien() {
		if (loaiDieuKien == null) return "";
		return switch (loaiDieuKien) {
			case 1 -> "Tiên quyết";
			case 2 -> "Học trước";
			case 3 -> "Song hành";
			default -> "";
		};
	}
}