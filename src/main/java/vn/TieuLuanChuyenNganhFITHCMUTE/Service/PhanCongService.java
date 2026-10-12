package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiangVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;

/** Phòng Đào tạo xem và đổi giảng viên phụ trách từng lớp học phần */
@Service
@RequiredArgsConstructor
public class PhanCongService {

	private static final int VAI_TRO_CHINH = 1;
	private static final int HIEU_LUC = 1;
	private static final int DA_HUY = 0;

	private final LopHocPhanRepository lopRepository;
	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongRepository;
	private final LichHocRepository lichHocRepository;

	/** Một dòng trong bảng phân công */
	@Getter
	@AllArgsConstructor
	public static class DongPhanCong {
		private LopHocPhan lop;
		private GiangVien giangVien;   // null nghĩa là chưa phân công
		private List<LichHoc> lich;
	}

	// ===================== XEM =====================

	@Transactional(readOnly = true)
	public List<DongPhanCong> dsLop(Integer maHocKy, boolean chiChuaPhanCong) {
		List<LopHocPhan> dsLop = lopRepository.findByHocKy_MaHocKy(
				maHocKy, Sort.by("hocPhan.maHocPhan", "maLop"));

		// Giảng viên chính của từng lớp
		Map<Integer, GiangVien> gvCuaLop = new HashMap<>();
		for (PhanCongGiangDay pc : phanCongRepository.findByLopHocPhan_HocKy_MaHocKyAndTrangThai(maHocKy, HIEU_LUC)) {
			if (pc.getVaiTro() == VAI_TRO_CHINH)
				gvCuaLop.put(pc.getLopHocPhan().getMaLopHocPhan(), pc.getGiangVien());
		}

		// Lịch của từng lớp
		Map<Integer, List<LichHoc>> lichCuaLop = new HashMap<>();
		if (!dsLop.isEmpty()) {
			for (LichHoc lh : lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(
					dsLop.stream().map(LopHocPhan::getMaLopHocPhan).toList())) {
				lichCuaLop.computeIfAbsent(lh.getLopHocPhan().getMaLopHocPhan(), k -> new ArrayList<>()).add(lh);
			}
		}

		List<DongPhanCong> kq = new ArrayList<>();
		for (LopHocPhan lop : dsLop) {
			GiangVien gv = gvCuaLop.get(lop.getMaLopHocPhan());
			if (chiChuaPhanCong && gv != null) continue;
			kq.add(new DongPhanCong(lop, gv, lichCuaLop.getOrDefault(lop.getMaLopHocPhan(), List.of())));
		}
		return kq;
	}

	/** Giảng viên của khoa phụ trách học phần, để gợi ý trước trong ô chọn */
	@Transactional(readOnly = true)
	public List<GiangVien> dsGiangVien(Integer maDonVi) {
		List<GiangVien> cungKhoa = maDonVi == null ? List.of()
				: giangVienRepository.findByDonVi_MaDonViAndTrangThaiOrderByMaSoGiangVien(maDonVi, 1);
		if (!cungKhoa.isEmpty()) return cungKhoa;
		return giangVienRepository.findAll(Sort.by("maSoGiangVien"));
	}

	// ===================== ĐỔI GIẢNG VIÊN =====================

	@Transactional
	public void doiGiangVien(Integer maLop, Integer maGiangVien) {
		LopHocPhan lop = lopRepository.findById(maLop)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
		GiangVien gv = giangVienRepository.findById(maGiangVien)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy giảng viên."));

		if (gv.getTrangThai() != null && gv.getTrangThai() != 1)
			throw new IllegalStateException("Giảng viên này không còn công tác.");
		if (lop.getTrangThai() == 4)
			throw new IllegalStateException("Lớp đã hủy, không phân công được.");

		kiemTraTrungGio(lop, gv);

		// Gỡ phân công cũ rồi ghi phân công mới, giữ lịch sử bằng trạng thái đã hủy
		List<PhanCongGiangDay> cu = phanCongRepository
				.findByLopHocPhan_MaLopHocPhanAndVaiTroAndTrangThai(maLop, VAI_TRO_CHINH, HIEU_LUC);
		for (PhanCongGiangDay pc : cu) {
			if (pc.getGiangVien().getMaGiangVien().equals(maGiangVien))
				throw new IllegalStateException("Giảng viên này đang phụ trách lớp rồi.");
			pc.setTrangThai(DA_HUY);
			pc.setGhiChu("Phòng Đào tạo đổi sang " + gv.getMaSoGiangVien());
			phanCongRepository.save(pc);
		}

		PhanCongGiangDay moi = new PhanCongGiangDay();
		moi.setLopHocPhan(lop);
		moi.setGiangVien(gv);
		moi.setVaiTro(VAI_TRO_CHINH);
		moi.setTrangThai(HIEU_LUC);
		phanCongRepository.save(moi);
	}

	/** Giảng viên không được dạy hai lớp cùng giờ trong một học kỳ */
	private void kiemTraTrungGio(LopHocPhan lop, GiangVien gv) {
		List<LichHoc> lichLopMoi = lichHocRepository
				.findByLopHocPhan_MaLopHocPhanIn(List.of(lop.getMaLopHocPhan()));
		if (lichLopMoi.isEmpty()) return;

		Integer maHocKy = lop.getHocKy().getMaHocKy();
		for (PhanCongGiangDay pc : phanCongRepository
				.findByLopHocPhan_HocKy_MaHocKyAndTrangThai(maHocKy, HIEU_LUC)) {

			if (!pc.getGiangVien().getMaGiangVien().equals(gv.getMaGiangVien())) continue;
			if (pc.getLopHocPhan().getMaLopHocPhan().equals(lop.getMaLopHocPhan())) continue;
			if (pc.getLopHocPhan().getTrangThai() == 4) continue;

			for (LichHoc a : lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(
					List.of(pc.getLopHocPhan().getMaLopHocPhan()))) {
				for (LichHoc b : lichLopMoi) {
					if (a.trungGio(b))
						throw new IllegalStateException(gv.getTenHienThi() + " đã dạy lớp "
								+ pc.getLopHocPhan().getMaLop() + " vào " + a.getTenThu()
								+ ", tiết " + a.getTietBatDau() + "–" + a.getTietKetThuc() + ".");
				}
			}
		}
	}
}