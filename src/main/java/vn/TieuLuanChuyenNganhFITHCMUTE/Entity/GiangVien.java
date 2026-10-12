package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "GiangVien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GiangVien {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaGiangVien")
	private Integer maGiangVien;

	/** Mã giảng viên thật, ví dụ GV001 */
	@Column(name = "MaSoGiangVien", length = 50, nullable = false, unique = true)
	private String maSoGiangVien;

	@Column(name = "HoTen", length = 200, nullable = false)
	private String hoTen;

	@Column(name = "NgaySinh")
	private LocalDate ngaySinh;

	/** 1 Nam, 2 Nữ */
	@Column(name = "GioiTinh")
	private Integer gioiTinh;

	@Column(name = "HocHam", length = 100)
	private String hocHam;

	@Column(name = "HocVi", length = 100)
	private String hocVi;

	@Column(name = "EmailTruong", length = 200)
	private String emailTruong;

	@Column(name = "SoDienThoai", length = 20)
	private String soDienThoai;

	@Column(name = "ChuyenMon", length = 300)
	private String chuyenMon;

	/** 1 cơ hữu, 2 thỉnh giảng, 3 kiêm nhiệm */
	@Column(name = "LoaiGiangVien", nullable = false)
	private Integer loaiGiangVien = 1;

	@Column(name = "NgayBatDauCongTac")
	private LocalDate ngayBatDauCongTac;

	/** 0 ngừng công tác, 1 đang công tác, 2 nghỉ phép, 3 tạm nghỉ */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaTaiKhoan", unique = true)
	private TaiKhoan taiKhoan;

	/** Khoa công tác */
	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaDonVi", nullable = false)
	private DonVi donVi;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	/** "TS. Nguyễn Văn A" — học vị viết tắt đứng trước tên */
	@Transient
	public String getTenHienThi() {
		return (hocVi != null && !hocVi.isBlank()) ? hocVi + ". " + hoTen : hoTen;
	}
}