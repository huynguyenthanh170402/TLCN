package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LichHoc")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LichHoc {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLichHoc")
	private Integer maLichHoc;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaLopHocPhan", nullable = false)
	private LopHocPhan lopHocPhan;

	/** 2 = Thứ Hai ... 7 = Thứ Bảy, 8 = Chủ nhật */
	@Column(name = "Thu", nullable = false)
	private Integer thu;

	@Column(name = "TietBatDau")
	private Integer tietBatDau;

	@Column(name = "SoTiet")
	private Integer soTiet;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaPhongHoc")
	private PhongHoc phongHoc;

	@Column(name = "NgayBatDau")
	private LocalDate ngayBatDau;

	@Column(name = "NgayKetThuc")
	private LocalDate ngayKetThuc;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	@Transient
	public int getTietKetThuc() {
		return tietBatDau + soTiet - 1;
	}

	@Transient
	public String getTenThu() {
		if (thu == null) return "";
		return thu == 8 ? "Chủ nhật" : "Thứ " + thu;
	}

	/** Hai buổi học có chồng giờ nhau không (cùng thứ, tiết giao nhau) */
	@Transient
	public boolean trungGio(LichHoc khac) {
		if (!thu.equals(khac.thu)) return false;
		return tietBatDau <= khac.getTietKetThuc() && khac.tietBatDau <= getTietKetThuc();
	}
}