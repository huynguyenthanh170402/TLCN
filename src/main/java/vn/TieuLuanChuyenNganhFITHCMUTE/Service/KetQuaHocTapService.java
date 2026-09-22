package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;

@Service
@RequiredArgsConstructor
public class KetQuaHocTapService {

	public static final int SO_DONG_MOI_TRANG = 10;

	/** Chỉ hiện điểm đã công bố */
	private static final int DA_CONG_BO = 3;

	/** Học kỳ mới nhất lên đầu, trong cùng học kỳ xếp theo mã học phần */
	private static final Sort SAP_XEP = Sort.by(
			Sort.Order.desc("dangKyHocPhan.lopHocPhan.hocKy.ngayBatDau"),
			Sort.Order.asc("dangKyHocPhan.lopHocPhan.hocPhan.maHocPhan"));

	private final KetQuaHocPhanRepository ketQuaRepository;

	/** Kết quả tổng hợp để hiện trên các thẻ thống kê */
	@Getter
	@AllArgsConstructor
	public static class TongKet {
		private BigDecimal gpa;
		private int tinChiDaHoc;
		private int tinChiTichLuy;
		private int soMonDaHoc;
		private int soMonDat;
		private String xepLoai;
	}

	/** Bảng điểm có phân trang; maHocKy = null → tất cả học kỳ */
	public Page<KetQuaHocPhan> phanTrang(Integer maSv, Integer maHocKy, int trang) {
		Pageable pageable = PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG, SAP_XEP);

		if (maHocKy == null) {
			return ketQuaRepository
					.findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(maSv, DA_CONG_BO, pageable);
		}
		return ketQuaRepository
				.findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThaiAndDangKyHocPhan_LopHocPhan_HocKy_MaHocKy(
						maSv, DA_CONG_BO, maHocKy, pageable);
	}

	/** Toàn bộ kết quả đã công bố, không phân trang — dùng tính GPA */
	public List<KetQuaHocPhan> tatCa(Integer maSv) {
		return ketQuaRepository
				.findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(maSv, DA_CONG_BO, SAP_XEP);
	}

	/**
	 * GPA = Σ(điểm hệ 4 × tín chỉ) / Σ(tín chỉ)
	 * Tín chỉ tích lũy = tổng tín chỉ các môn đạt.
	 */
	public TongKet tinhTongKet(List<KetQuaHocPhan> ds) {
		BigDecimal tongDiem = BigDecimal.ZERO;
		int tinChiDaHoc = 0, tinChiTichLuy = 0, soMonDat = 0;

		for (KetQuaHocPhan kq : ds) {
			int tc = kq.getDangKyHocPhan().getLopHocPhan().getHocPhan().getSoTinChi();

			if (kq.getDiemHe4() != null) {
				tongDiem = tongDiem.add(kq.getDiemHe4().multiply(BigDecimal.valueOf(tc)));
				tinChiDaHoc += tc;
			}
			if (Integer.valueOf(1).equals(kq.getKetQua())) {
				tinChiTichLuy += tc;
				soMonDat++;
			}
		}

		BigDecimal gpa = tinChiDaHoc == 0
				? BigDecimal.ZERO
				: tongDiem.divide(BigDecimal.valueOf(tinChiDaHoc), 2, RoundingMode.HALF_UP);

		return new TongKet(gpa, tinChiDaHoc, tinChiTichLuy, ds.size(), soMonDat, xepLoai(gpa));
	}

	/** Các học kỳ sinh viên có kết quả, mới nhất trước — dùng cho ô lọc */
	public List<HocKy> dsHocKy(List<KetQuaHocPhan> ds) {
		return ds.stream()
				.map(kq -> kq.getDangKyHocPhan().getLopHocPhan().getHocKy())
				.distinct()
				.sorted(Comparator.comparing(HocKy::getNgayBatDau).reversed())
				.toList();
	}

	private String xepLoai(BigDecimal gpa) {
		double g = gpa.doubleValue();
		if (g >= 3.6) return "Xuất sắc";
		if (g >= 3.2) return "Giỏi";
		if (g >= 2.5) return "Khá";
		if (g >= 2.0) return "Trung bình";
		return "Yếu";
	}
}