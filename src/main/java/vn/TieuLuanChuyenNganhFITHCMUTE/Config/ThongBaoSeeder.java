package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.NguoiNhanThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBaoQuyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NguoiNhanThongBaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.QuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ThongBaoQuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ThongBaoRepository;

/**
 * Seed thông báo mẫu, đủ cả 3 chế độ người nhận.
 * Quyền: 1 Sinh viên, 2 Giảng viên, 3 Chuyên viên, 4 BGH, 5 Phòng ban.
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class ThongBaoSeeder implements CommandLineRunner {

	private final ThongBaoRepository thongBaoRepository;
	private final ThongBaoQuyenRepository thongBaoQuyenRepository;
	private final NguoiNhanThongBaoRepository nguoiNhanRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final QuyenRepository quyenRepository;
	private final DonViRepository donViRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (thongBaoRepository.count() > 0) return;

		TaiKhoan phongBan = taiKhoanRepository.findByTenDangNhap("phongban").orElse(null);
		TaiKhoan bgh      = taiKhoanRepository.findByTenDangNhap("bgh").orElse(null);
		TaiKhoan sv       = taiKhoanRepository.findByTenDangNhap("sinhvien").orElse(null);
		if (phongBan == null || bgh == null || sv == null) return;

		DonVi daoTao   = donViRepository.findByTenDonVi("Phòng Đào tạo").orElse(null);
		DonVi taiChinh = donViRepository.findByTenDonVi("Phòng Kế hoạch Tài chính").orElse(null);
		DonVi ctsv     = donViRepository.findByTenDonVi("Phòng Công tác Sinh viên").orElse(null);

		// 1. Phòng Đào tạo → sinh viên
		taoTheoQuyen(phongBan, daoTao, 2, 5,
				"Thông báo đăng ký học phần Học kỳ 1 năm học 2026-2027",
				"Phòng Đào tạo thông báo thời gian đăng ký học phần HK1 năm học 2026-2027.\n\n"
				+ "Sinh viên đăng nhập cổng sinh viên, vào mục Đăng ký học phần để chọn lớp.\n"
				+ "Mỗi sinh viên được đăng ký tối đa 25 tín chỉ. Lưu ý không chọn các lớp trùng lịch.",
				List.of(1));

		// 2. Phòng Kế hoạch Tài chính → sinh viên
		taoTheoQuyen(phongBan, taiChinh, 3, 3,
				"Hạn nộp học phí Học kỳ 1 năm học 2026-2027",
				"Học phí được tính theo số tín chỉ đã đăng ký, đơn giá 850.000 đồng/tín chỉ.\n\n"
				+ "Hạn nộp: 30 ngày kể từ ngày bắt đầu học kỳ. "
				+ "Sinh viên xem chi tiết công nợ tại mục Học phí.",
				List.of(1));

		// 3. Phòng Công tác Sinh viên → toàn trường
		ThongBao khaiGiang = taoThongBao(phongBan, ctsv, 4, 1, 7,
				"Lễ khai giảng năm học 2026-2027",
				"Nhà trường tổ chức Lễ khai giảng năm học 2026-2027 vào 7 giờ 30 sáng thứ Hai tuần tới "
				+ "tại Hội trường A4-401.\n\nĐề nghị toàn thể cán bộ, giảng viên và sinh viên tham dự đầy đủ.");
		log.debug("Khai giảng: {}", khaiGiang.getMaThongBao());

		// 4. Phòng Đào tạo → chuyên viên và phòng ban (sinh viên KHÔNG thấy)
		taoTheoQuyen(phongBan, daoTao, 1, 2,
				"Họp triển khai kế hoạch đào tạo học kỳ mới",
				"Mời đại diện các phòng ban và chuyên viên phụ trách đào tạo dự họp triển khai "
				+ "kế hoạch đào tạo HK1 năm học 2026-2027 tại phòng họp tầng 2, khu hành chính.",
				List.of(3, 5));

		// 5. Ban Giám hiệu → phòng ban, chuyên viên, giảng viên (sinh viên KHÔNG thấy)
		taoTheoQuyen(bgh, null, 5, 1,
				"Chỉ đạo chuẩn bị công tác kiểm định chất lượng",
				"Ban Giám hiệu đề nghị các đơn vị rà soát minh chứng, hoàn thiện hồ sơ "
				+ "phục vụ đợt kiểm định chất lượng chương trình đào tạo, hạn nộp cuối tháng.",
				List.of(2, 3, 5));

		// 6. Phòng Đào tạo → riêng sinh viên 22110123
		ThongBao caNhan = taoThongBao(phongBan, daoTao, 1, 3, 0,
				"Nhắc nhở cập nhật hồ sơ cá nhân",
				"Hồ sơ của bạn còn thiếu ảnh đại diện. Vui lòng liên hệ Phòng Đào tạo để bổ sung.");
		NguoiNhanThongBao nn = new NguoiNhanThongBao();
		nn.setThongBao(caNhan);
		nn.setTaiKhoan(sv);
		nn.setNgayGui(caNhan.getNgayDang());
		nn.setTrangThaiDoc(0);
		nguoiNhanRepository.save(nn);

		log.info("Đã tạo {} thông báo mẫu.", thongBaoRepository.count());
	}

	private void taoTheoQuyen(TaiKhoan nguoiDang, DonVi donVi, int loai, int soNgayTruoc,
	                          String tieuDe, String noiDung, List<Integer> dsMaQuyen) {
		ThongBao tb = taoThongBao(nguoiDang, donVi, loai, 2, soNgayTruoc, tieuDe, noiDung);
		for (Integer maQuyen : dsMaQuyen) {
			ThongBaoQuyen tq = new ThongBaoQuyen();
			tq.setThongBao(tb);
			tq.setQuyen(quyenRepository.findById(maQuyen).orElseThrow());
			thongBaoQuyenRepository.save(tq);
		}
	}

	private ThongBao taoThongBao(TaiKhoan nguoiDang, DonVi donVi, int loai, int cheDo,
	                             int soNgayTruoc, String tieuDe, String noiDung) {
		ThongBao tb = new ThongBao();
		tb.setNguoiDang(nguoiDang);
		tb.setDonVi(donVi);
		tb.setLoaiThongBao(loai);
		tb.setCheDoNguoiNhan(cheDo);
		tb.setTieuDe(tieuDe);
		tb.setNoiDung(noiDung);
		tb.setNgayDang(LocalDateTime.now().minusDays(soNgayTruoc));
		tb.setTrangThai(1);
		return thongBaoRepository.save(tb);
	}
}
