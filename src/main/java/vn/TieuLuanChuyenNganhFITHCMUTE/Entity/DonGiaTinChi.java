package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.math.BigDecimal;

import jakarta.persistence.*;
import lombok.*;

/**
 * Đơn giá một tín chỉ, áp cho một học kỳ.
 *
 * KhoaTuyenSinh để trống nghĩa là đơn giá chung của học kỳ đó, áp cho mọi
 * khóa. Điền một khóa cụ thể (ví dụ "2024") thì dòng đó thắng dòng chung.
 * Nhờ vậy trường tăng học phí cho khóa mới mà không đụng tới khóa cũ.
 *
 * Ràng buộc duy nhất trên cặp (MaHocKy, KhoaTuyenSinh) chặn việc một học kỳ
 * có hai đơn giá cho cùng một khóa. SQL Server chỉ cho phép một dòng NULL
 * trong ràng buộc này, đúng với ý muốn: mỗi học kỳ chỉ một đơn giá chung.
 */
@Entity
@Table(name = "DonGiaTinChi",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaHocKy", "KhoaTuyenSinh"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DonGiaTinChi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDonGia")
	private Integer maDonGia;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	/** Để trống = áp cho mọi khóa của học kỳ này */
	@Column(name = "KhoaTuyenSinh", length = 20)
	private String khoaTuyenSinh;

	@Column(name = "DonGia", precision = 18, scale = 2, nullable = false)
	private BigDecimal donGia = BigDecimal.ZERO;

	@Column(name = "GhiChu", length = 500)
	private String ghiChu;

	@Transient
	public String getTenPhamVi() {
		return (khoaTuyenSinh == null || khoaTuyenSinh.isBlank())
				? "Mọi khóa"
				: "Khóa " + khoaTuyenSinh;
	}
}
