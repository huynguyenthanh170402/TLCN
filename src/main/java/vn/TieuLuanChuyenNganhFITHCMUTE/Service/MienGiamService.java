package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.MienGiamHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.MienGiamHocPhiRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;

/**
 * Miễn giảm học phí và học bổng.
 *
 * Phòng Công tác Sinh viên lập và duyệt hồ sơ ở đây. Phòng Kế hoạch Tài chính
 * chỉ gọi hàm tongTyLeDaDuyet khi tính lại công nợ, không sửa gì.
 */
@Service
@RequiredArgsConstructor
public class MienGiamService {

	public static final int CHO_DUYET = 0;
	public static final int DA_DUYET  = 1;
	public static final int TU_CHOI   = 2;

	private static final BigDecimal TRAN_PHAN_TRAM = new BigDecimal("100");

	private final MienGiamHocPhiRepository mienGiamRepository;
	private final SinhVienRepository sinhVienRepository;
	private final HocKyRepository hocKyRepository;

	// ===================== DÀNH CHO TÀI CHÍNH =====================

	/**
	 * Tổng tỉ lệ được giảm của một sinh viên trong một học kỳ, đã chặn trần 100%.
	 *
	 * Một sinh viên có thể vừa thuộc diện chính sách vừa được học bổng, nên
	 * các quyết định cộng dồn. Nhưng không ai được giảm quá 100% học phí,
	 * nếu không công nợ sẽ ra số âm.
	 */
	@Transactional(readOnly = true)
	public BigDecimal tongTyLeDaDuyet(Integer maSinhVien, Integer maHocKy) {
		BigDecimal tong = mienGiamRepository.tongTyLeDaDuyet(maSinhVien, maHocKy);
		if (tong == null) return BigDecimal.ZERO;
		return tong.min(TRAN_PHAN_TRAM);
	}

	@Transactional(readOnly = true)
	public List<MienGiamHocPhi> canCu(Integer maSinhVien, Integer maHocKy) {
		return mienGiamRepository.dsDaDuyet(maSinhVien, maHocKy);
	}

	// ===================== TRA CỨU =====================

	@Transactional(readOnly = true)
	public Page<MienGiamHocPhi> traCuu(Integer maHocKy, Integer trangThai,
	                                   String tuKhoa, Pageable pageable) {
		String khoa = (tuKhoa == null) ? "" : tuKhoa.trim();
		return mienGiamRepository.traCuu(
				maHocKy == null ? 0 : 1, maHocKy == null ? 0 : maHocKy,
				trangThai == null ? 0 : 1, trangThai == null ? 0 : trangThai,
				khoa.isEmpty() ? 0 : 1, khoa,
				pageable);
	}

	@Transactional(readOnly = true)
	public long soHoSoChoDuyet() {
		return mienGiamRepository.countByTrangThai(CHO_DUYET);
	}

	// ===================== LẬP HỒ SƠ =====================

	/**
	 * Tạo đề nghị miễn giảm, trạng thái chờ duyệt.
	 *
	 * Nhận mã số sinh viên dạng chuỗi vì cán bộ nhập MSSV trên giấy tờ,
	 * không biết khóa chính trong cơ sở dữ liệu.
	 */
	@Transactional
	public void deNghi(String maSoSinhVien, Integer maHocKy, Integer loai,
	                   String dienHuong, BigDecimal tyLe, String ghiChu,
	                   String nguoiDeNghi) {

		SinhVien sv = sinhVienRepository.timTheoMaSo(chuanHoa(maSoSinhVien))
				.orElseThrow(() -> new IllegalStateException(
						"Không tìm thấy sinh viên có MSSV " + maSoSinhVien + "."));

		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Học kỳ không tồn tại."));

		kiemTraTyLe(tyLe);

		// Cảnh báo sớm nếu cộng vào sẽ vượt 100%: để cán bộ biết ngay lúc lập
		// hồ sơ, thay vì tới lúc Tài chính tính tiền mới phát hiện.
		BigDecimal daCo = mienGiamRepository.tongTyLeDaDuyet(sv.getMaSinhVien(), maHocKy);
		if (daCo != null && daCo.add(tyLe).compareTo(TRAN_PHAN_TRAM) > 0) {
			throw new IllegalStateException(
					"Sinh viên này đã được duyệt " + daCo.stripTrailingZeros().toPlainString()
					+ "% trong học kỳ. Cộng thêm " + tyLe.stripTrailingZeros().toPlainString()
					+ "% sẽ vượt quá 100%.");
		}

		MienGiamHocPhi mg = new MienGiamHocPhi();
		mg.setSinhVien(sv);
		mg.setHocKy(hk);
		mg.setLoai(loai == null ? 1 : loai);
		mg.setDienHuong(dienHuong);
		mg.setTyLePhanTram(tyLe);
		mg.setTrangThai(CHO_DUYET);
		mg.setNguoiDeNghi(nguoiDeNghi);
		mg.setNgayDeNghi(LocalDateTime.now());
		mg.setGhiChu(ghiChu);
		mienGiamRepository.save(mg);
	}

	// ===================== DUYỆT =====================

	@Transactional
	public void duyet(Integer maMienGiam, String nguoiDuyet) {
		MienGiamHocPhi mg = tim(maMienGiam);

		if (!mg.isChoDuyet()) {
			throw new IllegalStateException("Hồ sơ này đã được xử lý rồi.");
		}

		// Kiểm tra lại lúc duyệt, vì giữa lúc lập và lúc duyệt có thể đã có
		// hồ sơ khác của cùng sinh viên được duyệt trước.
		BigDecimal daCo = tongTyLeDaDuyet(mg.getSinhVien().getMaSinhVien(),
		                                  mg.getHocKy().getMaHocKy());
		if (daCo.add(mg.getTyLePhanTram()).compareTo(TRAN_PHAN_TRAM) > 0) {
			throw new IllegalStateException(
					"Không duyệt được: tổng tỉ lệ của sinh viên sẽ vượt quá 100%.");
		}

		mg.setTrangThai(DA_DUYET);
		mg.setNguoiDuyet(nguoiDuyet);
		mg.setNgayDuyet(LocalDateTime.now());
		mienGiamRepository.save(mg);
	}

	@Transactional
	public void tuChoi(Integer maMienGiam, String nguoiDuyet, String lyDo) {
		MienGiamHocPhi mg = tim(maMienGiam);

		if (!mg.isChoDuyet()) {
			throw new IllegalStateException("Hồ sơ này đã được xử lý rồi.");
		}

		mg.setTrangThai(TU_CHOI);
		mg.setNguoiDuyet(nguoiDuyet);
		mg.setNgayDuyet(LocalDateTime.now());
		if (lyDo != null && !lyDo.isBlank()) {
			mg.setGhiChu(lyDo);
		}
		mienGiamRepository.save(mg);
	}

	/**
	 * Thu hồi một quyết định đã duyệt, đưa về trạng thái từ chối.
	 *
	 * Không xóa khỏi cơ sở dữ liệu để còn dấu vết. Sau khi thu hồi, Tài chính
	 * cần tính lại công nợ thì số tiền miễn giảm mới trở về đúng.
	 */
	@Transactional
	public void thuHoi(Integer maMienGiam, String nguoiDuyet) {
		MienGiamHocPhi mg = tim(maMienGiam);

		if (!mg.isDaDuyet()) {
			throw new IllegalStateException("Chỉ thu hồi được hồ sơ đã duyệt.");
		}

		mg.setTrangThai(TU_CHOI);
		mg.setNguoiDuyet(nguoiDuyet);
		mg.setNgayDuyet(LocalDateTime.now());
		mg.setGhiChu("Đã thu hồi quyết định. " + (mg.getGhiChu() == null ? "" : mg.getGhiChu()));
		mienGiamRepository.save(mg);
	}

	// ===================== HỖ TRỢ =====================

	@Transactional(readOnly = true)
	public MienGiamHocPhi tim(Integer maMienGiam) {
		return mienGiamRepository.findById(maMienGiam)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy hồ sơ."));
	}

	private void kiemTraTyLe(BigDecimal tyLe) {
		if (tyLe == null
				|| tyLe.signum() <= 0
				|| tyLe.compareTo(TRAN_PHAN_TRAM) > 0) {
			throw new IllegalStateException("Tỉ lệ phải nằm trong khoảng từ 1 đến 100.");
		}
	}

	private String chuanHoa(String maSo) {
		return maSo == null ? "" : maSo.trim();
	}
}
