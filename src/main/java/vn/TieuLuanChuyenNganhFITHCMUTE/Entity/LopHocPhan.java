package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LopHocPhan")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LopHocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLopHocPhan")
	private Integer maLopHocPhan;

	@Column(name = "MaLop", length = 100, nullable = false, unique = true)
	private String maLop;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocPhanId", nullable = false)
	private HocPhan hocPhan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaHocKy", nullable = false)
	private HocKy hocKy;

	@Column(name = "SiSoToiDa")
	private Integer siSoToiDa;

	@Column(name = "SiSoToiThieu")
	private Integer siSoToiThieu;

	/** 1 chính khóa, 2 học lại và cải thiện, 3 học hè, 4 lớp tối */
	@Column(name = "LoaiLop", nullable = false)
	private Integer loaiLop = 1;

	/**
	 * Khóa tuyển sinh được ưu tiên đăng ký, ví dụ "2024".
	 * Null nghĩa là mọi khóa đều đăng ký được (lớp học lại, học hè, lớp tối).
	 */
	@Column(name = "KhoaApDung", length = 20)
	private String khoaApDung;

	/** Ngành mà lớp chính khóa này phục vụ, null nếu lớp mở chung */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNganh")
	private Nganh nganh;

	/** 0 chưa mở, 1 mở đăng ký, 2 đang học, 3 đã kết thúc, 4 đã hủy */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public String getTenLoaiLop() {
		if (loaiLop == null) return "";
		return switch (loaiLop) {
			case 1 -> "Chính khóa";
			case 2 -> "Học lại / cải thiện";
			case 3 -> "Học hè";
			case 4 -> "Lớp tối";
			default -> "";
		};
	}

	@Transient
	public String getMauLoaiLop() {
		if (loaiLop == null) return "bg-secondary";
		return switch (loaiLop) {
			case 1 -> "bg-primary";
			case 2 -> "bg-warning text-dark";
			case 3 -> "bg-success";
			case 4 -> "bg-dark";
			default -> "bg-secondary";
		};
	}
}