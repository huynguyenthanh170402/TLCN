package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;

/**
 * Kết quả học tập của sinh viên.
 * Một học phần có thể học nhiều lần (học lại, học cải thiện); điểm được công nhận
 * là lần có điểm cao nhất, nên GPA tích lũy không cộng dồn các lần rớt.
 */
@Service
@RequiredArgsConstructor
public class KetQuaHocTapService {

	public static final int SO_DONG_MOI_TRANG = 10;

	private final KetQuaHocPhanRepository ketQuaRepository;

	/** Thống kê tích lũy hiện trên các thẻ */
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

	/** Một dòng điểm: kết quả của một lần học, kèm số thứ tự lần học */
	@Getter
	@AllArgsConstructor
	public static class DongDiem {
		private KetQuaHocPhan ketQua;
		private int lanHoc;            // 1, 2, 3...
		private boolean duocCongNhan;  // lần điểm cao nhất của học phần

		public HocPhan getHocPhan() {
			return ketQua.getDangKyHocPhan().getLopHocPhan().getHocPhan();
		}
		public String getGhiChu() {
			if (lanHoc > 1 && duocCongNhan) return "Học lại lần " + lanHoc + ", điểm được công nhận";
			if (lanHoc > 1) return "Học lại lần " + lanHoc;
			if (!duocCongNhan) return "Đã học lại, dùng điểm lần sau";
			return "";
		}
	}

	/** Nhóm điểm của một học kỳ */
	@Getter
	@AllArgsConstructor
	public static class NhomHocKy {
		private HocKy hocKy;
		private List<DongDiem> dsDiem;
		private BigDecimal gpaHocKy;
		private int tinChiDangKy;
		private int tinChiDat;
	}

	/** Một học phần với các lần học của nó */
	@Getter
	@AllArgsConstructor
	public static class TongHopHocPhan {
		private HocPhan hocPhan;
		private List<KetQuaHocPhan> cacLan;   // theo thứ tự thời gian
		private KetQuaHocPhan duocCongNhan;   // lần điểm cao nhất

		public int getSoLan() { return cacLan.size(); }
		public boolean isDat() {
			return duocCongNhan != null && Integer.valueOf(1).equals(duocCongNhan.getKetQua());
		}
		/** Điểm hệ 10 của lần thứ n, null nếu chưa học tới lần đó */
		public BigDecimal diemLan(int n) {
			return n <= cacLan.size() ? cacLan.get(n - 1).getDiemTongKet() : null;
		}
	}

	// ===================== DỮ LIỆU GỐC =====================

	public List<KetQuaHocPhan> tatCa(Integer maSv) {
		return ketQuaRepository.timTatCaKetQuaCongBo(maSv);
	}

	public Page<KetQuaHocPhan> phanTrang(Integer maSv, Integer maHocKy, int trang) {
		return ketQuaRepository.timKetQuaCongBo(maSv, maHocKy,
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG));
	}

	// ===================== TỔNG HỢP THEO HỌC PHẦN =====================

	/**
	 * Gom các lần học của cùng một học phần lại, sắp theo thời gian,
	 * rồi đánh dấu lần có điểm cao nhất là lần được công nhận.
	 */
	public List<TongHopHocPhan> tongHopTheoHocPhan(List<KetQuaHocPhan> ds) {
		Map<Integer, List<KetQuaHocPhan>> theoHocPhan = new LinkedHashMap<>();
		ds.stream()
			.sorted(Comparator.comparing(kq -> ngayBatDau(kq)))
			.forEach(kq -> theoHocPhan
					.computeIfAbsent(hocPhan(kq).getMaHocPhanId(), k -> new ArrayList<>())
					.add(kq));

		List<TongHopHocPhan> kq = new ArrayList<>();
		for (List<KetQuaHocPhan> cacLan : theoHocPhan.values()) {
			kq.add(new TongHopHocPhan(hocPhan(cacLan.get(0)), cacLan, lanCaoNhat(cacLan)));
		}
		kq.sort(Comparator.comparing(t -> t.getHocPhan().getMaHocPhan()));
		return kq;
	}

	/** Lần có điểm tổng kết cao nhất; bằng điểm thì lấy lần sau cùng */
	private KetQuaHocPhan lanCaoNhat(List<KetQuaHocPhan> cacLan) {
		KetQuaHocPhan tot = null;
		for (KetQuaHocPhan kq : cacLan) {
			if (kq.getDiemTongKet() == null) continue;
			if (tot == null || kq.getDiemTongKet().compareTo(tot.getDiemTongKet()) >= 0) tot = kq;
		}
		return tot != null ? tot : cacLan.get(cacLan.size() - 1);
	}

	// ===================== NHÓM THEO HỌC KỲ =====================

	public List<NhomHocKy> theoHocKy(List<KetQuaHocPhan> ds) {
		List<TongHopHocPhan> tongHop = tongHopTheoHocPhan(ds);

		// Tra nhanh: mã kết quả nào là lần được công nhận, lần thứ mấy
		Map<Integer, Integer> lanThu = new LinkedHashMap<>();
		Map<Integer, Boolean> congNhan = new LinkedHashMap<>();
		for (TongHopHocPhan t : tongHop) {
			for (int i = 0; i < t.getCacLan().size(); i++) {
				KetQuaHocPhan kq = t.getCacLan().get(i);
				lanThu.put(kq.getMaKetQuaHocPhan(), i + 1);
				congNhan.put(kq.getMaKetQuaHocPhan(),
						t.getDuocCongNhan() != null
						&& t.getDuocCongNhan().getMaKetQuaHocPhan().equals(kq.getMaKetQuaHocPhan()));
			}
		}

		// Gom theo học kỳ, học kỳ mới nhất trước
		Map<Integer, List<KetQuaHocPhan>> theoKy = new LinkedHashMap<>();
		ds.stream()
			.sorted(Comparator.comparing((KetQuaHocPhan kq) -> ngayBatDau(kq)).reversed()
					.thenComparing(kq -> hocPhan(kq).getMaHocPhan()))
			.forEach(kq -> theoKy.computeIfAbsent(hocKy(kq).getMaHocKy(), k -> new ArrayList<>()).add(kq));

		List<NhomHocKy> kqNhom = new ArrayList<>();
		for (List<KetQuaHocPhan> trongKy : theoKy.values()) {
			List<DongDiem> dong = trongKy.stream()
					.map(kq -> new DongDiem(kq,
							lanThu.getOrDefault(kq.getMaKetQuaHocPhan(), 1),
							Boolean.TRUE.equals(congNhan.get(kq.getMaKetQuaHocPhan()))))
					.toList();

			BigDecimal tongDiem = BigDecimal.ZERO;
			int tcCoDiem = 0, tcDangKy = 0, tcDat = 0;
			for (KetQuaHocPhan kq : trongKy) {
				int tc = hocPhan(kq).getSoTinChi();
				tcDangKy += tc;
				if (kq.getDiemHe4() != null) {
					tongDiem = tongDiem.add(kq.getDiemHe4().multiply(BigDecimal.valueOf(tc)));
					tcCoDiem += tc;
				}
				if (Integer.valueOf(1).equals(kq.getKetQua())) tcDat += tc;
			}
			BigDecimal gpa = tcCoDiem == 0 ? BigDecimal.ZERO
					: tongDiem.divide(BigDecimal.valueOf(tcCoDiem), 2, RoundingMode.HALF_UP);

			kqNhom.add(new NhomHocKy(hocKy(trongKy.get(0)), dong, gpa, tcDangKy, tcDat));
		}
		return kqNhom;
	}

	// ===================== TÍCH LŨY =====================

	/**
	 * GPA tích lũy tính trên điểm được công nhận của mỗi học phần,
	 * nên môn học lại chỉ được tính một lần.
	 */
	public TongKet tinhTongKet(List<KetQuaHocPhan> ds) {
		List<TongHopHocPhan> tongHop = tongHopTheoHocPhan(ds);

		BigDecimal tongDiem = BigDecimal.ZERO;
		int tinChiDaHoc = 0, tinChiTichLuy = 0, soMonDat = 0;

		for (TongHopHocPhan t : tongHop) {
			KetQuaHocPhan kq = t.getDuocCongNhan();
			if (kq == null) continue;
			int tc = t.getHocPhan().getSoTinChi();

			if (kq.getDiemHe4() != null) {
				tongDiem = tongDiem.add(kq.getDiemHe4().multiply(BigDecimal.valueOf(tc)));
				tinChiDaHoc += tc;
			}
			if (Integer.valueOf(1).equals(kq.getKetQua())) {
				tinChiTichLuy += tc;
				soMonDat++;
			}
		}

		BigDecimal gpa = tinChiDaHoc == 0 ? BigDecimal.ZERO
				: tongDiem.divide(BigDecimal.valueOf(tinChiDaHoc), 2, RoundingMode.HALF_UP);

		return new TongKet(gpa, tinChiDaHoc, tinChiTichLuy, tongHop.size(), soMonDat, xepLoai(gpa));
	}

	/** Các học kỳ có điểm, mới nhất trước — dùng cho ô lọc */
	public List<HocKy> dsHocKy(List<KetQuaHocPhan> ds) {
		return ds.stream()
				.map(this::hocKy)
				.distinct()
				.sorted(Comparator.comparing(HocKy::getNgayBatDau).reversed())
				.toList();
	}

	public String xepLoai(BigDecimal gpa) {
		double g = gpa.doubleValue();
		if (g >= 3.6) return "Xuất sắc";
		if (g >= 3.2) return "Giỏi";
		if (g >= 2.5) return "Khá";
		if (g >= 2.0) return "Trung bình";
		return "Yếu";
	}

	// ===================== TIỆN ÍCH =====================

	private HocPhan hocPhan(KetQuaHocPhan kq) {
		return kq.getDangKyHocPhan().getLopHocPhan().getHocPhan();
	}

	private HocKy hocKy(KetQuaHocPhan kq) {
		return kq.getDangKyHocPhan().getLopHocPhan().getHocKy();
	}

	private java.time.LocalDate ngayBatDau(KetQuaHocPhan kq) {
		return hocKy(kq).getNgayBatDau();
	}
}