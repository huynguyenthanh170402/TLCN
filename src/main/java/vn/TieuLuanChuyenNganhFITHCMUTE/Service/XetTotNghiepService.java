package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonXetTotNghiep;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DotXetTotNghiep;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonXetTotNghiepRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DotXetTotNghiepRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KetQuaHocTapService.TongHopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KetQuaHocTapService.TongKet;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.RenLuyenService.KetQuaRenLuyen;

/**
 * Xét tốt nghiệp.
 *
 * Đây là nơi mọi phần của hệ thống gặp nhau: chương trình đào tạo quyết định
 * phải học những gì, kết quả học phần cho biết đã đạt tới đâu, công nợ cho
 * biết còn nợ tiền không, điểm rèn luyện cho biết quá trình ngoài giờ học.
 *
 * Mỗi điều kiện trả về kèm số liệu thật so với mức yêu cầu, chứ không chỉ
 * đạt hay không đạt. Sinh viên bị từ chối cần biết mình thiếu bao nhiêu tín
 * chỉ, nợ môn nào, chứ một chữ "chưa đủ điều kiện" thì họ không làm gì được.
 */
@Service
@RequiredArgsConstructor
public class XetTotNghiepService {

	/** Thời gian đào tạo tối đa, gấp đôi thời gian thiết kế 4 năm */
	public static final int SO_NAM_TOI_DA = 8;

	/** Điểm trung bình tích lũy tối thiểu, thang 4 */
	public static final BigDecimal GPA_TOI_THIEU = new BigDecimal("2.0");

	/** Điểm rèn luyện toàn khóa tối thiểu */
	public static final int DIEM_REN_LUYEN_TOI_THIEU = 80;

	private static final int SV_DANG_HOC = 1;
	private static final int SV_TOT_NGHIEP = 5;
	private static final int CTDT_DANG_AP_DUNG = 1;
	private static final int HOC_PHAN_BAT_BUOC = 1;
	private static final int CONG_NO_KHONG_PHAT_SINH = 3;

	// Trạng thái đợt
	public static final int DOT_NHAP = 0;
	public static final int DOT_NHAN_DON = 1;
	public static final int DOT_DA_CHOT = 2;
	public static final int DOT_CONG_BO = 3;

	// Trạng thái đơn
	public static final int DON_CHO_XET = 1;
	public static final int DON_DAT = 2;
	public static final int DON_KHONG_DAT = 3;

	private final DotXetTotNghiepRepository dotRepository;
	private final DonXetTotNghiepRepository donRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;
	private final HocKyRepository hocKyRepository;
	private final SinhVienRepository sinhVienRepository;
	private final KetQuaHocTapService ketQuaHocTapService;
	private final RenLuyenService renLuyenService;
	private final HocPhiService hocPhiService;

	// ===================== KẾT QUẢ XÉT =====================

	/** Một điều kiện tốt nghiệp, kèm số liệu thật để sinh viên biết thiếu gì */
	@Getter
	public static class DieuKien {
		private final String ten;
		private final boolean dat;
		private final String chiTiet;

		DieuKien(String ten, boolean dat, String chiTiet) {
			this.ten = ten; this.dat = dat; this.chiTiet = chiTiet;
		}
	}

	@Getter
	public static class KetQuaXet {
		private final List<DieuKien> dsDieuKien = new ArrayList<>();

		private BigDecimal gpa = BigDecimal.ZERO;
		private int tinChiTichLuy;
		private int tinChiYeuCau;
		private int diemRenLuyen;
		private int soNamDaHoc;
		private final List<String> monCongNo = new ArrayList<>();

		public boolean isDuDieuKien() {
			return !dsDieuKien.isEmpty() && dsDieuKien.stream().allMatch(DieuKien::isDat);
		}

		public long getSoDieuKienDat() {
			return dsDieuKien.stream().filter(DieuKien::isDat).count();
		}

		public int getTongDieuKien() { return dsDieuKien.size(); }
	}

	/**
	 * Xét toàn bộ điều kiện tốt nghiệp của một sinh viên.
	 *
	 * Luôn chạy hết mọi điều kiện chứ không dừng ở cái đầu tiên không đạt.
	 * Dừng sớm thì sinh viên sửa xong một chỗ lại phát hiện còn chỗ khác,
	 * phải quay lại nhiều lần mới biết hết việc phải làm.
	 */
	@Transactional(readOnly = true)
	public KetQuaXet xet(SinhVien sv) {
		KetQuaXet kq = new KetQuaXet();
		if (sv == null) return kq;

		List<KetQuaHocPhan> dsDiem = ketQuaHocTapService.tatCa(sv.getMaSinhVien());
		TongKet tongKet = ketQuaHocTapService.tinhTongKet(dsDiem);

		kq.gpa = tongKet.getGpa() == null ? BigDecimal.ZERO : tongKet.getGpa();
		kq.tinChiTichLuy = tongKet.getTinChiTichLuy();

		dieuKienTrangThai(kq, sv);
		dieuKienThoiGian(kq, sv);
		dieuKienChuongTrinh(kq, sv, dsDiem);
		dieuKienNoMon(kq, dsDiem);
		dieuKienGpa(kq);
		dieuKienRenLuyen(kq, sv, dsDiem);
		dieuKienHocPhi(kq, sv);

		return kq;
	}

	// --- 1. Trạng thái sinh viên ---

	private void dieuKienTrangThai(KetQuaXet kq, SinhVien sv) {
		boolean dat = sv.getTrangThai() != null && sv.getTrangThai() == SV_DANG_HOC;
		kq.dsDieuKien.add(new DieuKien("Trạng thái sinh viên", dat,
				dat ? "Đang học"
				    : "Hiện là \"" + sv.getTenTrangThai() + "\". Chỉ sinh viên đang học "
				      + "mới được xét tốt nghiệp."));
	}

	// --- 2. Thời gian đào tạo ---

	/**
	 * Số năm đã học tính từ ngày nhập học, không có thì tính từ khóa tuyển sinh.
	 *
	 * Quá thời gian tối đa là bị buộc thôi học chứ không phải vẫn xét tốt
	 * nghiệp, nên đây là điều kiện chặn thật chứ không phải cảnh báo.
	 */
	private void dieuKienThoiGian(KetQuaXet kq, SinhVien sv) {
		int soNam = soNamDaHoc(sv);
		kq.soNamDaHoc = soNam;

		boolean dat = soNam >= 0 && soNam <= SO_NAM_TOI_DA;
		kq.dsDieuKien.add(new DieuKien(
				"Thời gian đào tạo tối đa " + SO_NAM_TOI_DA + " năm", dat,
				soNam < 0 ? "Hồ sơ chưa có ngày nhập học và khóa tuyển sinh."
				          : "Đã học " + soNam + " năm"
				            + (dat ? "" : ", vượt quá thời gian tối đa.")));
	}

	private int soNamDaHoc(SinhVien sv) {
		LocalDate moc = sv.getNgayNhapHoc();
		if (moc == null && sv.getKhoaTuyenSinh() != null) {
			try {
				moc = LocalDate.of(Integer.parseInt(sv.getKhoaTuyenSinh().trim()), 9, 1);
			} catch (Exception boQua) {
				return -1;
			}
		}
		if (moc == null) return -1;
		return (int) java.time.temporal.ChronoUnit.YEARS.between(moc, LocalDate.now());
	}

	// --- 3. Hoàn thành chương trình đào tạo ---

	/**
	 * Hai việc khác nhau, kiểm riêng: đủ tổng tín chỉ, và đã đạt hết các học
	 * phần bắt buộc.
	 *
	 * Gộp chung là sai. Sinh viên học lại nhiều và chọn nhiều môn tự chọn có
	 * thể dư tín chỉ mà vẫn thiếu một môn bắt buộc; ngược lại đủ môn bắt
	 * buộc nhưng chưa đủ tín chỉ tự chọn cũng chưa tốt nghiệp được.
	 */
	private void dieuKienChuongTrinh(KetQuaXet kq, SinhVien sv, List<KetQuaHocPhan> dsDiem) {
		if (sv.getNganh() == null) {
			kq.dsDieuKien.add(new DieuKien("Hoàn thành chương trình đào tạo", false,
					"Hồ sơ chưa gắn ngành nên không đối chiếu được chương trình."));
			return;
		}

		List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				sv.getNganh().getMaNganh(), CTDT_DANG_AP_DUNG, Sort.by("hocKyDuKien"));

		if (khung.isEmpty()) {
			kq.dsDieuKien.add(new DieuKien("Hoàn thành chương trình đào tạo", false,
					"Ngành chưa được khai báo chương trình đào tạo. Liên hệ Phòng Đào tạo."));
			return;
		}

		int tinChiYeuCau = khung.stream()
				.mapToInt(ct -> ct.getHocPhan().getSoTinChi() == null
						? 0 : ct.getHocPhan().getSoTinChi())
				.sum();
		kq.tinChiYeuCau = tinChiYeuCau;

		// Các học phần đã đạt, lấy từ bảng tổng hợp theo học phần
		Set<Integer> daDat = new HashSet<>();
		for (TongHopHocPhan th : ketQuaHocTapService.tongHopTheoHocPhan(dsDiem)) {
			if (th.isDat()) daDat.add(th.getHocPhan().getMaHocPhanId());
		}

		List<String> thieuBatBuoc = new ArrayList<>();
		for (ChuongTrinhDaoTao ct : khung) {
			boolean batBuoc = ct.getLoaiHocPhan() != null
					&& ct.getLoaiHocPhan() == HOC_PHAN_BAT_BUOC;
			if (batBuoc && !daDat.contains(ct.getHocPhan().getMaHocPhanId())) {
				thieuBatBuoc.add(ct.getHocPhan().getMaHocPhan()
						+ " " + ct.getHocPhan().getTenHocPhan());
			}
		}

		boolean duTinChi = kq.tinChiTichLuy >= tinChiYeuCau;
		kq.dsDieuKien.add(new DieuKien("Tích lũy đủ tín chỉ", duTinChi,
				"Đã tích lũy " + kq.tinChiTichLuy + " / " + tinChiYeuCau + " tín chỉ"
				+ (duTinChi ? "" : ", còn thiếu " + (tinChiYeuCau - kq.tinChiTichLuy) + ".")));

		boolean duMonBatBuoc = thieuBatBuoc.isEmpty();
		kq.dsDieuKien.add(new DieuKien("Hoàn thành học phần bắt buộc", duMonBatBuoc,
				duMonBatBuoc ? "Đã đạt toàn bộ học phần bắt buộc của chương trình"
				             : "Còn " + thieuBatBuoc.size() + " học phần bắt buộc chưa đạt: "
				               + String.join("; ", thieuBatBuoc.subList(0,
				                       Math.min(5, thieuBatBuoc.size())))
				               + (thieuBatBuoc.size() > 5 ? "…" : "")));
	}

	// --- 4. Không nợ môn ---

	/**
	 * Nợ môn là học phần đã học nhưng lần điểm được công nhận vẫn chưa đạt.
	 *
	 * Xét trên lần điểm cao nhất chứ không phải từng lần. Sinh viên rớt rồi
	 * học lại và qua thì không còn nợ môn đó, dù dòng điểm F cũ vẫn nằm
	 * trong bảng điểm.
	 */
	private void dieuKienNoMon(KetQuaXet kq, List<KetQuaHocPhan> dsDiem) {
		for (TongHopHocPhan th : ketQuaHocTapService.tongHopTheoHocPhan(dsDiem)) {
			if (!th.isDat()) {
				kq.monCongNo.add(th.getHocPhan().getMaHocPhan()
						+ " " + th.getHocPhan().getTenHocPhan());
			}
		}

		boolean dat = kq.monCongNo.isEmpty();
		kq.dsDieuKien.add(new DieuKien("Không còn nợ môn", dat,
				dat ? "Không có học phần nào chưa đạt"
				    : "Còn nợ " + kq.monCongNo.size() + " học phần: "
				      + String.join("; ", kq.monCongNo.subList(0,
				              Math.min(5, kq.monCongNo.size())))
				      + (kq.monCongNo.size() > 5 ? "…" : "")));
	}

	// --- 5. Điểm trung bình tích lũy ---

	private void dieuKienGpa(KetQuaXet kq) {
		boolean dat = kq.gpa.compareTo(GPA_TOI_THIEU) >= 0;
		kq.dsDieuKien.add(new DieuKien(
				"Điểm trung bình tích lũy từ " + GPA_TOI_THIEU + " (thang 4)", dat,
				"Hiện đạt " + kq.gpa + " / 4.0"));
	}

	// --- 6. Điểm rèn luyện toàn khóa ---

	/**
	 * Trung bình điểm rèn luyện của các học kỳ sinh viên có học.
	 *
	 * Học kỳ nào không tham gia hoạt động nào vẫn tính điểm nền, không bỏ
	 * qua. Bỏ qua thì sinh viên chỉ tham gia đúng một học kỳ sẽ có điểm toàn
	 * khóa bằng đúng học kỳ đó, cao hơn người đều đặn cả khóa.
	 */
	private void dieuKienRenLuyen(KetQuaXet kq, SinhVien sv, List<KetQuaHocPhan> dsDiem) {
		List<HocKy> dsHocKyDaHoc = ketQuaHocTapService.dsHocKy(dsDiem);

		Map<Integer, Integer> diemTheoHocKy = new HashMap<>();
		for (KetQuaRenLuyen r : renLuyenService.ketQuaMoiHocKy(sv)) {
			diemTheoHocKy.put(r.getHocKy().getMaHocKy(), r.getTongDiem());
		}

		int diem;
		if (dsHocKyDaHoc.isEmpty()) {
			diem = RenLuyenService.DIEM_NEN;
		} else {
			int tong = 0;
			for (HocKy hk : dsHocKyDaHoc) {
				tong += diemTheoHocKy.getOrDefault(hk.getMaHocKy(), RenLuyenService.DIEM_NEN);
			}
			diem = tong / dsHocKyDaHoc.size();
		}
		kq.diemRenLuyen = diem;

		boolean dat = diem >= DIEM_REN_LUYEN_TOI_THIEU;
		kq.dsDieuKien.add(new DieuKien(
				"Điểm rèn luyện toàn khóa từ " + DIEM_REN_LUYEN_TOI_THIEU, dat,
				"Trung bình " + diem + " điểm qua " + dsHocKyDaHoc.size() + " học kỳ"
				+ (dat ? "" : ". Tham gia thêm hoạt động công tác xã hội để nâng điểm.")));
	}

	// --- 7. Học phí ---

	private void dieuKienHocPhi(KetQuaXet kq, SinhVien sv) {
		java.math.BigDecimal conNo = java.math.BigDecimal.ZERO;
		for (CongNoHocPhi cn : hocPhiService.dsCongNo(sv)) {
			if (cn.getTrangThai() != null && cn.getTrangThai() == CONG_NO_KHONG_PHAT_SINH) continue;
			conNo = conNo.add(cn.getSoTienConLai());
		}

		boolean dat = conNo.signum() <= 0;
		kq.dsDieuKien.add(new DieuKien("Không còn nợ học phí", dat,
				dat ? "Đã đóng đủ học phí"
				    : "Còn nợ " + String.format("%,d", conNo.longValue()) + " đ"));
	}

	// ===================== ĐỢT XÉT =====================

	@Transactional(readOnly = true)
	public List<DotXetTotNghiep> dsDot() {
		return dotRepository.findAllBy(Sort.by(Sort.Direction.DESC, "maDot"));
	}

	@Transactional(readOnly = true)
	public DotXetTotNghiep timDot(Integer maDot) {
		return dotRepository.findWithHocKyByMaDot(maDot)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy đợt xét tốt nghiệp."));
	}

	@Transactional(readOnly = true)
	public List<DonXetTotNghiep> dsDon(Integer maDot) {
		return donRepository.dsTheoDot(maDot);
	}

	@Transactional
	public DotXetTotNghiep taoDot(Integer maHocKy, String tenDot,
	                              LocalDateTime moDangKy, LocalDateTime dongDangKy,
	                              LocalDate ngayXet, String nguoiTao) {
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));

		if (tenDot == null || tenDot.isBlank())
			throw new IllegalStateException("Chưa nhập tên đợt xét.");

		if (moDangKy != null && dongDangKy != null && !dongDangKy.isAfter(moDangKy))
			throw new IllegalStateException("Ngày đóng nhận đơn phải sau ngày mở.");

		DotXetTotNghiep dot = new DotXetTotNghiep();
		dot.setHocKy(hk);
		dot.setTenDot(tenDot.trim());
		dot.setNgayMoDangKy(moDangKy);
		dot.setNgayDongDangKy(dongDangKy);
		dot.setNgayXet(ngayXet);
		dot.setTrangThai(DOT_NHAP);
		dot.setNguoiTao(nguoiTao);
		return dotRepository.saveAndFlush(dot);
	}

	@Transactional
	public void doiTrangThaiDot(Integer maDot, int trangThaiMoi) {
		DotXetTotNghiep dot = timDot(maDot);
		int hienTai = dot.getTrangThai();

		if (hienTai == DOT_CONG_BO)
			throw new IllegalStateException(
					"Đợt đã công bố kết quả, không đổi trạng thái nữa.");

		boolean hopLe = switch (trangThaiMoi) {
			case DOT_NHAN_DON -> hienTai == DOT_NHAP || hienTai == DOT_DA_CHOT;
			case DOT_DA_CHOT  -> hienTai == DOT_NHAN_DON;
			// Chỉ công bố khi không còn đơn nào chờ xét
			case DOT_CONG_BO  -> hienTai == DOT_DA_CHOT;
			default -> false;
		};
		if (!hopLe)
			throw new IllegalStateException("Không chuyển từ \"" + dot.getTenTrangThai()
					+ "\" sang trạng thái đó được.");

		if (trangThaiMoi == DOT_CONG_BO) {
			long choXet = donRepository.countByDot_MaDotAndTrangThai(maDot, DON_CHO_XET);
			if (choXet > 0)
				throw new IllegalStateException("Còn " + choXet + " đơn chưa xét. "
						+ "Xét hết rồi mới công bố kết quả được.");
		}

		dot.setTrangThai(trangThaiMoi);
		dotRepository.saveAndFlush(dot);
	}

	// ===================== SINH VIÊN NỘP ĐƠN =====================

	/** Các đợt sinh viên còn nộp đơn được */
	@Transactional(readOnly = true)
	public List<DotXetTotNghiep> dsDotDangNhanDon() {
		return dotRepository.findByTrangThai(DOT_NHAN_DON, Sort.by(Sort.Direction.DESC, "maDot"))
				.stream().filter(DotXetTotNghiep::isDangNhanDon).toList();
	}

	@Transactional(readOnly = true)
	public List<DonXetTotNghiep> donCuaSinhVien(SinhVien sv) {
		if (sv == null) return List.of();
		return donRepository.dsTheoSinhVien(sv.getMaSinhVien());
	}

	/**
	 * Sinh viên nộp đơn xét tốt nghiệp.
	 *
	 * Chặn ngay ở đây nếu chưa đủ điều kiện, thay vì nhận đơn rồi để hội
	 * đồng từ chối. Điều kiện tính được bằng dữ liệu nên không có lý do bắt
	 * người ta chờ một vòng chỉ để nhận lại câu trả lời mà hệ thống đã biết.
	 */
	@Transactional
	public DonXetTotNghiep nopDon(SinhVien sv, Integer maDot) {
		DotXetTotNghiep dot = timDot(maDot);

		if (!dot.isDangNhanDon())
			throw new IllegalStateException(dot.isHetHanNopDon()
					? "Đã quá hạn nộp đơn của đợt này."
					: "Đợt này hiện không nhận đơn.");

		DonXetTotNghiep cu = donRepository
				.findByDot_MaDotAndSinhVien_MaSinhVien(maDot, sv.getMaSinhVien())
				.orElse(null);
		if (cu != null)
			throw new IllegalStateException("Bạn đã nộp đơn cho đợt này rồi.");

		KetQuaXet kq = xet(sv);
		if (!kq.isDuDieuKien())
			throw new IllegalStateException(
					"Bạn chưa đủ điều kiện tốt nghiệp. Xem bảng điều kiện phía trên "
					+ "để biết còn thiếu gì.");

		DonXetTotNghiep don = new DonXetTotNghiep();
		don.setDot(dot);
		don.setSinhVien(sv);
		don.setNgayNop(LocalDateTime.now());
		don.setGpaLucNop(kq.getGpa());
		don.setTinChiLucNop(kq.getTinChiTichLuy());
		don.setTinChiYeuCau(kq.getTinChiYeuCau());
		don.setDiemRenLuyenLucNop(kq.getDiemRenLuyen());
		don.setSoNamDaHoc(kq.getSoNamDaHoc());
		don.setTrangThai(DON_CHO_XET);
		return donRepository.saveAndFlush(don);
	}

	// ===================== HỘI ĐỒNG XÉT =====================

	/**
	 * Công nhận tốt nghiệp: đổi luôn trạng thái sinh viên.
	 *
	 * Xét lại điều kiện một lần nữa ngay trước khi duyệt. Từ lúc nộp đơn tới
	 * lúc hội đồng họp có thể cách nhau vài tuần, trong khoảng đó điểm có thể
	 * bị sửa hoặc phát sinh công nợ mới.
	 */
	@Transactional
	public void duyet(Integer maDon, String nguoiXet) {
		DonXetTotNghiep don = donRepository.findById(maDon)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn."));

		if (!don.isChoXet())
			throw new IllegalStateException("Đơn này đã được xét rồi.");

		SinhVien sv = sinhVienRepository.timTheoMa(don.getSinhVien().getMaSinhVien())
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy sinh viên."));

		KetQuaXet kq = xet(sv);
		if (!kq.isDuDieuKien())
			throw new IllegalStateException(
					"Sinh viên không còn đủ điều kiện tại thời điểm xét. "
					+ "Kiểm tra lại bảng điều kiện trước khi công nhận.");

		don.setTrangThai(DON_DAT);
		don.setNguoiXet(nguoiXet);
		don.setNgayXet(LocalDateTime.now());
		donRepository.saveAndFlush(don);

		sv.setTrangThai(SV_TOT_NGHIEP);
		sv.setNgayTotNghiep(LocalDate.now());
		sv.setNgayThayDoiTrangThai(LocalDate.now());
		sv.setLyDoThayDoiTrangThai("Công nhận tốt nghiệp đợt " + don.getDot().getTenDot());
		sinhVienRepository.saveAndFlush(sv);
	}

	@Transactional
	public void tuChoi(Integer maDon, String lyDo, String nguoiXet) {
		DonXetTotNghiep don = donRepository.findById(maDon)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn."));

		if (!don.isChoXet())
			throw new IllegalStateException("Đơn này đã được xét rồi.");

		if (lyDo == null || lyDo.isBlank())
			throw new IllegalStateException("Phải ghi lý do để sinh viên biết cần làm gì tiếp.");

		don.setTrangThai(DON_KHONG_DAT);
		don.setLyDoTuChoi(lyDo.trim());
		don.setNguoiXet(nguoiXet);
		don.setNgayXet(LocalDateTime.now());
		donRepository.saveAndFlush(don);
	}

	/** Xét lại điều kiện của một đơn, để hội đồng xem số liệu hiện tại */
	@Transactional(readOnly = true)
	public Map<Integer, KetQuaXet> xetCacDon(List<DonXetTotNghiep> ds) {
		Map<Integer, KetQuaXet> kq = new HashMap<>();
		for (DonXetTotNghiep don : ds) {
			kq.put(don.getMaDon(), xet(don.getSinhVien()));
		}
		return kq;
	}
}
