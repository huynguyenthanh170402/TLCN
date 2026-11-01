package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhanTienQuyet;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService.BoiCanh;

/**
 * Dựng màn hình đăng ký học phần theo chương trình đào tạo của ngành.
 *
 * Trước đây màn hình liệt kê các lớp đang mở trong học kỳ. Cách đó sai với
 * cách sinh viên thật sự nghĩ: họ mở trang lên để xem "mình còn nợ môn nào
 * của ngành, môn nào kỳ này học được". Danh sách lớp phẳng không trả lời
 * được câu đó — môn chưa tới lượt học thì biến mất khỏi màn hình thay vì
 * nói rõ vì sao chưa học được.
 *
 * Nên ở đây lật lại: lấy khung chương trình của ngành làm gốc, xếp theo học
 * kỳ dự kiến, rồi gắn trạng thái cho từng học phần. Môn nào kỳ này mở lớp và
 * đủ điều kiện thì tô xanh cho đăng ký; môn nào chưa được thì vẫn nằm đó
 * kèm lý do cụ thể.
 *
 * Service này chỉ sắp xếp lại dữ liệu để hiển thị. Mọi quyết định cho phép
 * hay không vẫn do DangKyHocPhanService quyết, và chính nó kiểm lại lần nữa
 * lúc bấm nút đăng ký.
 */
@Service
@RequiredArgsConstructor
public class KeHoachDangKyService {

	private static final int CTDT_DANG_AP_DUNG = 1;
	private static final int LOAI_LOP_CAI_THIEN = 2;

	private final DangKyHocPhanService dangKyHocPhanService;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;

	// ===================== KIỂU DỮ LIỆU CHO GIAO DIỆN =====================

	/** Một lớp đang mở của học phần, kèm lý do nếu không đăng ký được */
	@Getter
	public static class LopKemLyDo {
		private final LopHocPhan lop;
		private final long siSo;
		private final String lyDo;      // null nghĩa là đăng ký được

		LopKemLyDo(LopHocPhan lop, long siSo, String lyDo) {
			this.lop = lop; this.siSo = siSo; this.lyDo = lyDo;
		}

		public boolean isDangKyDuoc() { return lyDo == null; }

		public boolean isDayLop() {
			return lop.getSiSoToiDa() != null && siSo >= lop.getSiSoToiDa();
		}
	}

	/**
	 * Một học phần trong chương trình, nhìn dưới góc độ đăng ký kỳ này.
	 *
	 * Dùng lớp có @Getter thay vì record: Thymeleaf đọc thuộc tính theo quy
	 * ước getX(), còn record sinh ra x() nên truy cập không chắc chắn.
	 */
	@Getter
	public static class MucDangKy {
		private final ChuongTrinhDaoTao ct;
		private final List<LopKemLyDo> dsLop;
		private final DangKyHocPhan dangKy;   // null nếu chưa đăng ký kỳ này
		private final boolean daDat;
		private final String lyDo;            // lý do ở mức học phần

		MucDangKy(ChuongTrinhDaoTao ct, List<LopKemLyDo> dsLop,
		          DangKyHocPhan dangKy, boolean daDat, String lyDo) {
			this.ct = ct; this.dsLop = dsLop;
			this.dangKy = dangKy; this.daDat = daDat; this.lyDo = lyDo;
		}

		public HocPhan getHocPhan() { return ct.getHocPhan(); }

		/* --- Bốn trạng thái loại trừ nhau, giao diện chỉ cần hỏi đúng một câu --- */

		/** Đã đăng ký trong học kỳ này. Xét trước tiên vì nó đè lên mọi trạng thái khác. */
		public boolean isDaDangKy() { return dangKy != null; }

		/**
		 * Kỳ này mở lớp và có ít nhất một lớp đăng ký được → tô xanh nhạt.
		 *
		 * Lấy thẳng từ kết quả xét của từng lớp chứ không tự suy luận, nên
		 * màn hình không bao giờ tô xanh một môn mà bấm vào lại báo lỗi.
		 */
		public boolean isDangKyDuoc() {
			return !isDaDangKy() && dsLop.stream().anyMatch(LopKemLyDo::isDangKyDuoc);
		}

		/** Kỳ này có mở lớp nhưng không lớp nào đăng ký được */
		public boolean isVuongDieuKien() {
			return !isDaDangKy() && !dsLop.isEmpty() && !isDangKyDuoc();
		}

		/** Kỳ này không mở lớp nào cho học phần này */
		public boolean isChuaMo() { return !isDaDangKy() && dsLop.isEmpty(); }

		/** Câu giải thích hiện ra khi sinh viên bấm vào dòng không đăng ký được */
		public String getGiaiThich() {
			if (lyDo != null) return lyDo;
			if (isChuaMo())
				return "Học kỳ này không mở lớp nào cho học phần. Chờ Phòng Đào tạo "
						+ "mở lớp ở học kỳ sau, hoặc theo dõi đợt đăng ký bổ sung.";
			// Còn lại là vướng ở mức lớp: sĩ số, trùng giờ, giới hạn tín chỉ
			return dsLop.stream().map(LopKemLyDo::getLyDo)
					.filter(l -> l != null).findFirst()
					.orElse("Không đăng ký được học phần này.");
		}

		/** Màu nền của dòng trong bảng */
		public String getMauDong() {
			if (isDaDangKy())    return "table-primary";
			if (isDangKyDuoc())  return "table-success";   // xanh nhạt
			if (isChuaMo())      return "table-light";
			return "";
		}
	}

	/** Các học phần cùng một học kỳ dự kiến của chương trình */
	@Getter
	public static class NhomHocKy {
		private final int hocKyDuKien;
		private final List<MucDangKy> dsMuc = new ArrayList<>();

		NhomHocKy(int hocKyDuKien) { this.hocKyDuKien = hocKyDuKien; }

		public int getSoTinChi() {
			return dsMuc.stream().mapToInt(m -> soTinChi(m.getHocPhan())).sum();
		}

		public long getSoMonDangKyDuoc() {
			return dsMuc.stream().filter(MucDangKy::isDangKyDuoc).count();
		}

		public long getSoMonConLai() {
			return dsMuc.stream().filter(m -> !m.isDaDat()).count();
		}
	}

	/** Toàn bộ dữ liệu màn hình đăng ký */
	@Getter
	public static class KeHoach {
		/** Chỉ gồm môn đăng ký được kỳ này và môn đã đăng ký, gom theo học kỳ */
		private final List<NhomHocKy> dsNhom = new ArrayList<>();

		/**
		 * Môn tới lượt học nhưng kỳ này chưa đụng vào được: kỳ này không mở
		 * lớp, chưa đạt môn tiên quyết, lớp đã đầy... Để riêng ra một chỗ gấp
		 * lại, không trộn vào danh sách chính.
		 */
		private final List<MucDangKy> dsChuaDuoc = new ArrayList<>();

		private final List<MucDangKy> dsDaDat = new ArrayList<>();
		private boolean coChuongTrinh;

		/* Môn của các học kỳ sau không liệt kê ra, chỉ đếm để nói cho sinh
		   viên biết chương trình còn bao nhiêu phần phía trước. */
		private int soMonKySau;
		private int soTinChiKySau;

		public int getSoMonDaDat()   { return dsDaDat.size(); }
		public int getSoTinChiDaDat() {
			return dsDaDat.stream().mapToInt(m -> soTinChi(m.getHocPhan())).sum();
		}
		public int getSoMonChuaDuoc() { return dsChuaDuoc.size(); }

		/** Số môn đăng ký được hoặc đã đăng ký, tức số dòng của danh sách chính */
		public int getSoMonMoRa() {
			return dsNhom.stream().mapToInt(n -> n.getDsMuc().size()).sum();
		}

		public int getTongSoMon() {
			return getSoMonDaDat() + soMonKySau + getSoMonChuaDuoc() + getSoMonMoRa();
		}
	}

	private static int soTinChi(HocPhan hp) {
		return hp.getSoTinChi() == null ? 0 : hp.getSoTinChi();
	}

	// ===================== DỰNG KẾ HOẠCH =====================

	@Transactional(readOnly = true)
	public KeHoach keHoach(SinhVien sv, HocKy hk, BoiCanh bc) {
		KeHoach kh = new KeHoach();
		if (sv == null || sv.getNganh() == null) return kh;

		List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				sv.getNganh().getMaNganh(), CTDT_DANG_AP_DUNG,
				Sort.by("hocKyDuKien", "khoiKienThuc"));
		if (khung.isEmpty()) return kh;
		kh.coChuongTrinh = true;

		Map<Integer, List<LopHocPhan>> lopTheoMon = lopDangMoTheoHocPhan(hk);
		Map<Integer, Long> siSo = dangKyHocPhanService.siSoCacLop(
				lopTheoMon.values().stream().flatMap(List::stream).toList());
		Map<Integer, DangKyHocPhan> dangKyTheoMon = dangKyTheoHocPhan(bc);

		// TreeMap để học kỳ 1, 2, 3... ra đúng thứ tự mà không phải sắp lại
		Map<Integer, NhomHocKy> theoHocKy = new TreeMap<>();

		for (ChuongTrinhDaoTao ct : khung) {
			Integer maHp = ct.getHocPhan().getMaHocPhanId();
			boolean daDat = bc.getHocPhanDaDat().contains(maHp);
			DangKyHocPhan dk = dangKyTheoMon.get(maHp);

			List<LopKemLyDo> dsLop = new ArrayList<>();
			for (LopHocPhan lop : lopTheoMon.getOrDefault(maHp, List.of())) {
				long hienTai = siSo.getOrDefault(lop.getMaLopHocPhan(), 0L);
				dsLop.add(new LopKemLyDo(lop, hienTai,
						dangKyHocPhanService.lyDoKhongDangKyDuoc(bc, lop, hienTai)));
			}
			// Lớp đăng ký được xếp lên đầu để sinh viên thấy ngay
			dsLop.sort(Comparator.comparing(LopKemLyDo::isDangKyDuoc).reversed()
					.thenComparing(l -> l.getLop().getMaLop()));

			MucDangKy muc = new MucDangKy(ct, dsLop, dk, daDat, lyDoMucHocPhan(bc, ct, daDat, dsLop));

			// Môn đã đạt tách riêng: sinh viên mở trang này để tìm môn còn nợ,
			// trộn chung vào thì danh sách dài gấp đôi mà chẳng để làm gì.
			//
			// Trừ khi kỳ này có lớp cải thiện đăng ký được. Giấu nó đi thì
			// sinh viên muốn nâng điểm không còn chỗ nào bấm đăng ký.
			boolean coCuaDeVao = muc.isDangKyDuoc();
			if (daDat && dk == null && !coCuaDeVao) {
				kh.dsDaDat.add(muc);
				continue;
			}

			/*
			 * Môn của những học kỳ còn ở phía trước thì không bày ra đây.
			 *
			 * Sinh viên năm hai kỳ một mở trang này là để chọn môn của chính
			 * kỳ này và trả những môn còn nợ. Đổ cả khung bốn năm ra thì danh
			 * sách dài gấp mấy lần, mà phần lớn chỉ để nhìn chứ không bấm
			 * được, lẫn vào giữa đúng mấy môn cần chọn. Muốn xem toàn bộ
			 * khung chương trình thì đã có trang Chương trình đào tạo.
			 *
			 * Hai trường hợp vẫn giữ lại: đã lỡ đăng ký rồi (giấu đi thì mất
			 * luôn chỗ hủy) và kỳ này thật sự đăng ký được — lớp học vượt hay
			 * lớp hè không bị luật học kỳ chặn, giấu đi thì sinh viên muốn học
			 * vượt không còn nút nào để bấm.
			 */
			if (chuaToiLuot(ct, bc) && !muc.isDaDangKy() && !coCuaDeVao) {
				kh.soMonKySau++;
				kh.soTinChiKySau += soTinChi(muc.getHocPhan());
				continue;
			}

			/*
			 * Môn tới lượt rồi nhưng kỳ này vẫn chưa đụng vào được thì đẩy
			 * xuống khối gấp lại phía dưới.
			 *
			 * Danh sách chính là chỗ để bấm, nên chỉ chứa thứ bấm được. Trộn
			 * chung thì một màn hình ba chục dòng chỉ có vài dòng xanh, sinh
			 * viên phải lướt tìm. Vẫn giữ lại chúng ở khối riêng chứ không xóa
			 * hẳn: câu "vì sao môn này chưa học được" là câu sinh viên hỏi
			 * nhiều nhất, không có chỗ trả lời thì họ gọi lên Phòng Đào tạo.
			 */
			if (!coCuaDeVao && !muc.isDaDangKy()) {
				kh.dsChuaDuoc.add(muc);
				continue;
			}

			theoHocKy.computeIfAbsent(ct.getHocKyDuKien() == null ? 0 : ct.getHocKyDuKien(),
					NhomHocKy::new).dsMuc.add(muc);
		}

		kh.dsNhom.addAll(theoHocKy.values());
		return kh;
	}

	/**
	 * Học phần này thuộc một học kỳ còn ở phía trước hay chưa.
	 *
	 * Môn chưa xếp học kỳ (hocKyDuKien null) thì không chặn: phần lớn là môn
	 * tự chọn, học kỳ nào mở lớp là học được.
	 */
	private boolean chuaToiLuot(ChuongTrinhDaoTao ct, BoiCanh bc) {
		Integer hkDuKien = ct.getHocKyDuKien();
		return hkDuKien != null && hkDuKien > bc.getHocKyThu();
	}

	/** Các lớp đang mở trong học kỳ, gom theo học phần */
	private Map<Integer, List<LopHocPhan>> lopDangMoTheoHocPhan(HocKy hk) {
		Map<Integer, List<LopHocPhan>> kq = new HashMap<>();
		for (LopHocPhan lop : dangKyHocPhanService.dsLopDangMo(hk)) {
			kq.computeIfAbsent(lop.getHocPhan().getMaHocPhanId(), k -> new ArrayList<>()).add(lop);
		}
		return kq;
	}

	/** Mã học phần → lượt đăng ký của học kỳ này */
	private Map<Integer, DangKyHocPhan> dangKyTheoHocPhan(BoiCanh bc) {
		Map<Integer, DangKyHocPhan> kq = new HashMap<>();
		for (DangKyHocPhan dk : bc.getDaDangKy()) {
			kq.put(dk.getLopHocPhan().getHocPhan().getMaHocPhanId(), dk);
		}
		return kq;
	}

	/**
	 * Lý do không đăng ký được, xét ở mức học phần chứ không phải mức lớp.
	 *
	 * Có hai mức lý do khác nhau. Mức học phần là những thứ đúng với mọi lớp
	 * của môn đó — chưa tới học kỳ trong chương trình, chưa đạt môn tiên
	 * quyết. Mức lớp là những thứ chỉ đúng với một lớp cụ thể — lớp đầy, lớp
	 * trùng giờ, lớp dành cho khóa khác. Tách ra vì câu trả lời cho sinh viên
	 * khác nhau: mức học phần nghĩa là kỳ này đừng mong học, mức lớp nghĩa là
	 * đổi lớp khác thì được.
	 *
	 * Trả về null khi mức học phần không vướng gì. Lúc đó nếu vẫn không đăng
	 * ký được thì lý do nằm ở từng lớp, giao diện sẽ lấy lý do của lớp.
	 *
	 * Hàm này chỉ để hiển thị. Quyền quyết định vẫn thuộc về
	 * DangKyHocPhanService.lyDoKhongDangKyDuoc, và trạng thái "được đăng ký"
	 * của giao diện cũng lấy từ đó chứ không lấy từ đây.
	 */
	private String lyDoMucHocPhan(BoiCanh bc, ChuongTrinhDaoTao ct,
	                              boolean daDat, List<LopKemLyDo> dsLop) {
		if (bc.getLoiChung() != null) return bc.getLoiChung();

		Integer maHp = ct.getHocPhan().getMaHocPhanId();

		// Đã đạt mà vẫn còn hiện ở đây nghĩa là sinh viên muốn cải thiện điểm
		if (daDat) {
			boolean coLopCaiThien = dsLop.stream().anyMatch(
					l -> Integer.valueOf(LOAI_LOP_CAI_THIEN).equals(l.getLop().getLoaiLop()));
			if (!coLopCaiThien)
				return "Bạn đã học đạt học phần này. Muốn nâng điểm thì chờ lớp cải thiện được mở.";
			return null;
		}

		// Chưa tới lượt học phần này trong khung chương trình
		Integer hocKyCuaMon = bc.getHocKyDuKien().get(maHp);
		if (hocKyCuaMon != null && hocKyCuaMon > bc.getHocKyThu())
			return "Học phần thuộc học kỳ " + hocKyCuaMon + " của chương trình, "
					+ "bạn đang ở học kỳ " + bc.getHocKyThu() + ". Chưa tới lượt học.";

		// Điều kiện tiên quyết, học trước, song hành
		Set<Integer> daDatTatCa = bc.getHocPhanDaDat();
		Set<Integer> daHoc = bc.getHocPhanDaHoc();
		Set<Integer> daDangKy = bc.getHocPhanDaDangKy();

		for (HocPhanTienQuyet tq : bc.getTienQuyet().getOrDefault(maHp, List.of())) {
			Integer maDk = tq.getHocPhanDieuKien().getMaHocPhanId();
			String tenDk = tq.getHocPhanDieuKien().getTenHocPhan();

			if (tq.getLoaiDieuKien() == 1 && !daDatTatCa.contains(maDk))
				return "Chưa đạt học phần tiên quyết \"" + tenDk + "\".";

			if (tq.getLoaiDieuKien() == 2 && !daHoc.contains(maDk))
				return "Phải học \"" + tenDk + "\" trước học phần này.";

			if (tq.getLoaiDieuKien() == 3 && !daHoc.contains(maDk) && !daDangKy.contains(maDk))
				return "Phải học \"" + tenDk + "\" trước hoặc cùng học kỳ này.";
		}

		return null;
	}
}
