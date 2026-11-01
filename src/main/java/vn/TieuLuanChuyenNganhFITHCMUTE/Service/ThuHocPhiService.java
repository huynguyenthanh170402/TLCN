package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhieuThu;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.CongNoHocPhiRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhieuThuRepository;

/**
 * Nghiệp vụ thu học phí của Phòng Kế hoạch Tài chính.
 *
 * Ba việc: sinh công nợ cho cả học kỳ, tra cứu công nợ, và ghi nhận tiền nộp.
 */
@Service
@RequiredArgsConstructor
public class ThuHocPhiService {

	private static final int DA_DANG_KY = 1;
	private static final int PHIEU_HIEU_LUC = 1;
	private static final int PHIEU_DA_HUY = 0;

	private final CongNoHocPhiRepository congNoRepository;
	private final PhieuThuRepository phieuThuRepository;
	private final HocKyRepository hocKyRepository;
	private final HocPhiService hocPhiService;
	private final EntityManager em;

	/** Bốn con số cho hàng thẻ thống kê phía trên bảng công nợ */
	@Getter
	public static class ThongKe {
		private BigDecimal phaiThu = BigDecimal.ZERO;
		private BigDecimal daThu = BigDecimal.ZERO;
		private long soDongCongNo;
		private long soDongConNo;

		public BigDecimal getConNo() {
			return phaiThu.subtract(daThu).max(BigDecimal.ZERO);
		}

		/** Phần trăm đã thu, làm tròn một chữ số thập phân */
		public double getTyLeThu() {
			if (phaiThu.signum() == 0) return 0;
			return Math.round(daThu.doubleValue() * 1000.0 / phaiThu.doubleValue()) / 10.0;
		}
	}

	// ===================== TRA CỨU =====================

	@Transactional(readOnly = true)
	public Page<CongNoHocPhi> traCuu(Integer maHocKy, Integer trangThai,
	                                 String tuKhoa, Pageable pageable) {
		String khoa = (tuKhoa == null) ? "" : tuKhoa.trim();
		return congNoRepository.traCuu(
				maHocKy == null ? 0 : 1, maHocKy == null ? 0 : maHocKy,
				trangThai == null ? 0 : 1, trangThai == null ? 0 : trangThai,
				khoa.isEmpty() ? 0 : 1, khoa,
				pageable);
	}

	@Transactional(readOnly = true)
	public ThongKe thongKe(Integer maHocKy) {
		List<Object[]> rows = congNoRepository.thongKe(
				maHocKy == null ? 0 : 1, maHocKy == null ? 0 : maHocKy);

		ThongKe tk = new ThongKe();
		if (rows.isEmpty()) return tk;

		Object[] r = rows.get(0);
		tk.phaiThu      = (BigDecimal) r[0];
		tk.daThu        = (BigDecimal) r[1];
		tk.soDongCongNo = ((Number) r[2]).longValue();
		tk.soDongConNo  = ((Number) r[3]).longValue();
		return tk;
	}

	@Transactional(readOnly = true)
	public CongNoHocPhi chiTiet(Integer maCongNo) {
		return congNoRepository.timChiTiet(maCongNo)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy công nợ."));
	}

	@Transactional(readOnly = true)
	public List<PhieuThu> dsPhieuCuaCongNo(Integer maCongNo) {
		return phieuThuRepository.findByCongNo_MaCongNoHocPhiOrderByNgayThuDesc(maCongNo);
	}

	@Transactional(readOnly = true)
	public Page<PhieuThu> soPhieuThu(Pageable pageable) {
		return phieuThuRepository.findAllByOrderByNgayThuDesc(pageable);
	}

	// ===================== SINH CÔNG NỢ HÀNG LOẠT =====================

	/**
	 * Tính công nợ cho mọi sinh viên có đăng ký học phần trong học kỳ.
	 *
	 * Chạy lại nhiều lần vẫn an toàn: capNhatCongNo tìm dòng cũ rồi ghi đè
	 * số tiền phải đóng, không tạo thêm dòng mới. Nhờ vậy sau khi sinh viên
	 * rút bớt học phần, chạy lại là số tiền tự khớp.
	 *
	 * Số tiền đã đóng không bị đụng tới, nên không làm mất dấu tiền đã thu.
	 */
	@Transactional
	public int sinhCongNo(Integer maHocKy) {
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Học kỳ không tồn tại."));

		List<SinhVien> dsSinhVien = em.createQuery("""
				SELECT DISTINCT dk.sinhVien FROM DangKyHocPhan dk
				WHERE dk.lopHocPhan.hocKy.maHocKy = :maHocKy AND dk.trangThai = :trangThai
				""", SinhVien.class)
				.setParameter("maHocKy", maHocKy)
				.setParameter("trangThai", DA_DANG_KY)
				.getResultList();

		for (SinhVien sv : dsSinhVien) {
			hocPhiService.capNhatCongNo(sv, hk);
		}
		return dsSinhVien.size();
	}

	// ===================== LẬP PHIẾU THU =====================

	/**
	 * Ghi nhận một lần nộp tiền.
	 *
	 * Số tiền cộng vào SoTienDaDong của công nợ rồi tính lại trạng thái.
	 * Không cho nộp quá số còn lại, tránh tạo ra số âm trong sổ sách.
	 */
	@Transactional
	public PhieuThu lapPhieuThu(Integer maCongNo, BigDecimal soTien,
	                            Integer hinhThuc, String nguoiThu, String ghiChu) {

		if (soTien == null || soTien.signum() <= 0) {
			throw new IllegalStateException("Số tiền phải lớn hơn 0.");
		}

		CongNoHocPhi cn = congNoRepository.findById(maCongNo)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy công nợ."));

		BigDecimal conLai = cn.getSoTienConLai();
		if (conLai.signum() == 0) {
			throw new IllegalStateException("Công nợ này đã đóng đủ.");
		}
		if (soTien.compareTo(conLai) > 0) {
			throw new IllegalStateException(
					"Số tiền vượt quá số còn phải đóng (" + dinhDang(conLai) + " đ).");
		}

		cn.setSoTienDaDong(cn.getSoTienDaDong().add(soTien));
		capNhatTrangThai(cn);
		congNoRepository.save(cn);

		PhieuThu pt = new PhieuThu();
		pt.setSoPhieu(sinhSoPhieu());
		pt.setCongNo(cn);
		pt.setSoTien(soTien);
		pt.setHinhThuc(hinhThuc == null ? 1 : hinhThuc);
		pt.setNgayThu(LocalDateTime.now());
		pt.setNguoiThu(nguoiThu);
		pt.setGhiChu(ghiChu);
		pt.setTrangThai(PHIEU_HIEU_LUC);
		return phieuThuRepository.save(pt);
	}

	/**
	 * Hủy một phiếu đã lập: trừ lại tiền khỏi công nợ, đánh dấu phiếu đã hủy.
	 * Phiếu vẫn nằm trong sổ để còn dấu vết, không xóa khỏi cơ sở dữ liệu.
	 */
	@Transactional
	public void huyPhieuThu(Integer maPhieuThu) {
		PhieuThu pt = phieuThuRepository.findById(maPhieuThu)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy phiếu thu."));

		if (pt.isDaHuy()) {
			throw new IllegalStateException("Phiếu này đã hủy trước đó.");
		}

		CongNoHocPhi cn = pt.getCongNo();
		BigDecimal conLaiSauHuy = cn.getSoTienDaDong().subtract(pt.getSoTien());
		cn.setSoTienDaDong(conLaiSauHuy.signum() < 0 ? BigDecimal.ZERO : conLaiSauHuy);
		capNhatTrangThai(cn);
		congNoRepository.save(cn);

		pt.setTrangThai(PHIEU_DA_HUY);
		phieuThuRepository.save(pt);
	}

	// ===================== HỖ TRỢ =====================

	/**
	 * Trạng thái sau khi tiền thay đổi. Khác hàm trong HocPhiService ở chỗ
	 * không cần biết số tín chỉ: dòng nào đã có số phải đóng thì chắc chắn
	 * có phát sinh, nên chỉ còn ba trạng thái 0, 1, 2.
	 */
	private void capNhatTrangThai(CongNoHocPhi cn) {
		if (cn.getSoTienConLai().signum() == 0)      cn.setTrangThai(2);
		else if (cn.getSoTienDaDong().signum() > 0)  cn.setTrangThai(1);
		else                                         cn.setTrangThai(0);
	}

	/** Số phiếu dạng PT20261026-0007: ngày lập cộng số thứ tự trong ngày */
	private String sinhSoPhieu() {
		LocalDate homNay = LocalDate.now();
		long soTrongNgay = phieuThuRepository.demTrongNgay(
				homNay.atStartOfDay(), homNay.plusDays(1).atStartOfDay()) + 1;

		return String.format("PT%04d%02d%02d-%04d",
				homNay.getYear(), homNay.getMonthValue(), homNay.getDayOfMonth(), soTrongNgay);
	}

	private String dinhDang(BigDecimal tien) {
		return String.format("%,d", tien.longValue());
	}
}
