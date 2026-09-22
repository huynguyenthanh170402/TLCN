package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.time.LocalDate;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LoaiDonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Quyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LoaiDonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.QuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;


import org.springframework.core.annotation.Order;   // thêm dòng import này
/**
 * Chèn dữ liệu nền lúc khởi động.
 * Mỗi bảng có khối kiểm tra riêng, nên thêm bảng mới không ảnh hưởng bảng cũ.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1)  
public class DataSeeder implements CommandLineRunner {

	private final QuyenRepository quyenRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final LoaiDonViRepository loaiDonViRepository;
	private final DonViRepository donViRepository;
	private final NganhRepository nganhRepository;
	private final SinhVienRepository sinhVienRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	public void run(String... args) {
		seedQuyen();
		seedTaiKhoan();
		seedLoaiDonVi();
		seedDonVi();
		seedNganh();
		seedSinhVien();
	}

	// ===================== QUYỀN =====================
	private void seedQuyen() {
		if (quyenRepository.count() > 0) return;

		taoQuyen("Sinh viên");
		taoQuyen("Giảng viên");
		taoQuyen("Chuyên viên");
		taoQuyen("Ban Giám hiệu");
		taoQuyen("Phòng ban");
		log.info("Đã tạo 5 nhóm quyền.");
	}

	private void taoQuyen(String ten) {
		Quyen q = new Quyen();
		q.setTenQuyen(ten);
		q.setMoTa(ten);
		q.setTrangThai(1);
		quyenRepository.save(q);
	}

	// ===================== TÀI KHOẢN =====================
	private void seedTaiKhoan() {
		if (taiKhoanRepository.count() > 0) return;

		taoTaiKhoan("sinhvien",   "123456", 1);
		taoTaiKhoan("giangvien",  "123456", 2);
		taoTaiKhoan("chuyenvien", "123456", 3);
		taoTaiKhoan("bgh",        "123456", 4);
		taoTaiKhoan("phongban",   "123456", 5);
		log.info("Đã tạo 5 tài khoản mẫu. Mật khẩu: 123456");
	}

	private void taoTaiKhoan(String tenDangNhap, String matKhau, int maQuyen) {
		TaiKhoan tk = new TaiKhoan();
		tk.setTenDangNhap(tenDangNhap);
		tk.setMatKhauBam(passwordEncoder.encode(matKhau));
		tk.setQuyen(quyenRepository.findById(maQuyen).orElseThrow());
		tk.setTrangThai(1);
		taiKhoanRepository.save(tk);
	}

	// ===================== LOẠI ĐƠN VỊ =====================
	private void seedLoaiDonVi() {
		if (loaiDonViRepository.count() > 0) return;

		taoLoaiDonVi("Khoa");
		taoLoaiDonVi("Phòng ban");
		taoLoaiDonVi("Trung tâm");
		taoLoaiDonVi("Viện");
		log.info("Đã tạo 4 loại đơn vị.");
	}

	private void taoLoaiDonVi(String ten) {
		LoaiDonVi l = new LoaiDonVi();
		l.setTenLoaiDonVi(ten);
		l.setMoTa(ten);
		l.setTrangThai(1);
		loaiDonViRepository.save(l);
	}

	// ===================== ĐƠN VỊ =====================
	private void seedDonVi() {
		if (donViRepository.count() > 0) return;

		// 1 Khoa, 2 Phòng ban, 3 Trung tâm, 4 Viện
		LoaiDonVi khoa     = loaiDonViRepository.findById(1).orElseThrow();
		LoaiDonVi phongBan = loaiDonViRepository.findById(2).orElseThrow();
		LoaiDonVi trungTam = loaiDonViRepository.findById(3).orElseThrow();
		LoaiDonVi vien     = loaiDonViRepository.findById(4).orElseThrow();

		List.of("Khoa Điện - Điện tử", "Khoa Cơ khí", "Khoa Kinh tế",
				"Khoa Giao thông và Năng lượng", "Khoa Hóa học và Khoa học sự sống",
				"Khoa Công nghệ Thông tin", "Khoa Xây dựng", "Khoa Đào tạo Tiên tiến",
				"Khoa Chính trị và Luật", "Khoa Khoa học Ứng dụng", "Khoa Ngoại ngữ",
				"Khoa Thời trang và Du lịch", "Khoa In và Truyền thông")
			.forEach(ten -> taoDonVi(ten, khoa));

		List.of("Phòng Đào tạo", "Phòng Khoa học Công nghệ", "Phòng Kế hoạch Tài chính",
				"Phòng Tổ chức - Nhân sự", "Phòng Hành chính - Tổng hợp",
				"Phòng Khảo thí và Đảm bảo Chất lượng", "Phòng Công tác Sinh viên",
				"Phòng Quan hệ Doanh nghiệp", "Phòng Quan hệ Quốc tế",
				"Phòng Quản trị Cơ sở vật chất")
			.forEach(ten -> taoDonVi(ten, phongBan));

		List.of("Trung tâm Học liệu và Dạy học số", "Trung tâm Công nghệ Phần mềm",
				"Trung tâm Ngoại ngữ", "Trung tâm Tin học",
				"Trung tâm Thông tin - Máy tính")
			.forEach(ten -> taoDonVi(ten, trungTam));

		List.of("Viện Đào tạo quốc tế", "Viện Sư phạm Kỹ thuật", "Viện Sau đại học")
			.forEach(ten -> taoDonVi(ten, vien));

		log.info("Đã tạo {} đơn vị.", donViRepository.count());
	}

	private void taoDonVi(String ten, LoaiDonVi loai) {
		DonVi dv = new DonVi();
		dv.setTenDonVi(ten);
		dv.setLoaiDonVi(loai);
		dv.setMoTa(ten);
		dv.setTrangThai(1);
		donViRepository.save(dv);
	}

	// ===================== NGÀNH =====================
	private void seedNganh() {
		if (nganhRepository.count() > 0) return;

		taoNganhTheoKhoa("Khoa Cơ khí",
				"Công nghệ chế tạo máy", "Công nghệ kỹ thuật cơ điện tử",
				"Công nghệ kỹ thuật cơ khí", "Kỹ thuật công nghiệp",
				"Kỹ nghệ gỗ và nội thất", "Robot và trí tuệ nhân tạo");

		taoNganhTheoKhoa("Khoa Giao thông và Năng lượng",
				"Công nghệ kỹ thuật nhiệt", "Công nghệ kỹ thuật ô tô",
				"Năng lượng tái tạo");

		taoNganhTheoKhoa("Khoa Hóa học và Khoa học sự sống",
				"Công nghệ kỹ thuật hóa học", "Công nghệ kỹ thuật môi trường",
				"Công nghệ thực phẩm", "Môi trường và phát triển bền vững",
				"Khoa học thực phẩm và dinh dưỡng");

		taoNganhTheoKhoa("Khoa Thời trang và Du lịch",
				"Công nghệ may", "Kinh tế gia đình", "Thiết kế thời trang",
				"Quản trị nhà hàng và dịch vụ ăn uống");

		taoNganhTheoKhoa("Khoa Công nghệ Thông tin",
				"Công nghệ thông tin", "Kỹ thuật dữ liệu", "An toàn thông tin");

		taoNganhTheoKhoa("Khoa Điện - Điện tử",
				"Công nghệ kỹ thuật điện - điện tử",
				"Công nghệ kỹ thuật điện tử - viễn thông",
				"Công nghệ kỹ thuật điều khiển và tự động hóa",
				"Công nghệ kỹ thuật máy tính", "Kỹ thuật y sinh");

		taoNganhTheoKhoa("Khoa In và Truyền thông",
				"Công nghệ in", "Truyền thông số và Công nghệ đa phương tiện");

		taoNganhTheoKhoa("Khoa Khoa học Ứng dụng",
				"Công nghệ vật liệu", "Vật lý kỹ thuật");

		taoNganhTheoKhoa("Khoa Kinh tế",
				"Kế toán", "Logistics và quản lý chuỗi cung ứng",
				"Quản lý công nghiệp", "Thương mại điện tử",
				"Kinh doanh quốc tế", "Quản trị kinh doanh", "Công nghệ tài chính");

		taoNganhTheoKhoa("Khoa Ngoại ngữ",
				"Ngôn ngữ Anh", "Sư phạm Tiếng Anh");

		taoNganhTheoKhoa("Khoa Xây dựng",
				"Công nghệ kỹ thuật công trình xây dựng",
				"Kỹ thuật xây dựng công trình giao thông", "Kiến trúc",
				"Hệ thống kỹ thuật công trình xây dựng", "Kiến trúc nội thất",
				"Quản lý và vận hành hạ tầng");

		taoNganhTheoKhoa("Viện Sư phạm Kỹ thuật",
				"Sư phạm Công nghệ", "Tâm lý học giáo dục");

		taoNganhTheoKhoa("Khoa Chính trị và Luật", "Luật");

		log.info("Đã tạo {} ngành.", nganhRepository.count());
	}

	private void taoNganhTheoKhoa(String tenKhoa, String... tenNganhs) {
		DonVi khoa = donViRepository.findByTenDonVi(tenKhoa).orElse(null);
		if (khoa == null) {
			log.warn("Không tìm thấy đơn vị: {}", tenKhoa);
			return;
		}
		for (String ten : tenNganhs) {
			Nganh n = new Nganh();
			n.setTenNganh(ten);
			n.setDonVi(khoa);
			n.setTrangThai(1);
			nganhRepository.save(n);
		}
	}

	// ===================== SINH VIÊN =====================
	private void seedSinhVien() {
		if (sinhVienRepository.count() > 0) return;

		TaiKhoan tk = taiKhoanRepository.findByTenDangNhap("sinhvien").orElse(null);
		Nganh cntt = nganhRepository.findByTenNganh("Công nghệ thông tin").orElse(null);

		if (tk == null || cntt == null) {
			log.warn("Thiếu tài khoản 'sinhvien' hoặc ngành CNTT, bỏ qua seed sinh viên.");
			return;
		}

		SinhVien sv = new SinhVien();
		sv.setMaSoSinhVien("22110123");
		sv.setTaiKhoan(tk);
		sv.setNganh(cntt);
		sv.setKhoaTuyenSinh("K2022");

		sv.setHoTen("Nguyễn Văn An");
		sv.setNgaySinh(LocalDate.of(2004, 5, 12));
		sv.setGioiTinh(1);
		sv.setNoiSinh("TP. Hồ Chí Minh");
		sv.setQueQuan("Long An");
		sv.setQuocTich("Việt Nam");
		sv.setDanToc("Kinh");

		sv.setLoaiGiayTo(1);
		sv.setSoGiayTo("079204001234");
		sv.setNgayCapGiayTo(LocalDate.of(2022, 3, 15));
		sv.setNoiCapGiayTo("Cục Cảnh sát QLHC về TTXH");

		sv.setEmailCaNhan("nguyenvanan@gmail.com");
		sv.setEmailTruong("22110123@student.hcmute.edu.vn");
		sv.setSoDienThoai("0901234567");
		sv.setDiaChiThuongTru("1 Võ Văn Ngân, TP. Thủ Đức, TP. Hồ Chí Minh");
		sv.setDiaChiLienHe("1 Võ Văn Ngân, TP. Thủ Đức, TP. Hồ Chí Minh");
		sv.setHoTenNguoiLienHe("Nguyễn Văn Bình");
		sv.setQuanHeVoiSinhVien("Cha");
		sv.setSoDienThoaiNguoiLienHe("0912345678");

		sv.setNgayNhapHoc(LocalDate.of(2022, 9, 5));
		sv.setNgayTotNghiepDuKien(LocalDate.of(2026, 9, 5));
		sv.setSoQuyetDinhNhapHoc("QĐ-1234/ĐHSPKT");
		sv.setTrangThai(1);

		sinhVienRepository.save(sv);
		log.info("Đã tạo sinh viên mẫu 22110123 cho tài khoản 'sinhvien'.");
	}
}