package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhanTienQuyet;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.CongNoHocPhiRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanTienQuyetRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;

/**
 * Đăng ký học phần của sinh viên, kèm toàn bộ quy tắc xét duyệt.
 * Mọi kiểm tra đều chạy ở server, giao diện chỉ hiển thị lại cho dễ nhìn.
 */
@Service
@RequiredArgsConstructor
public class DangKyHocPhanService {

	/** Giới hạn tín chỉ một học kỳ */
	public static final int TIN_CHI_TOI_DA = 25;
	/** Sinh viên có GPA dưới mức này bị hạn chế số tín chỉ */
	public static final int TIN_CHI_HOC_LUC_YEU = 14;
	private static final BigDecimal GPA_CANH_BAO = new BigDecimal("2.0");

	private static final int HOC_KY_DANG_DIEN_RA = 1;
	private static final int LOP_DANG_MO = 1;
	private static final int DA_DANG_KY = 1;
	private static final int DA_HUY = 2;
	private static final int SV_DANG_HOC = 1;
	private static final int DIEM_DA_CONG_BO = 3;
	private static final int CONG_NO_DA_DONG_DU = 2;

	private final HocKyRepository hocKyRepository;
	private final LopHocPhanRepository lopHocPhanRepository;
	private final DangKyHocPhanRepository dangKyRepository;
	private final KetQuaHocPhanRepository ketQuaRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;
	private final HocPhanTienQuyetRepository tienQuyetRepository;
	private final CongNoHocPhiRepository congNoRepository;
	private final HocPhiService hocPhiService;
	private final LichHocService lichHocService;
	private final KetQuaHocTapService ketQuaHocTapService;

	/**
	 * Toàn bộ dữ liệu cần để xét một lượt đăng ký, tính sẵn một lần
	 * rồi dùng lại cho cả danh sách lớp, tránh truy vấn lặp.
	 */
	@Getter
	public static class BoiCanh {
		private SinhVien sinhVien;
		private HocKy hocKy;
		private List<DangKyHocPhan> daDangKy;
		private int tongTinChiDaDangKy;
		private int gioiHanTinChi;
		private int hocKyThu;                    // sinh viên đang ở học kỳ thứ mấy
		private Set<Integer> hocPhanDaDat;       // đã học đạt, không cần học lại
		private Set<Integer> hocPhanDaDangKy;    // đã đăng ký trong học kỳ này
		private Set<Integer> hocPhanTrongCTDT;   // null nghĩa là ngành chưa có chương trình
		private Map<Integer, Integer> hocKyDuKien;  // mã học phần → học kỳ trong chương trình
		private Set<Integer> hocPhanDaHoc;       // đã học, đạt hay chưa cũng tính
		private Map<Integer, List<HocPhanTienQuyet>> tienQuyet;
		private List<LichHoc> lichDaDangKy;
		private String loiChung;                 // lỗi chặn toàn bộ, ví dụ còn nợ học phí
	}

	// ===================== ĐỌC DỮ LIỆU =====================

	public Optional<HocKy> hocKyHienTai() {
		return hocKyRepository.findFirstByTrangThaiOrderByNgayBatDauDesc(HOC_KY_DANG_DIEN_RA);
	}

	public List<LopHocPhan> dsLopDangMo(HocKy hk) {
		return lopHocPhanRepository.findByHocKy_MaHocKyAndTrangThai(
				hk.getMaHocKy(), LOP_DANG_MO, Sort.by("hocPhan.maHocPhan", "maLop"));
	}

	public List<DangKyHocPhan> dsDaDangKy(SinhVien sv, HocKy hk) {
		return dangKyRepository.findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThai(
				sv.getMaSinhVien(), hk.getMaHocKy(), DA_DANG_KY);
	}

	public Map<Integer, Long> siSoCacLop(List<LopHocPhan> dsLop) {
		Map<Integer, Long> kq = new HashMap<>();
		for (LopHocPhan lop : dsLop) {
			kq.put(lop.getMaLopHocPhan(),
					dangKyRepository.countByLopHocPhan_MaLopHocPhanAndTrangThai(lop.getMaLopHocPhan(), DA_DANG_KY));
		}
		return kq;
	}

	public int tongTinChi(List<DangKyHocPhan> ds) {
		return ds.stream().mapToInt(dk -> dk.getLopHocPhan().getHocPhan().getSoTinChi()).sum();
	}

	// ===================== BỐI CẢNH =====================

	@Transactional(readOnly = true)
	public BoiCanh boiCanh(SinhVien sv, HocKy hk) {
		BoiCanh bc = new BoiCanh();
		bc.sinhVien = sv;
		bc.hocKy = hk;
		bc.daDangKy = dsDaDangKy(sv, hk);
		bc.tongTinChiDaDangKy = tongTinChi(bc.daDangKy);
		bc.hocKyThu = tinhHocKyThu(sv, hk);

		bc.hocPhanDaDangKy = new HashSet<>();
		for (DangKyHocPhan dk : bc.daDangKy)
			bc.hocPhanDaDangKy.add(dk.getLopHocPhan().getHocPhan().getMaHocPhanId());

		// Môn đã học đạt thì không cho đăng ký lại
		bc.hocPhanDaDat = new HashSet<>();
		List<KetQuaHocPhan> dsKetQua = ketQuaRepository
				.findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(
						sv.getMaSinhVien(), DIEM_DA_CONG_BO, Sort.by("maKetQuaHocPhan"));
		for (KetQuaHocPhan kq : dsKetQua) {
			if (Integer.valueOf(1).equals(kq.getKetQua()))
				bc.hocPhanDaDat.add(kq.getDangKyHocPhan().getLopHocPhan().getHocPhan().getMaHocPhanId());
		}

		// Môn đã học, kể cả chưa đạt, dùng cho điều kiện "học trước"
		bc.hocPhanDaHoc = new HashSet<>(bc.hocPhanDaDangKy);
		for (KetQuaHocPhan kq : dsKetQua)
			bc.hocPhanDaHoc.add(kq.getDangKyHocPhan().getLopHocPhan().getHocPhan().getMaHocPhanId());
		bc.hocPhanDaHoc.addAll(bc.hocPhanDaDat);

		// Chương trình đào tạo của ngành
		bc.hocKyDuKien = new HashMap<>();
		if (sv.getNganh() != null) {
			List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
					sv.getNganh().getMaNganh(), 1, Sort.by("hocKyDuKien"));
			if (!khung.isEmpty()) {
				bc.hocPhanTrongCTDT = new HashSet<>();
				for (ChuongTrinhDaoTao ct : khung) {
					Integer maHp = ct.getHocPhan().getMaHocPhanId();
					bc.hocPhanTrongCTDT.add(maHp);
					bc.hocKyDuKien.put(maHp, ct.getHocKyDuKien());
				}
			}
		}

		// Điều kiện tiên quyết của các môn trong chương trình
		bc.tienQuyet = new HashMap<>();
		if (bc.hocPhanTrongCTDT != null && !bc.hocPhanTrongCTDT.isEmpty()) {
			for (HocPhanTienQuyet tq : tienQuyetRepository.findByHocPhan_MaHocPhanIdInAndTrangThai(
					bc.hocPhanTrongCTDT, 1)) {
				bc.tienQuyet.computeIfAbsent(tq.getHocPhan().getMaHocPhanId(),
						k -> new java.util.ArrayList<>()).add(tq);
			}
		}

		// Giới hạn tín chỉ theo học lực
		BigDecimal gpa = ketQuaHocTapService.tinhTongKet(
				ketQuaHocTapService.tatCa(sv.getMaSinhVien())).getGpa();
		bc.gioiHanTinChi = (gpa.signum() > 0 && gpa.compareTo(GPA_CANH_BAO) < 0)
				? TIN_CHI_HOC_LUC_YEU : TIN_CHI_TOI_DA;

		bc.lichDaDangKy = lichHocService.lichCuaCacDangKy(bc.daDangKy);
		bc.loiChung = kiemTraChung(sv, hk);
		return bc;
	}

	/**
	 * Sinh viên đang ở học kỳ thứ mấy của chương trình.
	 * Khóa 2022, học kỳ 1 năm học 2026-2027 → năm thứ 5, học kỳ thứ 9.
	 */
	private int tinhHocKyThu(SinhVien sv, HocKy hk) {
		try {
			int khoa = Integer.parseInt(sv.getKhoaTuyenSinh().trim());
			int namBatDau = Integer.parseInt(hk.getNamHoc().substring(0, 4));
			int hocKySo = hk.getHocKySo() == null ? 1 : hk.getHocKySo();
			return Math.max(1, (namBatDau - khoa) * 2 + hocKySo);
		} catch (Exception e) {
			// Thiếu khóa tuyển sinh hoặc dữ liệu lạ: không hạn chế theo học kỳ
			return 99;
		}
	}

	/** Các lỗi chặn toàn bộ việc đăng ký, không phụ thuộc lớp nào */
	private String kiemTraChung(SinhVien sv, HocKy hk) {
		if (!hk.isDangMoDangKy())
			return "Hiện không trong thời gian đăng ký học phần.";

		if (sv.getTrangThai() != null && sv.getTrangThai() != SV_DANG_HOC)
			return "Trạng thái của bạn là \"" + sv.getTenTrangThai()
					+ "\" nên không đăng ký học phần được. Liên hệ Phòng Đào tạo.";

		// Nợ học phí của học kỳ trước
		for (CongNoHocPhi cn : hocPhiService.dsCongNo(sv)) {
			boolean kyTruoc = !cn.getHocKy().getMaHocKy().equals(hk.getMaHocKy());
			boolean conNo = cn.getSoTienConLai().signum() > 0 && cn.getTrangThai() != CONG_NO_DA_DONG_DU;
			if (kyTruoc && conNo)
				return "Bạn còn nợ học phí " + cn.getHocKy().getMaHocKyCode()
						+ ". Đóng đủ rồi mới đăng ký học phần được.";
		}
		return null;
	}

	// ===================== XÉT TỪNG LỚP =====================

	/**
	 * Lý do không đăng ký được lớp này, null nghĩa là đăng ký được.
	 * Dùng chung cho cả giao diện lẫn lúc bấm nút đăng ký.
	 */
	public String lyDoKhongDangKyDuoc(BoiCanh bc, LopHocPhan lop, long siSoHienTai) {
		if (bc.loiChung != null) return bc.loiChung;
		final SinhVien sinhVien = bc.getSinhVien();

		Integer maHp = lop.getHocPhan().getMaHocPhanId();

		if (lop.getTrangThai() != LOP_DANG_MO)
			return "Lớp không mở đăng ký.";

		if (bc.hocPhanDaDangKy.contains(maHp))
			return null;   // đã đăng ký rồi, giao diện hiện nhãn riêng

		boolean lopChinhKhoa = lop.getLoaiLop() == null || lop.getLoaiLop() == 1;

		// Môn đã đạt: chỉ học lại được ở lớp cải thiện
		if (bc.hocPhanDaDat.contains(maHp) && lop.getLoaiLop() != null && lop.getLoaiLop() != 2)
			return "Bạn đã học đạt học phần này. Muốn nâng điểm thì đăng ký lớp cải thiện.";

		// Lớp chính khóa mở cho một khóa cụ thể
		if (lopChinhKhoa && lop.getKhoaApDung() != null
				&& sinhVien.getKhoaTuyenSinh() != null
				&& !lop.getKhoaApDung().equals(sinhVien.getKhoaTuyenSinh().trim()))
			return "Lớp chính khóa dành cho khóa " + lop.getKhoaApDung()
					+ ". Bạn học khóa " + sinhVien.getKhoaTuyenSinh()
					+ ", hãy chọn lớp học lại hoặc lớp của khóa mình.";

		// Môn phải nằm trong chương trình đào tạo của ngành
		if (bc.hocPhanTrongCTDT != null && !bc.hocPhanTrongCTDT.contains(maHp))
			return "Học phần không thuộc chương trình đào tạo ngành của bạn.";

		// Phải tới học kỳ đó mới được đăng ký. Lớp học lại, học hè, lớp tối thì bỏ qua luật này.
		Integer hocKyCuaMon = bc.hocKyDuKien.get(maHp);
		if (lopChinhKhoa && hocKyCuaMon != null && hocKyCuaMon > bc.hocKyThu)
			return "Học phần thuộc học kỳ " + hocKyCuaMon
					+ " của chương trình, bạn đang ở học kỳ " + bc.hocKyThu + ".";

		// Điều kiện tiên quyết
		for (HocPhanTienQuyet tq : bc.tienQuyet.getOrDefault(maHp, List.of())) {
			Integer maDk = tq.getHocPhanDieuKien().getMaHocPhanId();
			String tenDk = tq.getHocPhanDieuKien().getTenHocPhan();

			if (tq.getLoaiDieuKien() == 1 && !bc.hocPhanDaDat.contains(maDk))
				return "Chưa đạt học phần tiên quyết \"" + tenDk + "\".";

			if (tq.getLoaiDieuKien() == 2 && !bc.hocPhanDaHoc.contains(maDk))
				return "Phải học \"" + tenDk + "\" trước học phần này.";

			if (tq.getLoaiDieuKien() == 3
					&& !bc.hocPhanDaHoc.contains(maDk) && !bc.hocPhanDaDangKy.contains(maDk))
				return "Phải học \"" + tenDk + "\" trước hoặc cùng học kỳ này.";
		}

		// Sĩ số
		if (lop.getSiSoToiDa() != null && siSoHienTai >= lop.getSiSoToiDa())
			return "Lớp đã đủ sĩ số.";

		// Giới hạn tín chỉ
		int tcSau = bc.tongTinChiDaDangKy + lop.getHocPhan().getSoTinChi();
		if (tcSau > bc.gioiHanTinChi)
			return "Vượt giới hạn " + bc.gioiHanTinChi + " tín chỉ của học kỳ"
					+ (bc.gioiHanTinChi == TIN_CHI_HOC_LUC_YEU ? " (do học lực yếu)." : ".");

		// Trùng giờ với lớp đã đăng ký
		for (LichHoc a : lichHocService.lichCuaLop(lop.getMaLopHocPhan())) {
			for (LichHoc b : bc.lichDaDangKy) {
				if (a.trungGio(b))
					return "Trùng giờ với lớp " + b.getLopHocPhan().getMaLop()
							+ " (" + b.getTenThu() + ", tiết " + b.getTietBatDau()
							+ "–" + b.getTietKetThuc() + ").";
			}
		}
		return null;
	}

	/** Lý do của từng lớp trong danh sách, để giao diện hiện nút hoặc nhãn */
	public Map<Integer, String> lyDoTungLop(BoiCanh bc, List<LopHocPhan> dsLop, Map<Integer, Long> siSo) {
		Map<Integer, String> kq = new HashMap<>();
		for (LopHocPhan lop : dsLop) {
			long hienTai = siSo.getOrDefault(lop.getMaLopHocPhan(), 0L);
			kq.put(lop.getMaLopHocPhan(), lyDoKhongDangKyDuoc(bc, lop, hienTai));
		}
		return kq;
	}

	// ===================== ĐĂNG KÝ =====================

	@Transactional
	public void dangKy(SinhVien sv, Integer maLopHocPhan) {
		LopHocPhan lop = lopHocPhanRepository.findById(maLopHocPhan)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
		HocKy hk = lop.getHocKy();

		BoiCanh bc = boiCanh(sv, hk);
		Integer maHp = lop.getHocPhan().getMaHocPhanId();

		// Đã đăng ký một lớp khác của cùng học phần
		if (bc.getHocPhanDaDangKy().contains(maHp))
			throw new IllegalStateException("Bạn đã đăng ký một lớp của học phần "
					+ lop.getHocPhan().getTenHocPhan() + ".");

		long siSo = dangKyRepository.countByLopHocPhan_MaLopHocPhanAndTrangThai(maLopHocPhan, DA_DANG_KY);
		String lyDo = lyDoKhongDangKyDuoc(bc, lop, siSo);
		if (lyDo != null) throw new IllegalStateException(lyDo);

		// Đã từng đăng ký rồi hủy → dùng lại dòng cũ (ràng buộc UNIQUE)
		DangKyHocPhan dk = dangKyRepository
				.findBySinhVien_MaSinhVienAndLopHocPhan_MaLopHocPhan(sv.getMaSinhVien(), maLopHocPhan)
				.orElseGet(DangKyHocPhan::new);

		dk.setSinhVien(sv);
		dk.setLopHocPhan(lop);
		dk.setLoaiDangKy(bc.getHocPhanDaDat().contains(maHp) ? 2 : 1);   // 2 là học lại
		dk.setTrangThai(DA_DANG_KY);
		dk.setNgayDangKy(LocalDateTime.now());
		dk.setNgayHuy(null);
		dk.setLyDoHuy(null);
		dangKyRepository.saveAndFlush(dk);

		hocPhiService.capNhatCongNo(sv, hk);
	}

	// ===================== HỦY =====================

	@Transactional
	public void huy(SinhVien sv, Integer maDangKy) {
		DangKyHocPhan dk = dangKyRepository.findById(maDangKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt đăng ký."));

		if (!dk.getSinhVien().getMaSinhVien().equals(sv.getMaSinhVien()))
			throw new IllegalStateException("Bạn không có quyền hủy đăng ký này.");
		if (dk.getTrangThai() != DA_DANG_KY)
			throw new IllegalStateException("Lượt đăng ký này không còn hiệu lực.");

		HocKy hk = dk.getLopHocPhan().getHocKy();
		if (!hk.isDangMoDangKy())
			throw new IllegalStateException("Đã hết thời gian đăng ký, không thể hủy.");

		// Đã có điểm thì không cho hủy
		boolean coDiem = !ketQuaRepository
				.findByDangKyHocPhan_MaDangKyHocPhanIn(List.of(dk.getMaDangKyHocPhan())).isEmpty();
		if (coDiem)
			throw new IllegalStateException("Lớp đã có điểm, không thể hủy đăng ký.");

		dk.setTrangThai(DA_HUY);
		dk.setNgayHuy(LocalDateTime.now());
		dk.setLyDoHuy("Sinh viên tự hủy");
		dangKyRepository.saveAndFlush(dk);

		hocPhiService.capNhatCongNo(sv, hk);
	}
}