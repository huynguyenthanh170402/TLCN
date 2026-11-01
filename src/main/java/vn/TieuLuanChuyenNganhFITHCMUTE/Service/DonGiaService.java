package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonGiaTinChi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonGiaTinChiRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;

/**
 * Tra và quản lý đơn giá tín chỉ.
 *
 * Trước đây đơn giá là hằng số nằm trong mã nguồn, muốn đổi phải sửa code
 * rồi biên dịch lại. Nay nằm trong bảng, Phòng Kế hoạch Tài chính tự sửa
 * được trên giao diện.
 */
@Service
@RequiredArgsConstructor
public class DonGiaService {

	/** Dùng khi học kỳ chưa khai báo đơn giá nào, để công nợ không ra 0 đồng */
	public static final BigDecimal DON_GIA_MAC_DINH = new BigDecimal("850000");

	private final DonGiaTinChiRepository donGiaRepository;
	private final HocKyRepository hocKyRepository;

	/**
	 * Đơn giá áp cho một sinh viên trong một học kỳ.
	 *
	 * Thứ tự ưu tiên: đơn giá riêng của khóa, rồi đơn giá chung của học kỳ,
	 * cuối cùng mới tới giá mặc định. Nhờ vậy trường tăng học phí cho khóa
	 * mới mà khóa cũ vẫn giữ giá cam kết lúc nhập học.
	 */
	@Transactional(readOnly = true)
	public BigDecimal timDonGia(Integer maHocKy, String khoaTuyenSinh) {
		if (maHocKy == null) return DON_GIA_MAC_DINH;

		if (khoaTuyenSinh != null && !khoaTuyenSinh.isBlank()) {
			var rieng = donGiaRepository
					.findByHocKy_MaHocKyAndKhoaTuyenSinh(maHocKy, khoaTuyenSinh.trim());
			if (rieng.isPresent()) return rieng.get().getDonGia();
		}

		return donGiaRepository.timDonGiaChung(maHocKy)
				.map(DonGiaTinChi::getDonGia)
				.orElse(DON_GIA_MAC_DINH);
	}

	@Transactional(readOnly = true)
	public List<DonGiaTinChi> danhSach() {
		return donGiaRepository.findAllByOrderByHocKy_NgayBatDauDescKhoaTuyenSinhAsc();
	}

	@Transactional(readOnly = true)
	public DonGiaTinChi tim(Integer maDonGia) {
		return donGiaRepository.findById(maDonGia)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn giá cần sửa."));
	}

	/**
	 * Thêm mới hoặc sửa một dòng đơn giá.
	 * maDonGia để trống là thêm mới.
	 */
	@Transactional
	public void luu(Integer maDonGia, Integer maHocKy, String khoaTuyenSinh,
	                BigDecimal donGia, String ghiChu) {

		if (maHocKy == null) {
			throw new IllegalStateException("Chưa chọn học kỳ.");
		}
		if (donGia == null || donGia.signum() < 0) {
			throw new IllegalStateException("Đơn giá phải là số không âm.");
		}

		HocKy hocKy = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Học kỳ không tồn tại."));

		// Chuỗi rỗng và chuỗi toàn khoảng trắng đều quy về null, để ràng buộc
		// duy nhất trên (MaHocKy, KhoaTuyenSinh) hiểu đây là dòng áp chung.
		String khoa = (khoaTuyenSinh == null || khoaTuyenSinh.isBlank())
				? null : khoaTuyenSinh.trim();

		DonGiaTinChi dg = (maDonGia == null)
				? new DonGiaTinChi()
				: tim(maDonGia);

		// Chặn trùng trước khi lưu, để người dùng nhận thông báo tiếng Việt
		// thay vì màn hình lỗi ràng buộc của SQL Server.
		var trung = (khoa == null)
				? donGiaRepository.timDonGiaChung(maHocKy)
				: donGiaRepository.findByHocKy_MaHocKyAndKhoaTuyenSinh(maHocKy, khoa);
		if (trung.isPresent() && !trung.get().getMaDonGia().equals(dg.getMaDonGia())) {
			throw new IllegalStateException(
					"Học kỳ này đã có đơn giá cho " +
					(khoa == null ? "mọi khóa" : "khóa " + khoa) + ".");
		}

		dg.setHocKy(hocKy);
		dg.setKhoaTuyenSinh(khoa);
		dg.setDonGia(donGia);
		dg.setGhiChu(ghiChu);
		donGiaRepository.save(dg);
	}

	@Transactional
	public void xoa(Integer maDonGia) {
		donGiaRepository.deleteById(maDonGia);
	}
}
