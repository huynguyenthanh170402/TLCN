package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

/**
 * Báo cáo thống kê cho Ban Giám hiệu (chỉ đọc).
 * Dùng JPQL có GROUP BY qua EntityManager để gom số liệu ngay trong database.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BaoCaoService {

	private static final BigDecimal MOT_TRIEU = new BigDecimal("1000000");

	private final EntityManager em;

	/** Toàn bộ số liệu cho trang tổng quan; các cặp nhãn/giá trị dùng để vẽ biểu đồ */
	@Getter @Setter
	public static class BaoCao {
		// Thẻ tổng quan
		private long tongSinhVien;
		private long tongGiangVien;
		private long soLopHocKyNay;
		private BigDecimal tongPhaiThu = BigDecimal.ZERO;
		private BigDecimal tongDaThu = BigDecimal.ZERO;

		// Sinh viên theo khoa
		private List<String> khoaNhan = new ArrayList<>();
		private List<Long> khoaSoLuong = new ArrayList<>();

		// Xếp loại học lực theo GPA tích lũy
		private List<String> xepLoaiNhan = List.of("Xuất sắc", "Giỏi", "Khá", "Trung bình", "Yếu");
		private List<Long> xepLoaiSoLuong = new ArrayList<>();

		// Phân bố điểm chữ
		private List<String> diemChuNhan = List.of("A", "B+", "B", "C+", "C", "D+", "D", "F");
		private List<Long> diemChuSoLuong = new ArrayList<>();

		// Tỷ lệ đạt theo học phần
		private List<String> hocPhanNhan = new ArrayList<>();
		private List<Double> hocPhanTyLeDat = new ArrayList<>();
		private List<Long> hocPhanSoLuot = new ArrayList<>();

		// Học phí theo học kỳ (triệu đồng)
		private List<String> hocPhiNhan = new ArrayList<>();
		private List<BigDecimal> hocPhiPhaiThu = new ArrayList<>();
		private List<BigDecimal> hocPhiDaThu = new ArrayList<>();

		public BigDecimal getTongConNo() {
			return tongPhaiThu.subtract(tongDaThu).max(BigDecimal.ZERO);
		}
	}

	public BaoCao tongQuan() {
		BaoCao bc = new BaoCao();
		demTongQuan(bc);
		sinhVienTheoKhoa(bc);
		xepLoaiHocLuc(bc);
		phanBoDiemChu(bc);
		tyLeDatTheoHocPhan(bc);
		hocPhiTheoHocKy(bc);
		return bc;
	}

	// ===================== THẺ TỔNG QUAN =====================

	private void demTongQuan(BaoCao bc) {
		bc.setTongSinhVien(em.createQuery("SELECT COUNT(sv) FROM SinhVien sv", Long.class).getSingleResult());
		bc.setTongGiangVien(em.createQuery("SELECT COUNT(gv) FROM GiangVien gv", Long.class).getSingleResult());

		// Lớp đang mở đăng ký hoặc đang học của học kỳ đang diễn ra
		bc.setSoLopHocKyNay(em.createQuery("""
				SELECT COUNT(lop) FROM LopHocPhan lop
				WHERE lop.hocKy.trangThai = 1 AND lop.trangThai IN (1, 2)
				""", Long.class).getSingleResult());
	}

	// ===================== SINH VIÊN THEO KHOA =====================

	private void sinhVienTheoKhoa(BaoCao bc) {
		List<Object[]> rows = em.createQuery("""
				SELECT d.tenDonVi, COUNT(sv)
				FROM SinhVien sv JOIN sv.nganh n JOIN n.donVi d
				GROUP BY d.tenDonVi
				ORDER BY COUNT(sv) DESC
				""", Object[].class).getResultList();
		for (Object[] r : rows) {
			bc.getKhoaNhan().add((String) r[0]);
			bc.getKhoaSoLuong().add((Long) r[1]);
		}
	}

	// ===================== XẾP LOẠI HỌC LỰC =====================

	/** GPA tích lũy của từng sinh viên = Σ(hệ 4 × tín chỉ) / Σ tín chỉ, chỉ tính điểm đã công bố */
	private void xepLoaiHocLuc(BaoCao bc) {
		List<Object[]> rows = em.createQuery("""
				SELECT dk.sinhVien.maSinhVien, SUM(kq.diemHe4 * hp.soTinChi), SUM(hp.soTinChi)
				FROM KetQuaHocPhan kq
				JOIN kq.dangKyHocPhan dk
				JOIN dk.lopHocPhan lop
				JOIN lop.hocPhan hp
				WHERE kq.trangThai = 3 AND kq.diemHe4 IS NOT NULL
				GROUP BY dk.sinhVien.maSinhVien
				""", Object[].class).getResultList();

		long[] dem = new long[5];
		for (Object[] r : rows) {
			BigDecimal tongDiem = (BigDecimal) r[1];
			long tongTc = ((Number) r[2]).longValue();
			if (tongTc == 0) continue;
			double gpa = tongDiem.divide(BigDecimal.valueOf(tongTc), 2, RoundingMode.HALF_UP).doubleValue();

			if      (gpa >= 3.6) dem[0]++;
			else if (gpa >= 3.2) dem[1]++;
			else if (gpa >= 2.5) dem[2]++;
			else if (gpa >= 2.0) dem[3]++;
			else                 dem[4]++;
		}
		for (long d : dem) bc.getXepLoaiSoLuong().add(d);
	}

	// ===================== PHÂN BỐ ĐIỂM CHỮ =====================

	private void phanBoDiemChu(BaoCao bc) {
		List<Object[]> rows = em.createQuery("""
				SELECT kq.diemChu, COUNT(kq)
				FROM KetQuaHocPhan kq
				WHERE kq.trangThai = 3 AND kq.diemChu IS NOT NULL
				GROUP BY kq.diemChu
				""", Object[].class).getResultList();

		Map<String, Long> theoChu = new LinkedHashMap<>();
		for (Object[] r : rows) theoChu.put((String) r[0], (Long) r[1]);
		// Giữ đúng thứ tự A → F, điểm nào không có thì là 0
		for (String chu : bc.getDiemChuNhan()) bc.getDiemChuSoLuong().add(theoChu.getOrDefault(chu, 0L));
	}

	// ===================== TỶ LỆ ĐẠT THEO HỌC PHẦN =====================

	private void tyLeDatTheoHocPhan(BaoCao bc) {
		List<Object[]> rows = em.createQuery("""
				SELECT hp.tenHocPhan, COUNT(kq), SUM(CASE WHEN kq.ketQua = 1 THEN 1 ELSE 0 END)
				FROM KetQuaHocPhan kq
				JOIN kq.dangKyHocPhan dk
				JOIN dk.lopHocPhan lop
				JOIN lop.hocPhan hp
				WHERE kq.trangThai = 3 AND kq.ketQua IS NOT NULL
				GROUP BY hp.tenHocPhan
				ORDER BY hp.tenHocPhan
				""", Object[].class).getResultList();

		for (Object[] r : rows) {
			long tong = (Long) r[1];
			long dat = ((Number) r[2]).longValue();
			bc.getHocPhanNhan().add((String) r[0]);
			bc.getHocPhanSoLuot().add(tong);
			bc.getHocPhanTyLeDat().add(tong == 0 ? 0 : Math.round(dat * 1000.0 / tong) / 10.0);
		}
	}

	// ===================== HỌC PHÍ THEO HỌC KỲ =====================

	private void hocPhiTheoHocKy(BaoCao bc) {
		List<Object[]> rows = em.createQuery("""
				SELECT hk.maHocKyCode, SUM(cn.soTienPhaiDong - cn.soTienMienGiam), SUM(cn.soTienDaDong)
				FROM CongNoHocPhi cn JOIN cn.hocKy hk
				WHERE cn.trangThai <> 3
				GROUP BY hk.maHocKyCode, hk.ngayBatDau
				ORDER BY hk.ngayBatDau
				""", Object[].class).getResultList();

		for (Object[] r : rows) {
			BigDecimal phaiThu = (BigDecimal) r[1];
			BigDecimal daThu = (BigDecimal) r[2];
			bc.setTongPhaiThu(bc.getTongPhaiThu().add(phaiThu));
			bc.setTongDaThu(bc.getTongDaThu().add(daThu));

			bc.getHocPhiNhan().add((String) r[0]);
			bc.getHocPhiPhaiThu().add(phaiThu.divide(MOT_TRIEU, 2, RoundingMode.HALF_UP));
			bc.getHocPhiDaThu().add(daThu.divide(MOT_TRIEU, 2, RoundingMode.HALF_UP));
		}
	}
}
