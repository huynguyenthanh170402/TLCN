package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.CongNoHocPhiRepository;

/**
 * Phiếu xác nhận đăng ký học phần, gửi về hộp thư của sinh viên.
 *
 * Gửi theo phiếu chứ không gửi theo từng lần bấm đăng ký. Sinh viên thường
 * chọn mười mấy lớp rồi đổi tới đổi lui; gửi mỗi lần bấm một thư sẽ thành
 * mười mấy thư rác, mà thư nào cũng chỉ nói được một dòng nên chẳng dùng
 * vào việc gì. Ngoài đời cũng vậy: chốt xong mới in phiếu.
 *
 * Phiếu chỉ là bản sao để sinh viên đối chiếu và giữ làm bằng chứng. Dữ liệu
 * chính thức nằm trong cơ sở dữ liệu; gửi thư hỏng cũng không ảnh hưởng tới
 * kết quả đăng ký.
 */
@Service
@RequiredArgsConstructor
public class PhieuDangKyService {

	private static final DateTimeFormatter NGAY_GIO =
			DateTimeFormatter.ofPattern("HH:mm dd/MM/yyyy");
	private static final DateTimeFormatter NGAY =
			DateTimeFormatter.ofPattern("dd/MM/yyyy");

	private final DangKyHocPhanService dangKyHocPhanService;
	private final LichHocService lichHocService;
	private final CongNoHocPhiRepository congNoRepository;
	private final MailService mailService;

	public record KetQua(boolean thanhCong, String thongBao) { }

	/**
	 * Dựng phiếu từ dữ liệu đang có trong cơ sở dữ liệu rồi gửi đi.
	 *
	 * Để readOnly vì phiếu không sửa gì cả, chỉ đọc. Giao dịch mở suốt lời
	 * gọi nên các quan hệ LAZY (lớp học phần, học phần, phòng học) nạp được.
	 */
	@Transactional(readOnly = true)
	public KetQua guiPhieu(SinhVien sv, HocKy hk) {
		if (sv == null || hk == null) {
			return new KetQua(false, "Chưa xác định được sinh viên hoặc học kỳ.");
		}

		List<DangKyHocPhan> ds = dangKyHocPhanService.dsDaDangKy(sv, hk);
		if (ds.isEmpty()) {
			return new KetQua(false, "Bạn chưa đăng ký học phần nào trong học kỳ này.");
		}

		String nguoiNhan = emailNhan(sv);
		if (nguoiNhan == null) {
			return new KetQua(false,
					"Hồ sơ của bạn chưa khai email nào nên không có nơi gửi phiếu. "
					+ "Vào mục Hồ sơ để bổ sung email cá nhân.");
		}

		// Dấu tiếng Việt gửi được vì spring.mail.default-encoding mặc định là UTF-8
		String tieuDe = "Phiếu xác nhận đăng ký học phần - " + hk.getMaHocKyCode();
		boolean daGui = mailService.gui(nguoiNhan, tieuDe, noiDungPhieu(sv, hk, ds));

		return daGui
				? new KetQua(true, "Đã gửi phiếu xác nhận tới " + cheBot(nguoiNhan) + ".")
				: new KetQua(false,
						"Hệ thống thư đang không gửi được. Kết quả đăng ký của bạn vẫn "
						+ "được lưu đầy đủ, bạn thử gửi lại sau.");
	}

	/** Ưu tiên email cá nhân; chưa khai thì dùng email trường */
	private String emailNhan(SinhVien sv) {
		if (sv.getEmailCaNhan() != null && !sv.getEmailCaNhan().isBlank())
			return sv.getEmailCaNhan().trim();
		if (sv.getEmailTruong() != null && !sv.getEmailTruong().isBlank())
			return sv.getEmailTruong().trim();
		return null;
	}

	/**
	 * Che bớt địa chỉ khi hiện lên màn hình: ngu****@gmail.com
	 *
	 * Màn hình có thể đang chiếu lên máy chiếu hoặc có người đứng sau lưng,
	 * không cần phơi nguyên địa chỉ chỉ để báo đã gửi xong.
	 */
	private String cheBot(String email) {
		int at = email.indexOf('@');
		if (at <= 3) return email;
		return email.substring(0, 3) + "****" + email.substring(at);
	}

	// ===================== NỘI DUNG PHIẾU =====================

	private String noiDungPhieu(SinhVien sv, HocKy hk, List<DangKyHocPhan> ds) {
		StringBuilder sb = new StringBuilder();

		sb.append("PHIẾU XÁC NHẬN ĐĂNG KÝ HỌC PHẦN\n");
		sb.append("Trường Đại học Sư phạm Kỹ thuật Thành phố Hồ Chí Minh\n\n");

		sb.append("Sinh viên : ").append(sv.getHoTen()).append('\n');
		sb.append("MSSV      : ").append(sv.getMaSoSinhVien()).append('\n');
		if (sv.getNganh() != null) {
			sb.append("Ngành     : ").append(sv.getNganh().getTenNganh()).append('\n');
		}
		sb.append("Khóa      : ").append(sv.getKhoaTuyenSinh()).append('\n');
		sb.append("Học kỳ    : ").append(hk.getTenHocKy()).append('\n');
		sb.append("Lập lúc   : ").append(LocalDateTime.now().format(NGAY_GIO)).append("\n\n");

		sb.append("DANH SÁCH HỌC PHẦN ĐÃ ĐĂNG KÝ\n");
		sb.append("--------------------------------------------------------------\n");

		int tongTinChi = 0;
		int stt = 0;
		for (DangKyHocPhan dk : ds) {
			LopHocPhan lop = dk.getLopHocPhan();
			tongTinChi += lop.getHocPhan().getSoTinChi();
			stt++;

			sb.append(String.format("%d. %s - %s%n",
					stt, lop.getHocPhan().getMaHocPhan(), lop.getHocPhan().getTenHocPhan()));
			sb.append(String.format("   Lớp %s | %d tín chỉ | %s%n",
					lop.getMaLop(), lop.getHocPhan().getSoTinChi(), dk.getTenLoaiDangKy()));

			// Xếp theo thứ rồi theo tiết để sinh viên đọc theo đúng thứ tự trong tuần
			List<LichHoc> lich = lichHocService.lichCuaLop(lop.getMaLopHocPhan()).stream()
					.sorted(Comparator.comparing(LichHoc::getThu)
							.thenComparing(LichHoc::getTietBatDau))
					.toList();

			if (lich.isEmpty()) {
				sb.append("   Lịch học: chưa xếp\n");
			} else {
				for (LichHoc lh : lich) {
					sb.append(String.format("   %s, tiết %d-%d%s%s%n",
							lh.getTenThu(), lh.getTietBatDau(), lh.getTietKetThuc(),
							gio(lh.getTietBatDau()),
							lh.getPhongHoc() != null ? " | phòng " + lh.getPhongHoc().getMaPhong() : ""));
				}
			}
			sb.append('\n');
		}

		sb.append("--------------------------------------------------------------\n");
		sb.append("Tổng cộng : ").append(ds.size()).append(" học phần, ")
		  .append(tongTinChi).append(" tín chỉ\n\n");

		themHocPhi(sb, sv, hk);
		themLuuY(sb, hk);

		sb.append("\nĐây là thư tự động, vui lòng không trả lời.\n");
		return sb.toString();
	}

	/**
	 * Giờ bắt đầu của tiết, lấy từ bảng giờ dùng chung.
	 *
	 * Có kiểm tra chỉ số vì bảng chỉ liệt kê tới tiết 12; lớp xếp vào tiết
	 * ngoài khoảng đó thì bỏ trống giờ chứ không để văng lỗi cả phiếu.
	 */
	private String gio(Integer tiet) {
		if (tiet == null || tiet < 1 || tiet >= LichHocService.GIO_BAT_DAU.length) return "";
		return " (" + LichHocService.GIO_BAT_DAU[tiet] + ")";
	}

	/** Số tiền do Phòng Kế hoạch Tài chính tính, phiếu chỉ chép lại */
	private void themHocPhi(StringBuilder sb, SinhVien sv, HocKy hk) {
		CongNoHocPhi cn = congNoRepository
				.findBySinhVien_MaSinhVienAndHocKy_MaHocKy(sv.getMaSinhVien(), hk.getMaHocKy())
				.orElse(null);
		if (cn == null) return;

		sb.append("HỌC PHÍ HỌC KỲ NÀY\n");
		sb.append("  Phải đóng     : ").append(tien(cn.getSoTienPhaiDong())).append(" đ\n");
		if (cn.getSoTienMienGiam() != null && cn.getSoTienMienGiam().signum() > 0) {
			sb.append("  Miễn giảm     : ").append(tien(cn.getSoTienMienGiam())).append(" đ\n");
		}
		sb.append("  Đã đóng       : ").append(tien(cn.getSoTienDaDong())).append(" đ\n");
		sb.append("  Còn phải đóng : ").append(tien(cn.getSoTienConLai())).append(" đ\n");
		if (cn.getHanThanhToan() != null) {
			sb.append("  Hạn đóng      : ").append(cn.getHanThanhToan().format(NGAY)).append('\n');
		}
		sb.append('\n');
	}

	private void themLuuY(StringBuilder sb, HocKy hk) {
		sb.append("LƯU Ý\n");

		if (hk.getNgayDongDangKy() != null) {
			sb.append("  - Đợt đăng ký đóng lúc ")
			  .append(hk.getNgayDongDangKy().format(NGAY_GIO))
			  .append(". Trước giờ đó bạn còn sửa hoặc hủy được.\n");
		}
		sb.append("  - Sau khi đóng cổng đăng ký, lớp không đạt sĩ số tối thiểu sẽ bị hủy. "
				+ "Phòng Đào tạo sẽ thông báo để bạn chọn lớp khác.\n");
		sb.append("  - Phiếu này là bản sao để đối chiếu. Dữ liệu chính thức nằm trên hệ thống, "
				+ "xem ở mục Đăng ký học phần.\n");
		sb.append("  - Còn nợ học phí học kỳ trước thì học kỳ sau không đăng ký được.\n");
	}

	private String tien(BigDecimal so) {
		return String.format("%,d", so == null ? 0L : so.longValue());
	}
}
