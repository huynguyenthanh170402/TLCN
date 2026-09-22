package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "SinhVien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SinhVien {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaSinhVien")
	private Integer maSinhVien;

	@Column(name = "MaSoSinhVien", length = 50)
	private String maSoSinhVien;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaTaiKhoan", nullable = false)
	private TaiKhoan taiKhoan;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaNganh", nullable = false)
	private Nganh nganh;

	@Column(name = "KhoaTuyenSinh", length = 20)
	private String khoaTuyenSinh;

	// --- Nhân thân ---
	@Column(name = "HoTen", length = 200)
	private String hoTen;

	@Column(name = "NgaySinh")
	private LocalDate ngaySinh;

	/** 1 Nam, 2 Nữ, 3 Khác */
	@Column(name = "GioiTinh")
	private Integer gioiTinh;

	@Column(name = "AnhDaiDien", length = 500)
	private String anhDaiDien;

	@Column(name = "NoiSinh", length = 300)
	private String noiSinh;

	@Column(name = "QueQuan", length = 300)
	private String queQuan;

	@Column(name = "QuocTich", length = 100)
	private String quocTich;

	@Column(name = "DanToc", length = 100)
	private String danToc;

	@Column(name = "TonGiao", length = 100)
	private String tonGiao;

	// --- Giấy tờ ---
	/** 1 CCCD, 2 CMND, 3 Hộ chiếu */
	@Column(name = "LoaiGiayTo")
	private Integer loaiGiayTo;

	@Column(name = "SoGiayTo", length = 50)
	private String soGiayTo;

	@Column(name = "NgayCapGiayTo")
	private LocalDate ngayCapGiayTo;

	@Column(name = "NoiCapGiayTo", length = 300)
	private String noiCapGiayTo;

	@Column(name = "NgayHetHanGiayTo")
	private LocalDate ngayHetHanGiayTo;

	// --- Liên hệ ---
	@Column(name = "EmailCaNhan", length = 200)
	private String emailCaNhan;

	@Column(name = "EmailTruong", length = 200)
	private String emailTruong;

	@Column(name = "SoDienThoai", length = 20)
	private String soDienThoai;

	@Column(name = "DiaChiThuongTru", length = 500)
	private String diaChiThuongTru;

	@Column(name = "DiaChiLienHe", length = 500)
	private String diaChiLienHe;

	@Column(name = "HoTenNguoiLienHe", length = 200)
	private String hoTenNguoiLienHe;

	@Column(name = "QuanHeVoiSinhVien", length = 50)
	private String quanHeVoiSinhVien;

	@Column(name = "SoDienThoaiNguoiLienHe", length = 20)
	private String soDienThoaiNguoiLienHe;

	@Column(name = "DiaChiNguoiLienHe", length = 500)
	private String diaChiNguoiLienHe;

	// --- Học vụ ---
	@Column(name = "NgayNhapHoc")
	private LocalDate ngayNhapHoc;

	@Column(name = "NgayTotNghiepDuKien")
	private LocalDate ngayTotNghiepDuKien;

	@Column(name = "NgayTotNghiep")
	private LocalDate ngayTotNghiep;

	@Column(name = "SoQuyetDinhNhapHoc", length = 200)
	private String soQuyetDinhNhapHoc;

	/** 0 thôi học, 1 đang học, 2 bảo lưu, 3 tạm nghỉ, 4 đình chỉ, 5 tốt nghiệp */
	@Column(name = "TrangThai")
	private Integer trangThai;

	@Column(name = "NgayThayDoiTrangThai")
	private LocalDate ngayThayDoiTrangThai;

	@Column(name = "LyDoThayDoiTrangThai", length = 1000)
	private String lyDoThayDoiTrangThai;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** Dùng cho Thymeleaf hiển thị giới tính */
	@Transient
	public String getTenGioiTinh() {
		if (gioiTinh == null) return "";
		return switch (gioiTinh) {
			case 1 -> "Nam";
			case 2 -> "Nữ";
			default -> "Khác";
		};
	}

	@Transient
	public String getTenTrangThai() {
		if (trangThai == null) return "";
		return switch (trangThai) {
			case 0 -> "Thôi học";
			case 1 -> "Đang học";
			case 2 -> "Bảo lưu";
			case 3 -> "Tạm nghỉ";
			case 4 -> "Đình chỉ";
			case 5 -> "Đã tốt nghiệp";
			default -> "";
		};
	}
}