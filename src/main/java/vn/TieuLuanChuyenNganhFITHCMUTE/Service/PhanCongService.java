package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

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
	private static final int DANG_CONG_TAC = 1;

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

	/** Một ô trên màn hình chọn khoa */
	@Getter
	public static class TomTatKhoa {
		private final Integer maDonVi;
		private final String ten;
		private int soLop;
		private int soChuaPhanCong;

		TomTatKhoa(Integer maDonVi, String ten) { this.maDonVi = maDonVi; this.ten = ten; }

		public boolean isXong() { return soLop > 0 && soChuaPhanCong == 0; }
	}

	// ===================== CHIA VIỆC THEO KHOA =====================

	/**
	 * Số liệu cho màn hình chọn khoa.
	 *
	 * Lớp thuộc về khoa phụ trách học phần, không phải khoa quản lý ngành:
	 * người đứng lớp là giảng viên của khoa dạy môn đó. Lớp Toán cao cấp mở
	 * cho sinh viên Cơ khí vẫn do khoa dạy Toán lo giảng viên.
	 *
	 * Dùng lại dsLop thay vì viết truy vấn đếm riêng, vì danh sách đó đằng
	 * nào cũng đã gom sẵn giảng viên của từng lớp — đúng thứ cần để biết lớp
	 * nào còn trống.
	 */
	@Transactional(readOnly = true)
	public List<TomTatKhoa> tomTatTheoKhoa(Integer maHocKy) {
		Map<Integer, TomTatKhoa> theoKhoa = new HashMap<>();

		for (DongPhanCong d : dsLopToanTruong(maHocKy, false)) {
			Integer ma = maKhoa(d.getLop());
			String ten = tenKhoaCuaLop(d.getLop());

			TomTatKhoa t = theoKhoa.computeIfAbsent(ma, k -> new TomTatKhoa(k, ten));
			t.soLop++;
			if (d.getGiangVien() == null) t.soChuaPhanCong++;
		}

		List<TomTatKhoa> kq = new ArrayList<>(theoKhoa.values());
		kq.sort(Comparator.comparing(TomTatKhoa::getTen));
		return kq;
	}

	/** Mã khoa phụ trách học phần của lớp; -1 khi học phần chưa gắn khoa */
	public static Integer maKhoa(LopHocPhan lop) {
		var donVi = lop.getHocPhan().getDonVi();
		return (donVi == null) ? -1 : donVi.getMaDonVi();
	}

	public static String tenKhoaCuaLop(LopHocPhan lop) {
		var donVi = lop.getHocPhan().getDonVi();
		return (donVi == null) ? "Chưa rõ khoa phụ trách" : donVi.getTenDonVi();
	}

	// ===================== XEM =====================

	@Transactional(readOnly = true)
	public List<DongPhanCong> dsLop(Integer maHocKy, boolean chiChuaPhanCong) {
		return dsLop(maHocKy, chiChuaPhanCong, null);
	}

	/**
	 * @param maDonVi để trống thì lấy cả trường, có thì chỉ lấy lớp của khoa đó
	 */
	@Transactional(readOnly = true)
	public List<DongPhanCong> dsLop(Integer maHocKy, boolean chiChuaPhanCong, Integer maDonVi) {
		List<DongPhanCong> tatCa = dsLopToanTruong(maHocKy, chiChuaPhanCong);
		if (maDonVi == null) return tatCa;

		List<DongPhanCong> kq = new ArrayList<>();
		for (DongPhanCong d : tatCa) {
			if (maDonVi.equals(maKhoa(d.getLop()))) kq.add(d);
		}
		return kq;
	}

	private List<DongPhanCong> dsLopToanTruong(Integer maHocKy, boolean chiChuaPhanCong) {
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

	/**
	 * Giảng viên gợi ý cho ô chọn, gom theo khoa công tác.
	 *
	 * Gom theo khoa để ô chọn nói được giảng viên thuộc khoa nào mà không
	 * phải lặp tên khoa trên từng dòng — tên khoa dài, lặp lại thì ô chọn
	 * hẹp sẽ cắt mất tên người.
	 *
	 * Ưu tiên giảng viên của khoa phụ trách học phần. Khoa đó không có ai
	 * đang công tác thì mở ra toàn trường, lúc này việc gom theo khoa mới
	 * thật sự cần vì danh sách trộn nhiều khoa.
	 *
	 * Chỉ lấy người đang công tác. Trước đây nhánh dự phòng lấy tất cả, kể
	 * cả người đã nghỉ, nên ô chọn mời một cái tên mà bấm Giao lớp là bị
	 * doiGiangVien chặn lại — người dùng không hiểu vì sao chọn được mà
	 * không giao được.
	 */
	@Transactional(readOnly = true)
	public Map<String, List<GiangVien>> dsGiangVien(Integer maDonVi) {
		List<GiangVien> ds = maDonVi == null ? List.of()
				: giangVienRepository.findByDonVi_MaDonViAndTrangThaiOrderByMaSoGiangVien(
						maDonVi, DANG_CONG_TAC);

		if (ds.isEmpty())
			ds = giangVienRepository.findByTrangThaiOrderByMaSoGiangVien(DANG_CONG_TAC);

		// TreeMap để các khoa xếp theo tên, khỏi phải sắp lại ở giao diện
		Map<String, List<GiangVien>> theoKhoa = new TreeMap<>();
		for (GiangVien gv : ds) {
			theoKhoa.computeIfAbsent(tenKhoa(gv), k -> new ArrayList<>()).add(gv);
		}
		return theoKhoa;
	}

	/** Tên khoa công tác, dùng chung cho ô chọn lẫn dòng đang phân công */
	public static String tenKhoa(GiangVien gv) {
		if (gv == null || gv.getDonVi() == null) return "Chưa rõ khoa";
		return gv.getDonVi().getTenDonVi();
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