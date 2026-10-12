package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiangVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhongHocRepository;

/**
 * Mở lớp chính khóa hàng loạt theo kế hoạch đào tạo:
 * với mỗi khóa đang học của một ngành, lấy các học phần đúng học kỳ trong
 * chương trình, chia lớp theo sĩ số rồi tự xếp phòng và lịch.
 */
@Service
@RequiredArgsConstructor
public class MoLopHangLoatService {

	public static final int SI_SO_MOI_LOP = 50;
	private static final int SI_SO_TOI_THIEU = 15;
	private static final int SO_TIET_MOI_BUOI = 3;
	/** Các ca học trong ngày: tiết bắt đầu */
	private static final int[] CA_HOC = {1, 4, 7, 10};
	private static final int LOP_CHINH_KHOA = 1;

	private final EntityManager em;
	private final HocKyRepository hocKyRepository;
	private final NganhRepository nganhRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;
	private final LopHocPhanRepository lopRepository;
	private final PhongHocRepository phongHocRepository;
	private final LichHocRepository lichHocRepository;
	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongRepository;

	/** Một dòng trong bảng kế hoạch mở lớp */
	@Getter
	@AllArgsConstructor
	public static class DongKeHoach {
		private String khoa;
		private int hocKyThu;
		private HocPhan hocPhan;
		private long soSinhVien;
		private int soLopCan;
		private int soLopDaCo;

		public int getSoLopThieu() { return Math.max(0, soLopCan - soLopDaCo); }
	}

	@Getter
	@AllArgsConstructor
	public static class KetQuaMoLop {
		private int soLopDaTao;
		private List<String> canhBao;
	}

	// ===================== LẬP KẾ HOẠCH =====================

	@Transactional(readOnly = true)
	public List<DongKeHoach> keHoach(Integer maHocKy, Integer maNganh) {
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));
		Nganh nganh = nganhRepository.findById(maNganh)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy ngành."));

		List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				maNganh, 1, Sort.by("hocKyDuKien", "hocPhan.maHocPhan"));
		if (khung.isEmpty())
			throw new IllegalStateException("Ngành " + nganh.getTenNganh() + " chưa có chương trình đào tạo.");

		List<DongKeHoach> kq = new ArrayList<>();

		// Các khóa đang có sinh viên của ngành này
		List<Object[]> dsKhoa = em.createQuery("""
				SELECT sv.khoaTuyenSinh, COUNT(sv) FROM SinhVien sv
				WHERE sv.nganh.maNganh = :maNganh AND sv.trangThai = 1
				  AND sv.khoaTuyenSinh IS NOT NULL
				GROUP BY sv.khoaTuyenSinh ORDER BY sv.khoaTuyenSinh DESC
				""", Object[].class)
				.setParameter("maNganh", maNganh)
				.getResultList();

		for (Object[] r : dsKhoa) {
			String khoa = (String) r[0];
			long soSv = (Long) r[1];
			int hocKyThu = hocKyThuCuaKhoa(khoa, hk);
			if (hocKyThu < 1 || hocKyThu > 8) continue;   // đã ra trường hoặc chưa nhập học

			int soLopCan = (int) Math.ceil(soSv / (double) SI_SO_MOI_LOP);

			for (ChuongTrinhDaoTao ct : khung) {
				if (ct.getHocKyDuKien() != hocKyThu) continue;

				long daCo = lopRepository.countByHocKy_MaHocKyAndHocPhan_MaHocPhanIdAndKhoaApDung(
						maHocKy, ct.getHocPhan().getMaHocPhanId(), khoa);
				kq.add(new DongKeHoach(khoa, hocKyThu, ct.getHocPhan(), soSv, soLopCan, (int) daCo));
			}
		}
		return kq;
	}

	/** Khóa 2022, học kỳ 1 năm học 2026-2027 → học kỳ thứ 9 */
	private int hocKyThuCuaKhoa(String khoa, HocKy hk) {
		try {
			int namKhoa = Integer.parseInt(khoa.trim());
			int namBatDau = Integer.parseInt(hk.getNamHoc().substring(0, 4));
			int hocKySo = hk.getHocKySo() == null ? 1 : hk.getHocKySo();
			return (namBatDau - namKhoa) * 2 + hocKySo;
		} catch (Exception e) {
			return -1;
		}
	}

	// ===================== THỰC HIỆN =====================

	@Transactional
	public KetQuaMoLop moLop(Integer maHocKy, Integer maNganh, boolean moDangKyNgay) {
		HocKy hk = hocKyRepository.findById(maHocKy).orElseThrow();
		Nganh nganh = nganhRepository.findById(maNganh).orElseThrow();
		if (hk.getTrangThai() == 2)
			throw new IllegalStateException("Học kỳ đã kết thúc, không mở lớp được.");

		List<DongKeHoach> keHoach = keHoach(maHocKy, maNganh);
		List<PhongHoc> dsPhong = phongHocRepository.findByTrangThaiOrderByMaPhong(1);
		if (dsPhong.isEmpty())
			throw new IllegalStateException("Chưa có phòng học nào đang sử dụng.");

		// Lịch đã xếp trong học kỳ này, để tránh trùng phòng
		Set<String> phongDaDung = new HashSet<>();
		for (LichHoc lh : lichHocRepository.findByLopHocPhan_HocKy_MaHocKy(maHocKy)) {
			if (lh.getPhongHoc() == null) continue;
			for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++)
				phongDaDung.add(lh.getPhongHoc().getMaPhongHoc() + "-" + lh.getThu() + "-" + t);
		}
		// Lịch của từng khóa trong ngành, để sinh viên cùng khóa không bị trùng giờ
		Set<String> khoaDaBan = new HashSet<>();

		// Giờ bận và số lớp đang dạy của từng giảng viên trong học kỳ này
		Set<String> gvBanGio = new HashSet<>();
		Map<Integer, Integer> soLopCuaGv = new HashMap<>();
		for (PhanCongGiangDay pc : phanCongRepository.findByLopHocPhan_HocKy_MaHocKyAndTrangThai(maHocKy, 1)) {
			Integer maGv = pc.getGiangVien().getMaGiangVien();
			soLopCuaGv.merge(maGv, 1, Integer::sum);
			for (LichHoc lh : lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(
					List.of(pc.getLopHocPhan().getMaLopHocPhan()))) {
				for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++)
					gvBanGio.add(maGv + "-" + lh.getThu() + "-" + t);
			}
		}

		List<String> canhBao = new ArrayList<>();
		int soLopDaTao = 0;

		for (DongKeHoach d : keHoach) {
			for (int nhom = d.getSoLopDaCo() + 1; nhom <= d.getSoLopCan(); nhom++) {
				String maLop = d.getHocPhan().getMaHocPhan() + "_" + hk.getMaHocKyCode()
						+ "_" + d.getKhoa() + "_" + String.format("%02d", nhom);
				if (lopRepository.existsByMaLop(maLop)) continue;

				int[] slot = timSlot(dsPhong, phongDaDung, khoaDaBan, d.getKhoa());
				if (slot == null) {
					canhBao.add("Hết phòng trống cho " + d.getHocPhan().getTenHocPhan()
							+ " khóa " + d.getKhoa() + ".");
					continue;
				}
				int thu = slot[0], tietBatDau = slot[1];
				PhongHoc phong = dsPhong.get(slot[2]);

				LopHocPhan lop = new LopHocPhan();
				lop.setMaLop(maLop);
				lop.setHocPhan(d.getHocPhan());
				lop.setHocKy(hk);
				lop.setNganh(nganh);
				lop.setKhoaApDung(d.getKhoa());
				lop.setLoaiLop(LOP_CHINH_KHOA);
				lop.setSiSoToiDa(SI_SO_MOI_LOP);
				lop.setSiSoToiThieu(SI_SO_TOI_THIEU);
				lop.setTrangThai(moDangKyNgay ? 1 : 0);
				lop.setGhiChu("Mở theo kế hoạch đào tạo, học kỳ thứ " + d.getHocKyThu());
				lop = lopRepository.save(lop);

				LichHoc lh = new LichHoc();
				lh.setLopHocPhan(lop);
				lh.setThu(thu);
				lh.setTietBatDau(tietBatDau);
				lh.setSoTiet(SO_TIET_MOI_BUOI);
				lh.setPhongHoc(phong);
				lh.setNgayBatDau(hk.getNgayBatDau());
				lh.setNgayKetThuc(hk.getNgayKetThuc());
				lichHocRepository.save(lh);

				// Phân công giảng viên chính: cùng khoa phụ trách học phần, rảnh giờ này, ít lớp nhất
				GiangVien gv = chonGiangVien(d.getHocPhan().getDonVi().getMaDonVi(),
						thu, tietBatDau, gvBanGio, soLopCuaGv);
				if (gv == null) {
					canhBao.add("Chưa có giảng viên rảnh cho lớp " + maLop + ".");
				} else {
					PhanCongGiangDay pc = new PhanCongGiangDay();
					pc.setLopHocPhan(lop);
					pc.setGiangVien(gv);
					pc.setVaiTro(1);
					pc.setTrangThai(1);
					phanCongRepository.save(pc);

					soLopCuaGv.merge(gv.getMaGiangVien(), 1, Integer::sum);
					for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++)
						gvBanGio.add(gv.getMaGiangVien() + "-" + thu + "-" + t);
				}

				// Đánh dấu slot đã dùng
				for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++) {
					phongDaDung.add(phong.getMaPhongHoc() + "-" + thu + "-" + t);
					khoaDaBan.add(d.getKhoa() + "-" + thu + "-" + t);
				}
				soLopDaTao++;
			}
		}
		return new KetQuaMoLop(soLopDaTao, canhBao);
	}

	/**
	 * Chọn giảng viên cho một lớp: ưu tiên người của khoa phụ trách học phần,
	 * không trùng giờ dạy, đang dạy ít lớp nhất. Hết người trong khoa thì lấy khoa khác.
	 */
	private GiangVien chonGiangVien(Integer maDonVi, int thu, int tietBatDau,
	                                Set<String> gvBanGio, Map<Integer, Integer> soLopCuaGv) {
		List<GiangVien> ungVien =
				giangVienRepository.findByDonVi_MaDonViAndTrangThaiOrderByMaSoGiangVien(maDonVi, 1);
		GiangVien chon = timGiangVienRanh(ungVien, thu, tietBatDau, gvBanGio, soLopCuaGv);
		if (chon != null) return chon;

		// Khoa hết người rảnh: mượn giảng viên khoa khác
		return timGiangVienRanh(giangVienRepository.findAll(), thu, tietBatDau, gvBanGio, soLopCuaGv);
	}

	private GiangVien timGiangVienRanh(List<GiangVien> ungVien, int thu, int tietBatDau,
	                                   Set<String> gvBanGio, Map<Integer, Integer> soLopCuaGv) {
		GiangVien chon = null;
		int itNhat = Integer.MAX_VALUE;

		for (GiangVien gv : ungVien) {
			if (gv.getTrangThai() != null && gv.getTrangThai() != 1) continue;

			boolean ban = false;
			for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++)
				if (gvBanGio.contains(gv.getMaGiangVien() + "-" + thu + "-" + t)) { ban = true; break; }
			if (ban) continue;

			int soLop = soLopCuaGv.getOrDefault(gv.getMaGiangVien(), 0);
			if (soLop < itNhat) { itNhat = soLop; chon = gv; }
		}
		return chon;
	}

	/**
	 * Tìm chỗ trống đầu tiên: thứ 2 đến thứ 7, bốn ca mỗi ngày, phòng còn rảnh
	 * và khóa đó chưa có lớp nào vào giờ ấy.
	 * @return {thứ, tiết bắt đầu, chỉ số phòng} hoặc null nếu hết chỗ
	 */
	private int[] timSlot(List<PhongHoc> dsPhong, Set<String> phongDaDung,
	                      Set<String> khoaDaBan, String khoa) {
		for (int thu = 2; thu <= 7; thu++) {
			for (int tiet : CA_HOC) {
				// Khóa này đã có lớp vào giờ đó chưa
				boolean khoaBan = false;
				for (int t = tiet; t < tiet + SO_TIET_MOI_BUOI; t++)
					if (khoaDaBan.contains(khoa + "-" + thu + "-" + t)) { khoaBan = true; break; }
				if (khoaBan) continue;

				for (int i = 0; i < dsPhong.size(); i++) {
					PhongHoc p = dsPhong.get(i);
					if (p.getSucChua() < SI_SO_MOI_LOP) continue;

					boolean ban = false;
					for (int t = tiet; t < tiet + SO_TIET_MOI_BUOI; t++)
						if (phongDaDung.contains(p.getMaPhongHoc() + "-" + thu + "-" + t)) { ban = true; break; }
					if (!ban) return new int[]{thu, tiet, i};
				}
			}
		}
		return null;
	}
}