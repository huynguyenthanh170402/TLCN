package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.*;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.*;

/**
 * Dựng sáu khóa sinh viên từ 2020 đến 2025 kèm toàn bộ quá trình học.
 *
 * Trước seeder này, cơ sở dữ liệu chỉ có đúng một sinh viên có đăng ký và
 * điểm, nên mọi biểu đồ kết quả học tập đều trống. Những sinh viên nhập từ
 * Excel có hồ sơ nhưng chưa học gì.
 *
 * Mỗi khóa 20 sinh viên, trạng thái phân bố giống thực tế: phần lớn tốt
 * nghiệp đúng hạn, một số còn nợ môn nên vẫn đang học, vài trường hợp bảo
 * lưu, tạm nghỉ và thôi học giữa chừng.
 *
 * Chạy sau HocTapSeeder vì cần sẵn danh mục học phần và các học kỳ.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class KhoaHocSeeder implements CommandLineRunner {

	/** Sinh viên đầu tiên seeder tạo ra. Có rồi thì bỏ qua toàn bộ. */
	private static final String MOC_NHAN_DIEN = "20110901";

	private static final int KHOA_DAU = 2020;
	private static final int KHOA_CUOI = 2025;
	private static final int SO_SV_MOI_KHOA = 20;

	/** Đơn giá dự phòng khi bảng DonGiaTinChi chưa khai báo gì */
	private static final BigDecimal DON_GIA_DU_PHONG = new BigDecimal("850000");
	private static final BigDecimal MOT_TRAM = new BigDecimal("100");

	// Trạng thái sinh viên
	private static final int SV_THOI_HOC = 0, SV_DANG_HOC = 1,
	                         SV_BAO_LUU = 2, SV_TAM_NGHI = 3, SV_TOT_NGHIEP = 5;

	// Trạng thái học kỳ
	private static final int HK_DA_KET_THUC = 2;

	/**
	 * Khung chương trình bốn năm, tám học kỳ.
	 * Dùng đúng mã học phần mà HocTapSeeder đã tạo.
	 */
	private static final String[][] KHUNG_CHUONG_TRINH = {
		{ "INPR130285", "MATH132401", "ENGL130137" },   // năm 1 kỳ 1
		{ "PRTE230385", "MATH132501", "ENGL230237" },   // năm 1 kỳ 2
		{ "DASA230179", "DIGR240485", "CAAL230180" },   // năm 2 kỳ 1
		{ "OOPR230279", "DBSY230184", "OPSY330280" },   // năm 2 kỳ 2
		{ "NEES330380", "WEPR330479", "DBMS330284" },   // năm 3 kỳ 1
		{ "SOEN330679", "ARIN330585", "MOPR331279" },   // năm 3 kỳ 2
		{ "ITPM430884", "MALE431085", "INSE330380" },   // năm 4 kỳ 1
		{ "CLCO432779" }                                 // năm 4 kỳ 2
	};

	private static final String[] HO = {
		"Nguyễn", "Trần", "Lê", "Phạm", "Hoàng", "Huỳnh", "Phan",
		"Vũ", "Võ", "Đặng", "Bùi", "Đỗ", "Hồ", "Ngô", "Dương", "Lý"
	};
	private static final String[] DEM_NAM = { "Văn", "Hữu", "Minh", "Quốc", "Thanh", "Đức", "Công", "Tấn" };
	private static final String[] DEM_NU  = { "Thị", "Ngọc", "Thanh", "Kim", "Minh", "Thu", "Hoài", "Mỹ" };
	private static final String[] TEN_NAM = {
		"An", "Bình", "Cường", "Dũng", "Đạt", "Hiếu", "Huy", "Khang",
		"Long", "Nam", "Phúc", "Quân", "Sơn", "Thành", "Tuấn", "Vinh"
	};
	private static final String[] TEN_NU = {
		"Anh", "Chi", "Dung", "Giang", "Hà", "Hương", "Lan", "Linh",
		"Mai", "Ngân", "Nhi", "Oanh", "Phương", "Quỳnh", "Thảo", "Trang"
	};

	private final QuyenRepository quyenRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final NganhRepository nganhRepository;
	private final SinhVienRepository sinhVienRepository;
	private final HocKyRepository hocKyRepository;
	private final HocPhanRepository hocPhanRepository;
	private final LopHocPhanRepository lopHocPhanRepository;
	private final DangKyHocPhanRepository dangKyHocPhanRepository;
	private final KetQuaHocPhanRepository ketQuaHocPhanRepository;
	private final CongNoHocPhiRepository congNoHocPhiRepository;
	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongGiangDayRepository;
	private final MienGiamHocPhiRepository mienGiamHocPhiRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {
		if (sinhVienRepository.existsByMaSoSinhVien(MOC_NHAN_DIEN)) {
			log.info("Đã có dữ liệu sáu khóa mẫu, bỏ qua KhoaHocSeeder.");
			return;
		}

		Quyen quyenSinhVien = quyenRepository.findById(1).orElse(null);
		if (quyenSinhVien == null) {
			log.warn("Chưa có quyền Sinh viên (MaQuyen = 1), bỏ qua KhoaHocSeeder.");
			return;
		}

		List<Nganh> dsNganh = nganhRepository.findAll();
		if (dsNganh.isEmpty()) {
			log.warn("Chưa có ngành nào, bỏ qua KhoaHocSeeder.");
			return;
		}

		boSungHocKyCu();

		List<GiangVien> dsGiangVien = giangVienRepository.findAll();
		int demSinhVien = 0;

		for (int khoa = KHOA_DAU; khoa <= KHOA_CUOI; khoa++) {
			demSinhVien += taoMotKhoa(khoa, quyenSinhVien, dsNganh, dsGiangVien);
		}

		log.info("Đã tạo {} sinh viên cho các khóa {}–{}, kèm đăng ký, điểm và công nợ học phí.",
				demSinhVien, KHOA_DAU, KHOA_CUOI);
	}

	// ===================== HỌC KỲ CÁC NĂM TRƯỚC =====================

	/**
	 * HocTapSeeder chỉ tạo học kỳ từ năm học 2022-2023 trở đi, mà khối này
	 * cần ngược về 2020. Chỉ thêm những học kỳ còn thiếu, không đụng cái đã có.
	 */
	private void boSungHocKyCu() {
		for (int nam = KHOA_DAU; nam < 2022; nam++) {
			String namHoc = nam + "-" + (nam + 1);
			taoHocKyNeuThieu("HK1-" + namHoc, 1, namHoc,
					LocalDate.of(nam, 9, 5), LocalDate.of(nam + 1, 1, 15));
			taoHocKyNeuThieu("HK2-" + namHoc, 2, namHoc,
					LocalDate.of(nam + 1, 2, 10), LocalDate.of(nam + 1, 6, 25));
		}
	}

	private void taoHocKyNeuThieu(String code, int so, String namHoc,
	                              LocalDate batDau, LocalDate ketThuc) {
		if (hocKyRepository.findByMaHocKyCode(code).isPresent()) return;

		HocKy hk = new HocKy();
		hk.setMaHocKyCode(code);
		hk.setTenHocKy("Học kỳ " + so + " năm học " + namHoc);
		hk.setNamHoc(namHoc);
		hk.setHocKySo(so);
		hk.setNgayBatDau(batDau);
		hk.setNgayKetThuc(ketThuc);
		hk.setTrangThai(HK_DA_KET_THUC);
		hocKyRepository.save(hk);
	}

	// ===================== MỘT KHÓA =====================

	private int taoMotKhoa(int khoa, Quyen quyenSinhVien,
	                       List<Nganh> dsNganh, List<GiangVien> dsGiangVien) {

		// Các học kỳ của khóa này đã kết thúc, theo đúng thứ tự năm học
		List<HocKy> dsHocKy = hocKyCuaKhoa(khoa);
		if (dsHocKy.isEmpty()) {
			log.warn("Khóa {} chưa có học kỳ nào kết thúc, bỏ qua.", khoa);
			return 0;
		}

		boolean daXongChuongTrinh = dsHocKy.size() >= KHUNG_CHUONG_TRINH.length;

		// Mở lớp học phần riêng cho khóa này ở từng học kỳ
		List<List<LopHocPhan>> lopTheoKy = moLopChoKhoa(khoa, dsHocKy, dsGiangVien);

		int dem = 0;
		for (int i = 0; i < SO_SV_MOI_KHOA; i++) {
			SinhVien sv = taoSinhVien(khoa, i, quyenSinhVien, dsNganh, daXongChuongTrinh, dsHocKy);
			hocTap(sv, i, dsHocKy, lopTheoKy, daXongChuongTrinh);
			dem++;
		}
		return dem;
	}

	/** Tám học kỳ kể từ năm nhập học, chỉ lấy những kỳ đã kết thúc */
	private List<HocKy> hocKyCuaKhoa(int khoa) {
		List<HocKy> ds = new ArrayList<>();
		for (int ky = 0; ky < KHUNG_CHUONG_TRINH.length; ky++) {
			int namBatDau = khoa + ky / 2;
			String namHoc = namBatDau + "-" + (namBatDau + 1);
			String code = (ky % 2 == 0 ? "HK1-" : "HK2-") + namHoc;

			HocKy hk = hocKyRepository.findByMaHocKyCode(code).orElse(null);
			// Kỳ chưa kết thúc thì dừng: sinh viên chưa thể có điểm công bố
			if (hk == null || hk.getTrangThai() != HK_DA_KET_THUC) break;
			ds.add(hk);
		}
		return ds;
	}

	// ===================== LỚP HỌC PHẦN =====================

	/**
	 * Mỗi khóa học riêng một lớp cho cùng một học phần, giống thực tế.
	 * Mã lớp có thêm hậu tố khóa nên không đụng các lớp HocTapSeeder đã tạo.
	 */
	private List<List<LopHocPhan>> moLopChoKhoa(int khoa, List<HocKy> dsHocKy,
	                                            List<GiangVien> dsGiangVien) {
		List<List<LopHocPhan>> ketQua = new ArrayList<>();
		int soGv = dsGiangVien.size();
		int luot = 0;

		for (int ky = 0; ky < dsHocKy.size(); ky++) {
			HocKy hk = dsHocKy.get(ky);
			List<LopHocPhan> cuaKy = new ArrayList<>();

			for (String maHp : KHUNG_CHUONG_TRINH[ky]) {
				HocPhan hp = hocPhanRepository.findByMaHocPhan(maHp).orElse(null);
				if (hp == null) continue;

				String maLop = maHp + "_" + hk.getMaHocKyCode() + "_K" + khoa;
				LopHocPhan lop = lopHocPhanRepository.findByMaLop(maLop).orElse(null);

				if (lop == null) {
					lop = new LopHocPhan();
					lop.setMaLop(maLop);
					lop.setHocPhan(hp);
					lop.setHocKy(hk);
					lop.setSiSoToiDa(SO_SV_MOI_KHOA + 10);
					lop.setSiSoToiThieu(15);
					lop.setLoaiLop(1);
					lop.setKhoaApDung(String.valueOf(khoa));
					lop.setTrangThai(3);   // đã kết thúc
					lop = lopHocPhanRepository.save(lop);

					// Phân công một giảng viên chính, chia đều theo vòng
					if (soGv > 0) {
						PhanCongGiangDay pc = new PhanCongGiangDay();
						pc.setLopHocPhan(lop);
						pc.setGiangVien(dsGiangVien.get(luot % soGv));
						pc.setVaiTro(1);
						pc.setTrangThai(1);
						phanCongGiangDayRepository.save(pc);
						luot++;
					}
				}
				cuaKy.add(lop);
			}
			ketQua.add(cuaKy);
		}
		return ketQua;
	}

	// ===================== SINH VIÊN =====================

	private SinhVien taoSinhVien(int khoa, int thuTu, Quyen quyenSinhVien,
	                             List<Nganh> dsNganh, boolean daXongChuongTrinh,
	                             List<HocKy> dsHocKy) {

		String mssv = String.format("%02d1109%02d", khoa % 100, thuTu + 1);

		// Bộ sinh ngẫu nhiên gieo theo mã số: chạy lại bao nhiêu lần cũng ra
		// cùng một người, cùng bộ điểm. Dữ liệu mẫu mà mỗi lần một khác thì
		// không đối chiếu được khi làm báo cáo.
		Random rd = new Random(mssv.hashCode());

		boolean nam = rd.nextBoolean();
		String hoTen = HO[rd.nextInt(HO.length)] + " "
				+ (nam ? DEM_NAM[rd.nextInt(DEM_NAM.length)] : DEM_NU[rd.nextInt(DEM_NU.length)]) + " "
				+ (nam ? TEN_NAM[rd.nextInt(TEN_NAM.length)] : TEN_NU[rd.nextInt(TEN_NU.length)]);

		TaiKhoan tk = new TaiKhoan();
		tk.setTenDangNhap(mssv);
		tk.setMatKhauBam(passwordEncoder.encode("123456"));
		tk.setQuyen(quyenSinhVien);
		tk.setTrangThai(1);
		tk = taiKhoanRepository.save(tk);

		SinhVien sv = new SinhVien();
		sv.setMaSoSinhVien(mssv);
		sv.setTaiKhoan(tk);
		sv.setNganh(dsNganh.get((khoa * 7 + thuTu) % dsNganh.size()));
		sv.setKhoaTuyenSinh(String.valueOf(khoa));
		sv.setHoTen(hoTen);
		sv.setGioiTinh(nam ? 1 : 2);
		sv.setNgaySinh(LocalDate.of(khoa - 18, 1 + rd.nextInt(12), 1 + rd.nextInt(28)));
		sv.setQuocTich("Việt Nam");
		sv.setDanToc("Kinh");
		sv.setEmailTruong(mssv + "@student.hcmute.edu.vn");
		sv.setEmailCaNhan(mssv + "@gmail.com");
		sv.setSoDienThoai("09" + (10000000 + rd.nextInt(89999999)));

		// Ngày nhập học bám theo khóa, không dùng ngày hôm nay cho mọi người
		sv.setNgayNhapHoc(LocalDate.of(khoa, 9, 5));
		sv.setNgayTotNghiepDuKien(LocalDate.of(khoa + 4, 6, 30));
		sv.setTrangThai(trangThaiTheoThuTu(thuTu, daXongChuongTrinh));

		if (sv.getTrangThai() == SV_TOT_NGHIEP) {
			HocKy kyCuoi = dsHocKy.get(dsHocKy.size() - 1);
			sv.setNgayTotNghiep(kyCuoi.getNgayKetThuc());
		}
		if (sv.getTrangThai() == SV_THOI_HOC) {
			sv.setNgayThayDoiTrangThai(LocalDate.of(khoa + 1, 6, 30));
			sv.setLyDoThayDoiTrangThai("Tự ý bỏ học, không đăng ký học phần hai học kỳ liên tiếp.");
		}

		return sinhVienRepository.save(sv);
	}

	/**
	 * Phân bố trạng thái cố định theo thứ tự trong khóa.
	 *
	 * Dùng thứ tự thay vì ngẫu nhiên để mỗi khóa đều có đủ các trường hợp
	 * cần minh họa, không rơi vào cảnh chạy ra hai mươi người cùng tốt nghiệp.
	 */
	private int trangThaiTheoThuTu(int thuTu, boolean daXongChuongTrinh) {
		if (daXongChuongTrinh) {
			if (thuTu < 14) return SV_TOT_NGHIEP;   // 14 người ra trường đúng hạn
			if (thuTu < 17) return SV_DANG_HOC;     // 3 người còn nợ môn, học tiếp
			if (thuTu < 19) return SV_THOI_HOC;     // 2 người bỏ giữa chừng
			return SV_BAO_LUU;                      // 1 người bảo lưu
		}
		if (thuTu < 17) return SV_DANG_HOC;
		if (thuTu == 17) return SV_BAO_LUU;
		if (thuTu == 18) return SV_TAM_NGHI;
		return SV_THOI_HOC;
	}

	// ===================== QUÁ TRÌNH HỌC =====================

	private void hocTap(SinhVien sv, int thuTu, List<HocKy> dsHocKy,
	                    List<List<LopHocPhan>> lopTheoKy, boolean daXongChuongTrinh) {

		Random rd = new Random(sv.getMaSoSinhVien().hashCode() * 31L);
		int trangThai = sv.getTrangThai();

		// Người bỏ học hay nghỉ giữa chừng thì dừng đăng ký từ một kỳ nào đó
		int soKyHoc = dsHocKy.size();
		if (trangThai == SV_THOI_HOC)  soKyHoc = Math.max(1, dsHocKy.size() / 3);
		if (trangThai == SV_BAO_LUU)   soKyHoc = Math.max(1, dsHocKy.size() * 2 / 3);
		if (trangThai == SV_TAM_NGHI)  soKyHoc = Math.max(1, dsHocKy.size() - 1);

		// Người còn nợ môn: ép rớt vài học phần để dữ liệu có trường hợp thật
		boolean noMon = daXongChuongTrinh && trangThai == SV_DANG_HOC;
		int kyRot1 = noMon ? 2 + rd.nextInt(3) : -1;
		int kyRot2 = noMon ? 4 + rd.nextInt(3) : -1;

		for (int ky = 0; ky < soKyHoc; ky++) {
			HocKy hk = dsHocKy.get(ky);
			List<LopHocPhan> dsLop = lopTheoKy.get(ky);
			int tongTinChi = 0;

			for (int i = 0; i < dsLop.size(); i++) {
				LopHocPhan lop = dsLop.get(i);
				tongTinChi += lop.getHocPhan().getSoTinChi();

				DangKyHocPhan dk = new DangKyHocPhan();
				dk.setSinhVien(sv);
				dk.setLopHocPhan(lop);
				dk.setLoaiDangKy(1);
				dk.setTrangThai(1);
				dk.setNgayDangKy(hk.getNgayBatDau().minusDays(14).atStartOfDay());
				dk = dangKyHocPhanRepository.save(dk);

				// Môn đầu tiên của kỳ bị ép rớt nếu kỳ đó nằm trong diện nợ môn
				boolean epRot = (ky == kyRot1 || ky == kyRot2) && i == 0;
				luuDiem(dk, hk, rd, epRot);
			}

			taoCongNo(sv, hk, tongTinChi, rd);
		}
	}

	private void luuDiem(DangKyHocPhan dk, HocKy hk, Random rd, boolean epRot) {
		BigDecimal qt, gk, ck;

		if (epRot) {
			// Điểm cuối kỳ thấp kéo tổng kết xuống dưới 4,0
			qt = diem(rd, 4.0, 7.0);
			gk = diem(rd, 3.0, 6.0);
			ck = diem(rd, 1.0, 3.5);
		} else {
			qt = diem(rd, 6.0, 10.0);
			gk = diem(rd, 5.0, 10.0);
			// Vẫn chừa khoảng rớt tự nhiên, không phải ai cũng qua hết
			ck = diem(rd, 3.5, 10.0);
		}

		BigDecimal tk = qt.multiply(new BigDecimal("0.2"))
				.add(gk.multiply(new BigDecimal("0.3")))
				.add(ck.multiply(new BigDecimal("0.5")))
				.setScale(1, RoundingMode.HALF_UP);

		KetQuaHocPhan kq = new KetQuaHocPhan();
		kq.setDangKyHocPhan(dk);
		kq.setDiemQuaTrinh(qt);
		kq.setDiemGiuaKy(gk);
		kq.setDiemCuoiKy(ck);
		kq.setDiemTongKet(tk);
		quyDoi(kq, tk.doubleValue());
		kq.setTrangThai(3);   // đã công bố
		kq.setNgayNhap(hk.getNgayKetThuc().atStartOfDay());
		ketQuaHocPhanRepository.save(kq);
	}

	/** Quy đổi thang 10 sang điểm chữ, thang 4 và kết quả đạt hay không */
	private void quyDoi(KetQuaHocPhan kq, double tk) {
		String chu; double he4;
		if      (tk >= 8.5) { chu = "A";  he4 = 4.0; }
		else if (tk >= 8.0) { chu = "B+"; he4 = 3.5; }
		else if (tk >= 7.0) { chu = "B";  he4 = 3.0; }
		else if (tk >= 6.5) { chu = "C+"; he4 = 2.5; }
		else if (tk >= 5.5) { chu = "C";  he4 = 2.0; }
		else if (tk >= 5.0) { chu = "D+"; he4 = 1.5; }
		else if (tk >= 4.0) { chu = "D";  he4 = 1.0; }
		else                { chu = "F";  he4 = 0.0; }

		kq.setDiemChu(chu);
		kq.setDiemHe4(BigDecimal.valueOf(he4).setScale(2));
		kq.setKetQua(tk >= 4.0 ? 1 : 0);
	}

	/** Điểm ngẫu nhiên trong khoảng, làm tròn 0,5 */
	private BigDecimal diem(Random rd, double min, double max) {
		double d = min + rd.nextDouble() * (max - min);
		d = Math.round(d * 2) / 2.0;
		return BigDecimal.valueOf(d).setScale(1, RoundingMode.HALF_UP);
	}

	// ===================== HỌC PHÍ =====================

	/**
	 * Mỗi học kỳ một dòng công nợ, tính bằng số tín chỉ nhân đơn giá.
	 *
	 * Các kỳ cũ coi như đã thu đủ. Hai năm gần đây mới chừa lại phần chưa
	 * thu, để biểu đồ của Phòng Kế hoạch Tài chính và Ban Giám hiệu có
	 * khoảng cách giữa cột phải thu và cột đã thu.
	 */
	private void taoCongNo(SinhVien sv, HocKy hk, int tongTinChi, Random rd) {
		if (tongTinChi == 0) return;

		BigDecimal phaiDong = DON_GIA_DU_PHONG.multiply(BigDecimal.valueOf(tongTinChi));

		// Vài sinh viên diện chính sách được giảm, số còn lại đóng đủ
		BigDecimal tiLeGiam = BigDecimal.ZERO;
		if (sv.getMaSoSinhVien().endsWith("03")) tiLeGiam = new BigDecimal("50");
		if (sv.getMaSoSinhVien().endsWith("07")) tiLeGiam = new BigDecimal("70");

		BigDecimal mienGiam = phaiDong.multiply(tiLeGiam)
				.divide(MOT_TRAM, 0, RoundingMode.HALF_UP);
		BigDecimal conPhaiDong = phaiDong.subtract(mienGiam);

		// Kỳ kết thúc trước 2025 thì đã thu xong, gần đây thì còn dở dang
		BigDecimal daDong;
		if (hk.getNgayKetThuc().getYear() < 2025) {
			daDong = conPhaiDong;
		} else {
			int boc = rd.nextInt(10);
			if (boc < 6)      daDong = conPhaiDong;                                   // đóng đủ
			else if (boc < 9) daDong = conPhaiDong.divide(new BigDecimal("2"), 0, RoundingMode.HALF_UP);
			else              daDong = BigDecimal.ZERO;                               // chưa đóng
		}

		CongNoHocPhi cn = new CongNoHocPhi();
		cn.setSinhVien(sv);
		cn.setHocKy(hk);
		cn.setNoiDung("Học phí " + hk.getTenHocKy() + " — " + tongTinChi + " tín chỉ");
		cn.setSoTienPhaiDong(phaiDong);
		cn.setSoTienMienGiam(mienGiam);
		cn.setSoTienDaDong(daDong);
		cn.setHanThanhToan(hk.getNgayBatDau().plusDays(30));
		cn.setNgayTao(hk.getNgayBatDau().atStartOfDay());

		if (daDong.compareTo(conPhaiDong) >= 0)   cn.setTrangThai(2);   // đã đóng đủ
		else if (daDong.signum() > 0)             cn.setTrangThai(1);   // đóng một phần
		else                                      cn.setTrangThai(0);   // chưa đóng

		congNoHocPhiRepository.save(cn);

		// Có miễn giảm thì phải có quyết định đứng sau, nếu không Phòng Kế
		// hoạch Tài chính thấy số tiền giảm mà không tra được căn cứ
		if (tiLeGiam.signum() > 0) {
			MienGiamHocPhi mg = new MienGiamHocPhi();
			mg.setSinhVien(sv);
			mg.setHocKy(hk);
			mg.setLoai(tiLeGiam.intValue() >= 70 ? 1 : 2);
			mg.setDienHuong(tiLeGiam.intValue() >= 70
					? "Con thương binh hạng 1/4"
					: "Học bổng khuyến khích học tập loại Giỏi");
			mg.setTyLePhanTram(tiLeGiam);
			mg.setTrangThai(1);   // đã duyệt
			mg.setNguoiDeNghi("phongctsv");
			mg.setNgayDeNghi(hk.getNgayBatDau().atStartOfDay());
			mg.setNguoiDuyet("phongctsv");
			mg.setNgayDuyet(hk.getNgayBatDau().plusDays(7).atStartOfDay());
			mienGiamHocPhiRepository.save(mg);
		}
	}
}
