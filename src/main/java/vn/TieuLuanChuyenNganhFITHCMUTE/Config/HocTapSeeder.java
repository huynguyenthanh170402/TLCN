package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.*;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.*;

/**
 * Seed dữ liệu học tập mẫu: học kỳ, học phần, lớp học phần,
 * đăng ký và điểm của sinh viên 22110123, và các lớp mở đăng ký
 * cho học kỳ hiện tại.
 * Chạy sau DataSeeder (cần có Khoa CNTT và sinh viên mẫu).
 */
@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class HocTapSeeder implements CommandLineRunner {

	private final HocKyRepository hocKyRepository;
	private final HocPhanRepository hocPhanRepository;
	private final LopHocPhanRepository lopHocPhanRepository;
	private final DangKyHocPhanRepository dangKyHocPhanRepository;
	private final KetQuaHocPhanRepository ketQuaHocPhanRepository;
	private final DonViRepository donViRepository;
	private final SinhVienRepository sinhVienRepository;

	/** Học kỳ → danh sách mã học phần sinh viên mẫu đã học */
	private static final Map<String, List<String>> KE_HOACH = Map.of(
		"HK1-2022-2023", List.of("INPR130285", "MATH132401", "ENGL130137"),
		"HK2-2022-2023", List.of("PRTE230385", "MATH132501", "ENGL230237"),
		"HK1-2023-2024", List.of("DASA230179", "DIGR240485", "CAAL230180"),
		"HK2-2023-2024", List.of("OOPR230279", "DBSY230184", "OPSY330280"),
		"HK1-2024-2025", List.of("NEES330380", "WEPR330479", "DBMS330284"),
		"HK2-2024-2025", List.of("SOEN330679", "ARIN330585", "MOPR331279"),
		"HK1-2025-2026", List.of("ITPM430884", "MALE431085"),
		"HK2-2025-2026", List.of("INSE330380", "CLCO432779")
	);

	/**
	 * @Transactional giữ phiên Hibernate mở suốt quá trình seed,
	 * nhờ vậy các quan hệ LAZY nạp được khi cần.
	 */
	@Override
	@Transactional
	public void run(String... args) {
		seedHocKy();
		seedHocPhan();
		seedLopHocPhan();
		seedDangKyVaKetQua();
		seedLopMoDangKy();
	}

	// ===================== HỌC KỲ =====================
	private void seedHocKy() {
		if (hocKyRepository.count() > 0) return;

		for (int nam = 2022; nam <= 2025; nam++) {
			String namHoc = nam + "-" + (nam + 1);
			taoHocKy("HK1-" + namHoc, 1, namHoc,
					LocalDate.of(nam, 9, 5), LocalDate.of(nam + 1, 1, 15), 2);
			taoHocKy("HK2-" + namHoc, 2, namHoc,
					LocalDate.of(nam + 1, 2, 10), LocalDate.of(nam + 1, 6, 25), 2);
		}

		// Học kỳ hiện tại: đang diễn ra, đang mở đăng ký học phần
		HocKy hienTai = taoHocKy("HK1-2026-2027", 1, "2026-2027",
				LocalDate.of(2026, 9, 7), LocalDate.of(2027, 1, 17), 1);
		hienTai.setNgayMoDangKy(LocalDateTime.now().minusDays(3));
		hienTai.setNgayDongDangKy(LocalDateTime.now().plusDays(14));
		hocKyRepository.save(hienTai);

		log.info("Đã tạo {} học kỳ.", hocKyRepository.count());
	}

	private HocKy taoHocKy(String code, int so, String namHoc,
	                       LocalDate batDau, LocalDate ketThuc, int trangThai) {
		HocKy hk = new HocKy();
		hk.setMaHocKyCode(code);
		hk.setTenHocKy("Học kỳ " + so + " năm học " + namHoc);
		hk.setNamHoc(namHoc);
		hk.setHocKySo(so);
		hk.setNgayBatDau(batDau);
		hk.setNgayKetThuc(ketThuc);
		hk.setTrangThai(trangThai);
		return hocKyRepository.save(hk);
	}

	// ===================== HỌC PHẦN =====================
	private void seedHocPhan() {
		if (hocPhanRepository.count() > 0) return;

		DonVi cntt = donViRepository.findByTenDonVi("Khoa Công nghệ Thông tin").orElse(null);
		if (cntt == null) {
			log.warn("Không tìm thấy Khoa CNTT, bỏ qua seed học phần.");
			return;
		}

		Object[][] ds = {
			{"INPR130285", "Nhập môn lập trình", 3},
			{"MATH132401", "Toán cao cấp A1", 3},
			{"MATH132501", "Toán cao cấp A2", 3},
			{"ENGL130137", "Anh văn 1", 3},
			{"ENGL230237", "Anh văn 2", 3},
			{"PRTE230385", "Kỹ thuật lập trình", 3},
			{"DASA230179", "Cấu trúc dữ liệu và giải thuật", 3},
			{"DIGR240485", "Toán rời rạc", 4},
			{"CAAL230180", "Kiến trúc máy tính", 3},
			{"OOPR230279", "Lập trình hướng đối tượng", 3},
			{"DBSY230184", "Cơ sở dữ liệu", 3},
			{"OPSY330280", "Hệ điều hành", 3},
			{"NEES330380", "Mạng máy tính căn bản", 3},
			{"WEPR330479", "Lập trình Web", 3},
			{"DBMS330284", "Hệ quản trị cơ sở dữ liệu", 3},
			{"SOEN330679", "Công nghệ phần mềm", 3},
			{"ARIN330585", "Trí tuệ nhân tạo", 3},
			{"MOPR331279", "Lập trình di động", 3},
			{"ITPM430884", "Quản lý dự án công nghệ thông tin", 3},
			{"MALE431085", "Học máy", 3},
			{"INSE330380", "An toàn thông tin", 3},
			{"CLCO432779", "Điện toán đám mây", 3},
		};

		for (Object[] r : ds) {
			HocPhan hp = new HocPhan();
			hp.setMaHocPhan((String) r[0]);
			hp.setTenHocPhan((String) r[1]);
			hp.setSoTinChi((Integer) r[2]);
			hp.setSoTietLyThuyet(30);
			hp.setSoTietThucHanh(30);
			hp.setDonVi(cntt);
			hp.setTrangThai(1);
			hocPhanRepository.save(hp);
		}
		log.info("Đã tạo {} học phần.", hocPhanRepository.count());
	}

	// ===================== LỚP HỌC PHẦN (ĐÃ KẾT THÚC) =====================
	private void seedLopHocPhan() {
		if (lopHocPhanRepository.count() > 0) return;

		KE_HOACH.forEach((maHk, dsMaHp) -> {
			HocKy hk = hocKyRepository.findByMaHocKyCode(maHk).orElse(null);
			if (hk == null) return;
			for (String maHp : dsMaHp) {
				hocPhanRepository.findByMaHocPhan(maHp).ifPresent(hp -> {
					LopHocPhan lop = new LopHocPhan();
					lop.setMaLop(maHp + "_" + maHk);
					lop.setHocPhan(hp);
					lop.setHocKy(hk);
					lop.setSiSoToiDa(60);
					lop.setSiSoToiThieu(15);
					lop.setTrangThai(3);   // đã kết thúc
					lopHocPhanRepository.save(lop);
				});
			}
		});
		log.info("Đã tạo {} lớp học phần.", lopHocPhanRepository.count());
	}

	// ===================== ĐĂNG KÝ + KẾT QUẢ =====================
	private void seedDangKyVaKetQua() {
		if (dangKyHocPhanRepository.count() > 0) return;

		SinhVien sv = sinhVienRepository.timTheoTenDangNhap("sinhvien").orElse(null);
		if (sv == null) {
			log.warn("Chưa có sinh viên mẫu, bỏ qua seed đăng ký.");
			return;
		}

		// Seed cố định để lần nào tạo lại cũng ra cùng bộ điểm
		Random rd = new Random(22110123);

		// Chỉ lấy lớp đã kết thúc (TrangThai = 3); học kỳ đã được nạp sẵn nhờ @EntityGraph
		for (LopHocPhan lop : lopHocPhanRepository.findByTrangThai(3)) {
			DangKyHocPhan dk = new DangKyHocPhan();
			dk.setSinhVien(sv);
			dk.setLopHocPhan(lop);
			dk.setLoaiDangKy(1);
			dk.setTrangThai(1);
			dk.setNgayDangKy(lop.getHocKy().getNgayBatDau().minusDays(14).atStartOfDay());
			dk = dangKyHocPhanRepository.save(dk);

			BigDecimal qt = diemNgauNhien(rd, 6.0, 10.0);
			BigDecimal gk = diemNgauNhien(rd, 5.0, 10.0);
			BigDecimal ck = diemNgauNhien(rd, 3.5, 10.0);
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
			quyDoiDiem(kq, tk.doubleValue());
			kq.setTrangThai(3);   // đã công bố
			kq.setNgayNhap(lop.getHocKy().getNgayKetThuc().atStartOfDay());
			ketQuaHocPhanRepository.save(kq);
		}
		log.info("Đã tạo {} lượt đăng ký kèm điểm cho sinh viên {}.",
				dangKyHocPhanRepository.count(), sv.getMaSoSinhVien());
	}

	/** Điểm ngẫu nhiên trong khoảng [min, max], làm tròn 0.5 */
	private BigDecimal diemNgauNhien(Random rd, double min, double max) {
		double d = min + rd.nextDouble() * (max - min);
		d = Math.round(d * 2) / 2.0;
		return BigDecimal.valueOf(d).setScale(1, RoundingMode.HALF_UP);
	}

	/** Quy đổi thang 10 → điểm chữ và thang 4 */
	private void quyDoiDiem(KetQuaHocPhan kq, double tk) {
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

	// ===================== LỚP MỞ ĐĂNG KÝ (HỌC KỲ HIỆN TẠI) =====================
	private void seedLopMoDangKy() {
		HocKy hk = hocKyRepository.findByMaHocKyCode("HK1-2026-2027").orElse(null);
		if (hk == null || lopHocPhanRepository.existsByHocKy_MaHocKy(hk.getMaHocKy())) return;

		DonVi cntt = donViRepository.findByTenDonVi("Khoa Công nghệ Thông tin").orElse(null);
		if (cntt == null) return;

		Object[][] ds = {
			{"PROJ430879", "Tiểu luận chuyên ngành", 3},
			{"BDAN433877", "Phân tích dữ liệu lớn", 3},
			{"SOTE431079", "Kiểm thử phần mềm", 3},
			{"AWPR431279", "Lập trình Web nâng cao", 3},
			{"BLCH431585", "Công nghệ Blockchain", 3},
			{"IOTS431485", "Internet vạn vật", 3},
		};

		for (Object[] r : ds) {
			String ma = (String) r[0];

			HocPhan hp = hocPhanRepository.findByMaHocPhan(ma).orElseGet(() -> {
				HocPhan moi = new HocPhan();
				moi.setMaHocPhan(ma);
				moi.setTenHocPhan((String) r[1]);
				moi.setSoTinChi((Integer) r[2]);
				moi.setSoTietLyThuyet(30);
				moi.setSoTietThucHanh(30);
				moi.setDonVi(cntt);
				moi.setTrangThai(1);
				return hocPhanRepository.save(moi);
			});

			for (int nhom = 1; nhom <= 2; nhom++) {
				LopHocPhan lop = new LopHocPhan();
				lop.setMaLop(ma + "_" + hk.getMaHocKyCode() + "_0" + nhom);
				lop.setHocPhan(hp);
				lop.setHocKy(hk);
				lop.setSiSoToiDa(nhom == 1 ? 40 : 50);
				lop.setSiSoToiThieu(15);
				lop.setTrangThai(1);   // đang mở đăng ký
				lopHocPhanRepository.save(lop);
			}
		}
		log.info("Đã mở {} lớp cho {}.", ds.length * 2, hk.getMaHocKyCode());
	}
}
