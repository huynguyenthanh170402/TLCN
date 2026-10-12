package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhongHocRepository;

/**
 * Bổ sung phòng học cho đủ nhu cầu mở lớp hàng loạt.
 * 6 tòa nhà, mỗi tòa 5 tầng, mỗi tầng 6 phòng: 180 phòng.
 */
@Component
@Order(7)
@RequiredArgsConstructor
@Slf4j
public class PhongHocSeeder implements CommandLineRunner {

	private static final int SO_PHONG_TOI_THIEU = 100;

	private final PhongHocRepository phongHocRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (phongHocRepository.count() >= SO_PHONG_TOI_THIEU) return;

		int soTao = 0;
		for (char toa = 'A'; toa <= 'F'; toa++) {
			for (int tang = 1; tang <= 5; tang++) {
				for (int phong = 1; phong <= 6; phong++) {
					String ma = String.format("%c%d-%d%02d", toa, tang, tang, phong);
					if (phongHocRepository.findAll().stream()
							.anyMatch(p -> p.getMaPhong().equals(ma))) continue;

					PhongHoc p = new PhongHoc();
					p.setMaPhong(ma);
					p.setTenPhong("Phòng " + ma);
					p.setToaNha("Tòa " + toa);
					p.setTang(String.valueOf(tang));
					// Phòng máy nhỏ hơn, giảng đường lớn hơn
					p.setSucChua(phong <= 2 ? 45 : (phong == 6 ? 120 : 60));
					p.setLoaiPhong(phong <= 2 ? 2 : (phong == 6 ? 4 : 1));
					p.setTrangThai(1);
					phongHocRepository.save(p);
					soTao++;
				}
			}
		}
		log.info("Đã bổ sung {} phòng học, tổng cộng {} phòng.", soTao, phongHocRepository.count());
	}
}