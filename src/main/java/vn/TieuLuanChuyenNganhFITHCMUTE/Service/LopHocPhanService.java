package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhongHocRepository;

/** Phòng Đào tạo mở lớp học phần, xếp lịch, đổi trạng thái lớp */
@Service
@RequiredArgsConstructor
public class LopHocPhanService {

	// Trạng thái lớp
	public static final int CHUA_MO = 0;
	public static final int MO_DANG_KY = 1;
	public static final int DANG_HOC = 2;
	public static final int KET_THUC = 3;
	public static final int DA_HUY = 4;

	/** Loại lớp: 1 chính khóa, 2 học lại và cải thiện, 3 học hè, 4 lớp tối */
	public static final int LOP_CHINH_KHOA = 1;

	private static final int SO_TIET_TOI_DA_TRONG_NGAY = 12;
	private static final int DA_DANG_KY = 1;

	private final LopHocPhanRepository lopRepository;
	private final HocPhanRepository hocPhanRepository;
	private final HocKyRepository hocKyRepository;
	private final PhongHocRepository phongHocRepository;
	private final LichHocRepository lichHocRepository;
	private final DangKyHocPhanRepository dangKyRepository;
	private final EntityManager em;

	// ===================== DỮ LIỆU CHO FORM =====================

	public List<LopHocPhan> dsTheoHocKy(Integer maHocKy) {
		return lopRepository.findByHocKy_MaHocKy(maHocKy, Sort.by("hocPhan.maHocPhan", "maLop"));
	}

	public List<HocPhan> dsHocPhan() {
		return hocPhanRepository.findAll(Sort.by("maHocPhan"));
	}

	public List<PhongHoc> dsPhongDangDung() {
		return phongHocRepository.findByTrangThaiOrderByMaPhong(1);
	}

	/**
	 * Mã học phần → chuỗi mã ngành có học phần đó trong chương trình, dạng "|3|7|12|".
	 * Dùng cho bộ lọc theo ngành ở form mở lớp, lọc ngay trên trình duyệt.
	 */
	public Map<Integer, String> nganhCuaHocPhan() {
		Map<Integer, String> kq = new HashMap<>();
		List<Object[]> rows = em.createQuery("""
				SELECT ct.hocPhan.maHocPhanId, ct.nganh.maNganh
				FROM ChuongTrinhDaoTao ct WHERE ct.trangThai = 1
				""", Object[].class).getResultList();

		for (Object[] r : rows) {
			Integer maHp = (Integer) r[0];
			Integer maNganh = (Integer) r[1];
			kq.merge(maHp, maNganh + "|", (cu, moi) -> cu + moi);
		}
		// Thêm dấu | ở đầu để so khớp trọn vẹn: chuỗi "|3|" không khớp nhầm với 13, 23
		kq.replaceAll((k, v) -> "|" + v);
		return kq;
	}

	// ===================== MỞ LỚP MỚI =====================

	@Transactional
	public LopHocPhan taoLop(Integer maHocPhanId, Integer maHocKy, int nhom,
	                         int siSoToiDa, int siSoToiThieu,
	                         int thu, int tietBatDau, int soTiet, Integer maPhongHoc,
	                         int loaiLop, String khoaApDung) {

		HocPhan hp = hocPhanRepository.findById(maHocPhanId)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học phần."));
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));
		PhongHoc phong = phongHocRepository.findById(maPhongHoc)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy phòng học."));

		// --- Kiểm tra dữ liệu nhập ---
		if (hk.getTrangThai() == 2)
			throw new IllegalStateException("Học kỳ đã kết thúc, không thể mở lớp.");
		if (nhom < 1 || nhom > 99)
			throw new IllegalStateException("Số nhóm phải từ 1 đến 99.");
		if (siSoToiDa < 1)
			throw new IllegalStateException("Sĩ số tối đa phải lớn hơn 0.");
		if (siSoToiThieu < 0 || siSoToiThieu > siSoToiDa)
			throw new IllegalStateException("Sĩ số tối thiểu phải từ 0 đến sĩ số tối đa.");
		if (siSoToiDa > phong.getSucChua())
			throw new IllegalStateException("Phòng " + phong.getMaPhong() + " chỉ chứa được "
					+ phong.getSucChua() + " người, nhỏ hơn sĩ số tối đa.");
		if (thu < 2 || thu > 8)
			throw new IllegalStateException("Thứ không hợp lệ.");
		if (soTiet < 1 || tietBatDau < 1 || tietBatDau + soTiet - 1 > SO_TIET_TOI_DA_TRONG_NGAY)
			throw new IllegalStateException("Tiết học phải nằm trong khoảng 1 đến "
					+ SO_TIET_TOI_DA_TRONG_NGAY + ".");

		if (loaiLop < 1 || loaiLop > 4)
			throw new IllegalStateException("Loại lớp không hợp lệ.");

		String khoa = (khoaApDung == null || khoaApDung.isBlank()) ? null : khoaApDung.trim();
		if (loaiLop == LOP_CHINH_KHOA && khoa == null)
			throw new IllegalStateException("Lớp chính khóa phải ghi rõ khóa áp dụng, ví dụ 2024.");
		if (loaiLop != LOP_CHINH_KHOA) khoa = null;   // lớp học lại, hè, tối mở cho mọi khóa

		// Mã lớp có phần giữa cho biết lớp dành cho ai: khóa tuyển sinh hoặc loại lớp
		String phanGiua = switch (loaiLop) {
			case 2 -> "HL";
			case 3 -> "HE";
			case 4 -> "TOI";
			default -> khoa;
		};
		String maLop = hp.getMaHocPhan() + "_" + hk.getMaHocKyCode()
				+ "_" + phanGiua + "_" + String.format("%02d", nhom);
		if (lopRepository.existsByMaLop(maLop))
			throw new IllegalStateException("Lớp " + maLop + " đã tồn tại. Chọn số nhóm khác.");

		// --- Buổi học dự kiến ---
		LichHoc lichMoi = new LichHoc();
		lichMoi.setThu(thu);
		lichMoi.setTietBatDau(tietBatDau);
		lichMoi.setSoTiet(soTiet);
		lichMoi.setPhongHoc(phong);

		// --- Trùng phòng: cùng phòng, cùng thứ, cùng học kỳ, tiết giao nhau ---
		List<LichHoc> cungPhong = lichHocRepository
				.findByPhongHoc_MaPhongHocAndThuAndLopHocPhan_HocKy_MaHocKy(maPhongHoc, thu, maHocKy);
		for (LichHoc lh : cungPhong) {
			if (lh.getLopHocPhan().getTrangThai() == DA_HUY) continue;
			if (lichMoi.trungGio(lh)) {
				throw new IllegalStateException("Phòng " + phong.getMaPhong() + " đã có lớp "
						+ lh.getLopHocPhan().getMaLop() + " vào " + lh.getTenThu()
						+ ", tiết " + lh.getTietBatDau() + "–" + lh.getTietKetThuc() + ".");
			}
		}

		// --- Lưu lớp và lịch ---
		LopHocPhan lop = new LopHocPhan();
		lop.setMaLop(maLop);
		lop.setHocPhan(hp);
		lop.setHocKy(hk);
		lop.setSiSoToiDa(siSoToiDa);
		lop.setSiSoToiThieu(siSoToiThieu);
		lop.setLoaiLop(loaiLop);
		lop.setKhoaApDung(khoa);
		lop.setTrangThai(CHUA_MO);
		lop = lopRepository.save(lop);

		lichMoi.setLopHocPhan(lop);
		lichMoi.setNgayBatDau(hk.getNgayBatDau());
		lichMoi.setNgayKetThuc(hk.getNgayKetThuc());
		lichHocRepository.save(lichMoi);

		return lop;
	}

	// ===================== ĐỔI TRẠNG THÁI =====================

	@Transactional
	public LopHocPhan doiTrangThai(Integer maLop, int trangThaiMoi) {
		LopHocPhan lop = lopRepository.findById(maLop)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
		int cu = lop.getTrangThai();

		if (cu == KET_THUC || cu == DA_HUY)
			throw new IllegalStateException("Lớp đã kết thúc hoặc đã hủy, không đổi trạng thái được.");

		// Chỉ cho các bước chuyển hợp lý
		boolean hopLe = switch (trangThaiMoi) {
			case MO_DANG_KY -> cu == CHUA_MO || cu == DANG_HOC;   // mở / mở lại đăng ký
			case DANG_HOC   -> cu == MO_DANG_KY;                  // khóa đăng ký, bắt đầu học
			case KET_THUC   -> cu == DANG_HOC;
			case DA_HUY     -> cu == CHUA_MO || cu == MO_DANG_KY;
			default -> false;
		};
		if (!hopLe)
			throw new IllegalStateException("Không thể chuyển lớp từ trạng thái hiện tại sang trạng thái này.");

		// Không hủy lớp đã có sinh viên đăng ký
		if (trangThaiMoi == DA_HUY) {
			long siSo = dangKyRepository.countByLopHocPhan_MaLopHocPhanAndTrangThai(maLop, DA_DANG_KY);
			if (siSo > 0)
				throw new IllegalStateException("Lớp đã có " + siSo + " sinh viên đăng ký, không thể hủy.");
		}

		lop.setTrangThai(trangThaiMoi);
		return lopRepository.save(lop);
	}
}