package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "PhanCongGiangDay",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaLopHocPhan", "MaGiangVien", "VaiTro"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PhanCongGiangDay {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaPhanCong")
	private Integer maPhanCong;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaLopHocPhan", nullable = false)
	private LopHocPhan lopHocPhan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaGiangVien", nullable = false)
	private GiangVien giangVien;

	/** 1 giảng viên chính (người nhập điểm), 2 trợ giảng, 3 giảng viên thực hành */
	@Column(name = "VaiTro", nullable = false)
	private Integer vaiTro = 1;

	/** 0 đã hủy, 1 hiệu lực */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "NgayPhanCong", nullable = false)
	private LocalDateTime ngayPhanCong = LocalDateTime.now();

	/** Tài khoản Phòng Đào tạo thực hiện phân công */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiPhanCong")
	private TaiKhoan nguoiPhanCong;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public String getTenVaiTro() {
		if (vaiTro == null) return "";
		return switch (vaiTro) {
			case 1 -> "Giảng viên chính";
			case 2 -> "Trợ giảng";
			case 3 -> "Giảng viên thực hành";
			default -> "";
		};
	}
}