package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Giấy xác nhận sinh viên do Phòng Công tác Sinh viên cấp.
 *
 * Lưu lại thành sổ chứ không in rồi thôi. Ngoài đời giấy này đi ra ngoài
 * trường — ngân hàng, ban chỉ huy quân sự, nơi làm thêm — nên khi có người
 * gọi về hỏi "giấy số GXN20261027-0003 có thật không" thì phải tra được ai
 * cấp, cấp ngày nào, cho mục đích gì, còn hiệu lực không.
 *
 * Giấy cấp sai thì thu hồi chứ không xóa: bản giấy đã nằm ngoài tay trường,
 * xóa khỏi hệ thống chỉ làm mất dấu vết chứ không thu lại được tờ giấy.
 */
@Entity
@Table(name = "GiayXacNhanSinhVien")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class GiayXacNhanSinhVien {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaGiay")
	private Integer maGiay;

	/** Ví dụ: GXN20261027-0003 */
	@Column(name = "SoGiay", length = 50, nullable = false, unique = true)
	private String soGiay;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaSinhVien", nullable = false)
	private SinhVien sinhVien;

	/** 1 vay vốn, 2 tạm hoãn nghĩa vụ quân sự, 3 vé xe buýt hoặc tàu,
	 *  4 việc làm thêm và thực tập, 5 bảo hiểm và trợ cấp, 6 mục đích khác */
	@Column(name = "MucDich", nullable = false)
	private Integer mucDich = 1;

	/** Chỉ dùng khi mục đích là 6 */
	@Column(name = "MucDichKhac", length = 500)
	private String mucDichKhac;

	/** Nơi nhận giấy, ví dụ "Ngân hàng Chính sách xã hội huyện Cần Giờ" */
	@Column(name = "NoiNhan", length = 300)
	private String noiNhan;

	@Column(name = "NgayCap", nullable = false)
	private LocalDate ngayCap = LocalDate.now();

	@Column(name = "HanSuDung")
	private LocalDate hanSuDung;

	/*
	 * Chụp lại lớp và trạng thái tại thời điểm cấp.
	 *
	 * Không đọc thẳng từ hồ sơ sinh viên lúc in lại, vì hồ sơ còn thay đổi:
	 * sinh viên hôm nay đang học, sang năm bảo lưu. In lại mà ra trạng thái
	 * mới thì bản in không còn khớp tờ giấy đã ký và đã đóng dấu.
	 */
	@Column(name = "LopLucCap", length = 200)
	private String lopLucCap;

	@Column(name = "TrangThaiLucCap")
	private Integer trangThaiLucCap;

	/** Tên đăng nhập của cán bộ đã cấp */
	@Column(name = "NguoiCap", length = 100)
	private String nguoiCap;

	@Column(name = "NgayTao", nullable = false)
	private LocalDateTime ngayTao = LocalDateTime.now();

	/** 0 đã thu hồi, 1 còn hiệu lực */
	@Column(name = "TrangThai", nullable = false)
	private Integer trangThai = 1;

	@Column(name = "LyDoThuHoi", length = 500)
	private String lyDoThuHoi;

	@Column(name = "GhiChu", length = 1000)
	private String ghiChu;

	// ===================== HIỂN THỊ =====================

	@Transient
	public String getTenMucDich() {
		if (mucDich == null) return "";
		return switch (mucDich) {
			case 1 -> "Vay vốn tín dụng dành cho học sinh, sinh viên";
			case 2 -> "Tạm hoãn gọi nhập ngũ trong thời gian học tập";
			case 3 -> "Làm vé xe buýt, vé tàu ưu đãi cho sinh viên";
			case 4 -> "Xin việc làm thêm, thực tập tại doanh nghiệp";
			case 5 -> "Hưởng chế độ bảo hiểm, trợ cấp theo quy định";
			default -> (mucDichKhac == null || mucDichKhac.isBlank())
					? "Mục đích khác" : mucDichKhac;
		};
	}

	@Transient
	public String getTenTrangThai() {
		return Integer.valueOf(0).equals(trangThai) ? "Đã thu hồi" : "Còn hiệu lực";
	}

	/** Trạng thái học tập của sinh viên ghi trên giấy, lấy bản đã chụp lúc cấp */
	@Transient
	public String getTenTrangThaiLucCap() {
		if (trangThaiLucCap == null) return "";
		return switch (trangThaiLucCap) {
			case 0 -> "Thôi học";
			case 1 -> "Đang học";
			case 2 -> "Bảo lưu";
			case 3 -> "Tạm nghỉ";
			case 4 -> "Đình chỉ";
			case 5 -> "Đã tốt nghiệp";
			default -> "";
		};
	}

	/** Đã bị thu hồi hoặc đã quá hạn thì không còn dùng được */
	@Transient
	public boolean isConHieuLuc() {
		if (Integer.valueOf(0).equals(trangThai)) return false;
		return hanSuDung == null || !hanSuDung.isBefore(LocalDate.now());
	}

	@Transient
	public boolean isQuaHan() {
		return Integer.valueOf(1).equals(trangThai)
				&& hanSuDung != null && hanSuDung.isBefore(LocalDate.now());
	}
}
