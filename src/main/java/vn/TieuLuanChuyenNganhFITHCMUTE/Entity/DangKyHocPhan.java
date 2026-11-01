package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "DangKyHocPhan",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaSinhVien", "MaLopHocPhan"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class DangKyHocPhan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaDangKyHocPhan")
	private Integer maDangKyHocPhan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaLopHocPhan", nullable = false)
	private LopHocPhan lopHocPhan;

	/** 1 học lần đầu, 2 học lại, 3 học cải thiện, 4 học vượt */
	@Column(name = "LoaiDangKy", nullable = false)
	private Integer loaiDangKy = 1;

	@Column(name = "NgayDangKy", nullable = false)
	private LocalDateTime ngayDangKy = LocalDateTime.now();

	/**
	 * 1 lượt đăng ký còn hiệu lực, 2 đã hủy.
	 *
	 * Cột này trả lời câu "chỗ trong lớp còn giữ hay đã nhả ra", nên nó chi
	 * phối sĩ số, học phí, lịch học và bảng điểm. Việc Phòng Đào tạo đã xác
	 * nhận hay chưa là câu hỏi khác, nằm ở trangThaiDuyet bên dưới: lượt đăng
	 * ký đang chờ xác nhận vẫn giữ chỗ và vẫn tính tín chỉ, nếu không sinh
	 * viên có thể ôm hàng chục lớp trùng giờ trong lúc chờ duyệt.
	 */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	/* ---------- Xác nhận của Phòng Đào tạo ---------- */

	/**
	 * 0 chờ xác nhận, 1 đã xác nhận, 2 bị từ chối.
	 *
	 * null là dữ liệu có từ trước khi thêm bước xác nhận. Coi như đã xác nhận,
	 * chứ không phải đùn cả nghìn lượt đăng ký cũ vào hàng chờ duyệt.
	 */
	@Column(name = "TrangThaiDuyet")
	private Integer trangThaiDuyet;

	@Column(name = "NgayDuyet")
	private LocalDateTime ngayDuyet;

	/** Tên đăng nhập của chuyên viên đã xác nhận hoặc từ chối */
	@Column(name = "NguoiDuyet", length = 100)
	private String nguoiDuyet;

	@Column(name = "LyDoTuChoi", length = 1000)
	private String lyDoTuChoi;

	@Column(name = "NgayHuy")
	private LocalDateTime ngayHuy;

	@Column(name = "LyDoHuy", length = 1000)
	private String lyDoHuy;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** Kết quả gắn với lượt đăng ký này (có thể chưa có) */
	@OneToOne(mappedBy = "dangKyHocPhan", fetch = FetchType.LAZY)
	private KetQuaHocPhan ketQua;

	@Transient
	public boolean isChoDuyet() { return Integer.valueOf(0).equals(trangThaiDuyet); }

	/** null tính là đã xác nhận, xem ghi chú ở trangThaiDuyet */
	@Transient
	public boolean isDaDuyet() {
		return trangThaiDuyet == null || Integer.valueOf(1).equals(trangThaiDuyet);
	}

	@Transient
	public boolean isTuChoi() { return Integer.valueOf(2).equals(trangThaiDuyet); }

	@Transient
	public String getTenTrangThaiDuyet() {
		if (isChoDuyet()) return "Chờ Phòng Đào tạo xác nhận";
		if (isTuChoi())   return "Phòng Đào tạo không duyệt";
		return "Đã xác nhận";
	}

	@Transient
	public String getTenLoaiDangKy() {
		if (loaiDangKy == null) return "";
		return switch (loaiDangKy) {
			case 1 -> "Học lần đầu";
			case 2 -> "Học lại";
			case 3 -> "Cải thiện";
			case 4 -> "Học vượt";
			default -> "";
		};
	}
}