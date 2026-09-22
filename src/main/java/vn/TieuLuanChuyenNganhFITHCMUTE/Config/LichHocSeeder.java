package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhongHocRepository;

/** Seed phòng học và lịch học cho các lớp đang mở. Chạy sau HocTapSeeder. */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class LichHocSeeder implements CommandLineRunner {

	private final PhongHocRepository phongHocRepository;
	private final LichHocRepository lichHocRepository;
	private final LopHocPhanRepository lopHocPhanRepository;

	@Override
	@Transactional
	public void run(String... args) {
		seedPhongHoc();
		seedLichHoc();
	}

	private void seedPhongHoc() {
		if (phongHocRepository.count() > 0) return;

		taoPhong("A1-101", "Khu A1", "1", 60, 1);
		taoPhong("A1-102", "Khu A1", "1", 60, 1);
		taoPhong("A2-201", "Khu A2", "2", 80, 1);
		taoPhong("A3-301", "Khu A3", "3", 45, 2);
		taoPhong("A3-302", "Khu A3", "3", 45, 2);
		taoPhong("A4-401", "Khu A4", "4", 100, 4);
		log.info("Đã tạo {} phòng học.", phongHocRepository.count());
	}

	private void taoPhong(String ma, String toaNha, String tang, int sucChua, int loai) {
		PhongHoc p = new PhongHoc();
		p.setMaPhong(ma);
		p.setTenPhong("Phòng " + ma);
		p.setToaNha(toaNha);
		p.setTang(tang);
		p.setSucChua(sucChua);
		p.setLoaiPhong(loai);
		p.setTrangThai(1);
		phongHocRepository.save(p);
	}

	/**
	 * Xếp lịch cho 12 lớp đang mở: 6 lớp đầu học buổi sáng (tiết 1–3),
	 * 6 lớp sau học buổi chiều (tiết 7–9), rải từ Thứ 2 đến Thứ 7.
	 */
	private void seedLichHoc() {
		if (lichHocRepository.count() > 0) return;

		List<LopHocPhan> dsLop = new ArrayList<>(lopHocPhanRepository.findByTrangThai(1));
		if (dsLop.isEmpty()) return;
		dsLop.sort(Comparator.comparing(LopHocPhan::getMaLop));

		List<PhongHoc> dsPhong = phongHocRepository.findAll();

		for (int i = 0; i < dsLop.size(); i++) {
			LopHocPhan lop = dsLop.get(i);

			LichHoc lh = new LichHoc();
			lh.setLopHocPhan(lop);
			lh.setThu(2 + (i % 6));                 // Thứ 2 → Thứ 7
			lh.setTietBatDau(i < 6 ? 1 : 7);        // sáng hoặc chiều
			lh.setSoTiet(3);
			lh.setPhongHoc(dsPhong.get(i % dsPhong.size()));
			lh.setNgayBatDau(lop.getHocKy().getNgayBatDau());
			lh.setNgayKetThuc(lop.getHocKy().getNgayKetThuc());
			lichHocRepository.save(lh);
		}
		log.info("Đã xếp lịch cho {} lớp học phần.", lichHocRepository.count());
	}
}