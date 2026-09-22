package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ThongBao")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ThongBao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaThongBao")
	private Integer maThongBao;

	/** Tài khoản soạn và đăng thông báo */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNguoiDang", nullable = false)
	private TaiKhoan nguoiDang;

	/** Đơn vị phát hành (Phòng Đào tạo, ...). Null nếu Ban Giám hiệu gửi */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDonVi")
	private DonVi donVi;

	/** 1 chung, 2 học vụ, 3 học phí, 4 sự kiện, 5 khẩn */
	@Column(name = "LoaiThongBao", nullable = false)
	private Integer loaiThongBao = 1;

	/** 1 toàn trường, 2 theo nhóm quyền (ThongBaoQuyen), 3 cá nhân (NguoiNhanThongBao) */
	@Column(name = "CheDoNguoiNhan", nullable = false)
	private Integer cheDoNguoiNhan = 1;

	@Column(name = "TieuDe", length = 300, nullable = false)
	private String tieuDe;

	@Column(name = "NoiDung", nullable = false, columnDefinition = "nvarchar(max)")
	private String noiDung;

	@Column(name = "TepDinhKem", length = 1000)
	private String tepDinhKem;

	@Column(name = "NgayTao", nullable = false)
	private LocalDateTime ngayTao = LocalDateTime.now();

	@Column(name = "NgayDang")
	private LocalDateTime ngayDang;

	@Column(name = "NgayHetHan")
	private LocalDateTime ngayHetHan;

	/** 0 nháp, 1 đã đăng, 2 đã gỡ */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 0;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public String getTenLoai() {
		if (loaiThongBao == null) return "";
		return switch (loaiThongBao) {
			case 1 -> "Chung";
			case 2 -> "Học vụ";
			case 3 -> "Học phí";
			case 4 -> "Sự kiện";
			case 5 -> "Khẩn";
			default -> "";
		};
	}

	/** Màu badge Bootstrap theo loại */
	@Transient
	public String getMauLoai() {
		if (loaiThongBao == null) return "bg-secondary";
		return switch (loaiThongBao) {
			case 2 -> "bg-primary";
			case 3 -> "bg-warning text-dark";
			case 4 -> "bg-info text-dark";
			case 5 -> "bg-danger";
			default -> "bg-secondary";
		};
	}

	/** Tên nơi phát hành để hiển thị */
	@Transient
	public String getNoiPhatHanh() {
		return donVi != null ? donVi.getTenDonVi() : "Ban Giám hiệu";
	}
}
