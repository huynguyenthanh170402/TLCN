package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;

/** Sinh viên xem chương trình đào tạo ngành mình, kèm tiến độ từng môn */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChuongTrinhSinhVienService {

	private static final int DIEM_DA_CONG_BO = 3;
	private static final int DA_DANG_KY = 1;

	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;
	private final KetQuaHocPhanRepository ketQuaRepository;
	private final DangKyHocPhanRepository dangKyRepository;

	/** Một môn trong chương trình, kèm tình trạng học của sinh viên */
	@Getter
	@AllArgsConstructor
	public static class DongChuongTrinh {
		private ChuongTrinhDaoTao chuongTrinh;
		/** "Đã đạt", "Chưa đạt", "Đang học", "Chưa học" */
		private String trangThai;
		private BigDecimal diemTongKet;
		private String diemChu;

		public String getMauBadge() {
			return switch (trangThai) {
				case "Đã đạt"   -> "bg-success";
				case "Chưa đạt" -> "bg-danger";
				case "Đang học" -> "bg-primary";
				default -> "bg-light text-dark border";
			};
		}
	}

	/** Tiến độ tích lũy so với chương trình */
	@Getter
	@AllArgsConstructor
	public static class TienDo {
		private int tongTinChi;
		private int tinChiDat;
		private int soMonTong;
		private int soMonDat;
		private int soMonDangHoc;

		public int getPhanTram() {
			return tongTinChi == 0 ? 0 : (int) Math.round(tinChiDat * 100.0 / tongTinChi);
		}
		public int getTinChiConLai() {
			return Math.max(0, tongTinChi - tinChiDat);
		}
		public int getSoMonChuaHoc() {
			return Math.max(0, soMonTong - soMonDat - soMonDangHoc);
		}
	}

	public List<DongChuongTrinh> chuongTrinhCuaSinhVien(SinhVien sv) {
		if (sv == null || sv.getNganh() == null) return List.of();

		List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				sv.getNganh().getMaNganh(), 1, Sort.by("hocKyDuKien", "hocPhan.maHocPhan"));

		// Điểm đã công bố: mã học phần → kết quả (giữ lần đạt nếu có)
		Map<Integer, KetQuaHocPhan> theoHocPhan = new HashMap<>();
		for (KetQuaHocPhan kq : ketQuaRepository.timTatCaKetQuaCongBo(sv.getMaSinhVien())) {
			Integer maHp = kq.getDangKyHocPhan().getLopHocPhan().getHocPhan().getMaHocPhanId();
			KetQuaHocPhan cu = theoHocPhan.get(maHp);
			boolean totHon = cu == null
					|| Integer.valueOf(1).equals(kq.getKetQua())
					|| (cu.getDiemTongKet() != null && kq.getDiemTongKet() != null
					    && kq.getDiemTongKet().compareTo(cu.getDiemTongKet()) > 0);
			if (totHon) theoHocPhan.put(maHp, kq);
		}

		// Các môn đang đăng ký, chưa có điểm công bố
		Set<Integer> dangHoc = new HashSet<>();
		for (DangKyHocPhan dk : dangKyRepository.findBySinhVien_MaSinhVienAndTrangThai(
				sv.getMaSinhVien(), DA_DANG_KY)) {
			dangHoc.add(dk.getLopHocPhan().getHocPhan().getMaHocPhanId());
		}

		return khung.stream().map(ct -> {
			Integer maHp = ct.getHocPhan().getMaHocPhanId();
			KetQuaHocPhan kq = theoHocPhan.get(maHp);

			if (kq != null) {
				boolean dat = Integer.valueOf(1).equals(kq.getKetQua());
				return new DongChuongTrinh(ct, dat ? "Đã đạt" : "Chưa đạt",
						kq.getDiemTongKet(), kq.getDiemChu());
			}
			if (dangHoc.contains(maHp))
				return new DongChuongTrinh(ct, "Đang học", null, null);
			return new DongChuongTrinh(ct, "Chưa học", null, null);
		}).toList();
	}

	public TienDo tienDo(List<DongChuongTrinh> ds) {
		int tongTc = 0, tcDat = 0, monDat = 0, monDangHoc = 0;
		for (DongChuongTrinh d : ds) {
			int tc = d.getChuongTrinh().getHocPhan().getSoTinChi();
			tongTc += tc;
			if ("Đã đạt".equals(d.getTrangThai())) { tcDat += tc; monDat++; }
			if ("Đang học".equals(d.getTrangThai())) monDangHoc++;
		}
		return new TienDo(tongTc, tcDat, ds.size(), monDat, monDangHoc);
	}
}