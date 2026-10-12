package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Quyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiangVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.QuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/**
 * Seed giảng viên cho từng khoa, mỗi khoa 8 người, kèm tài khoản đăng nhập.
 * Giảng viên đầu tiên gắn với tài khoản mẫu "giangvien".
 */
@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class GiangVienSeeder implements CommandLineRunner {

	private static final int SO_GV_MOI_KHOA = 8;
	private static final int LOAI_DON_VI_KHOA = 1;
	private static final int QUYEN_GIANG_VIEN = 2;
	private static final String MAT_KHAU = "123456";

	private static final String[] HO = {"Nguyễn","Trần","Lê","Phạm","Hoàng","Huỳnh","Phan","Vũ",
			"Võ","Đặng","Bùi","Đỗ","Hồ","Ngô","Dương","Lý"};
	private static final String[] DEM_NAM = {"Văn","Hữu","Đức","Minh","Quang","Thanh","Gia","Trọng"};
	private static final String[] DEM_NU  = {"Thị","Ngọc","Thanh","Minh","Khánh","Phương","Thu","Kim"};
	private static final String[] TEN_NAM = {"An","Bình","Cường","Dũng","Đạt","Hải","Hùng","Khoa",
			"Long","Minh","Nam","Phong","Quân","Sơn","Thành","Tuấn","Vinh","Trí"};
	private static final String[] TEN_NU  = {"Anh","Chi","Dung","Hà","Hằng","Hiền","Hương","Lan",
			"Linh","Mai","Ngân","Nhung","Phương","Thảo","Trang","Vân","Yến","Quỳnh"};
	private static final String[] HOC_VI = {"TS","ThS","ThS","ThS","PGS.TS","TS"};

	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final DonViRepository donViRepository;
	private final LopHocPhanRepository lopHocPhanRepository;
	private final QuyenRepository quyenRepository;
	private final PasswordEncoder passwordEncoder;

	@Override
	@Transactional
	public void run(String... args) {
		List<DonVi> dsKhoa = donViRepository.findByLoaiDonVi_MaLoaiDonViOrderByTenDonVi(LOAI_DON_VI_KHOA);
		if (dsKhoa.isEmpty()) return;

		long daCo = giangVienRepository.count();
		if (daCo >= dsKhoa.size() * (long) SO_GV_MOI_KHOA) return;

		Quyen quyenGv = quyenRepository.findById(QUYEN_GIANG_VIEN).orElse(null);
		if (quyenGv == null) return;

		// Mã hóa mật khẩu một lần rồi dùng chung, BCrypt chạy chậm có chủ đích
		String matKhauBam = passwordEncoder.encode(MAT_KHAU);
		Random rd = new Random(2026);
		int soTao = 0, thuTu = (int) daCo;

		for (DonVi khoa : dsKhoa) {
			for (int i = 0; i < SO_GV_MOI_KHOA; i++) {
				thuTu++;
				String maSo = String.format("GV%03d", thuTu);
				if (giangVienRepository.existsByMaSoGiangVien(maSo)) continue;

				// Giảng viên đầu tiên dùng tài khoản mẫu có sẵn
				String tenDangNhap = (thuTu == 1) ? "giangvien" : maSo.toLowerCase();
				TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(tenDangNhap).orElse(null);
				if (tk == null) {
					tk = new TaiKhoan();
					tk.setTenDangNhap(tenDangNhap);
					tk.setMatKhauBam(matKhauBam);
					tk.setQuyen(quyenGv);
					tk.setTrangThai(1);
					tk = taiKhoanRepository.save(tk);
				}

				boolean nu = rd.nextDouble() < 0.4;
				String hoTen = HO[rd.nextInt(HO.length)] + " "
						+ (nu ? DEM_NU[rd.nextInt(DEM_NU.length)] : DEM_NAM[rd.nextInt(DEM_NAM.length)]) + " "
						+ (nu ? TEN_NU[rd.nextInt(TEN_NU.length)] : TEN_NAM[rd.nextInt(TEN_NAM.length)]);

				GiangVien gv = new GiangVien();
				gv.setMaSoGiangVien(maSo);
				gv.setHoTen(hoTen);
				gv.setNgaySinh(LocalDate.of(1970 + rd.nextInt(25), 1 + rd.nextInt(12), 1 + rd.nextInt(28)));
				gv.setGioiTinh(nu ? 2 : 1);
				gv.setHocVi(HOC_VI[rd.nextInt(HOC_VI.length)]);
				gv.setEmailTruong(maSo.toLowerCase() + "@hcmute.edu.vn");
				gv.setSoDienThoai("09" + (10000000 + rd.nextInt(89999999)));
				gv.setChuyenMon(khoa.getTenDonVi().replace("Khoa ", ""));
				gv.setLoaiGiangVien(1);
				gv.setNgayBatDauCongTac(LocalDate.of(2005 + rd.nextInt(18), 9, 1));
				gv.setTrangThai(1);
				gv.setTaiKhoan(tk);
				gv.setDonVi(khoa);
				gv = giangVienRepository.save(gv);
				soTao++;

				// Giảng viên mẫu nhận luôn các lớp đang mở, để demo nhập điểm
				if (thuTu == 1) {
					for (LopHocPhan lop : lopHocPhanRepository.findByTrangThai(1)) {
						PhanCongGiangDay pc = new PhanCongGiangDay();
						pc.setLopHocPhan(lop);
						pc.setGiangVien(gv);
						pc.setVaiTro(1);
						pc.setTrangThai(1);
						phanCongRepository.save(pc);
					}
				}
			}
		}
		log.info("Đã tạo {} giảng viên, tổng cộng {}.", soTao, giangVienRepository.count());
	}
}