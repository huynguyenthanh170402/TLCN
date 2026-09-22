package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;

/** Phòng Đào tạo quản lý học kỳ và khung giờ đăng ký học phần */
@Service
@RequiredArgsConstructor
public class HocKyService {

	/** Khi bấm "Mở ngay" mà chưa có ngày đóng hợp lệ, mặc định mở 14 ngày */
	private static final int SO_NGAY_MO_MAC_DINH = 14;

	private final HocKyRepository hocKyRepository;

	public List<HocKy> tatCa() {
		return hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau"));
	}

	private HocKy tim(Integer maHocKy) {
		return hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));
	}

	/** Đặt khung giờ đăng ký cụ thể */
	@Transactional
	public HocKy datThoiGianDangKy(Integer maHocKy, LocalDateTime mo, LocalDateTime dong) {
		if (mo == null || dong == null)
			throw new IllegalStateException("Phải nhập đủ thời gian mở và đóng đăng ký.");
		if (!dong.isAfter(mo))
			throw new IllegalStateException("Thời gian đóng phải sau thời gian mở.");

		HocKy hk = tim(maHocKy);
		if (hk.getTrangThai() == 2)
			throw new IllegalStateException("Học kỳ đã kết thúc, không thể mở đăng ký.");

		hk.setNgayMoDangKy(mo);
		hk.setNgayDongDangKy(dong);
		return hocKyRepository.save(hk);
	}

	/** Mở đăng ký ngay lập tức */
	@Transactional
	public HocKy moNgay(Integer maHocKy) {
		HocKy hk = tim(maHocKy);
		if (hk.getTrangThai() == 2)
			throw new IllegalStateException("Học kỳ đã kết thúc, không thể mở đăng ký.");

		LocalDateTime now = LocalDateTime.now();
		hk.setNgayMoDangKy(now);
		if (hk.getNgayDongDangKy() == null || !hk.getNgayDongDangKy().isAfter(now)) {
			hk.setNgayDongDangKy(now.plusDays(SO_NGAY_MO_MAC_DINH));
		}
		return hocKyRepository.save(hk);
	}

	/** Đóng đăng ký ngay lập tức */
	@Transactional
	public HocKy dongNgay(Integer maHocKy) {
		HocKy hk = tim(maHocKy);
		LocalDateTime now = LocalDateTime.now();
		hk.setNgayDongDangKy(now);
		if (hk.getNgayMoDangKy() == null || hk.getNgayMoDangKy().isAfter(now)) {
			hk.setNgayMoDangKy(now.minusMinutes(1));
		}
		return hocKyRepository.save(hk);
	}
}