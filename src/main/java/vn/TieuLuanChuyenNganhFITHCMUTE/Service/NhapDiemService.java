package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiangVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;

/** Giảng viên nhập điểm và gửi bảng điểm cho Phòng Đào tạo duyệt */
@Service
@RequiredArgsConstructor
public class NhapDiemService {

	// Trạng thái điểm (KetQuaHocPhan.TrangThai)
	public static final int KQ_DANG_NHAP = 0;
	public static final int KQ_CHO_DUYET = 1;
	public static final int KQ_DA_DUYET  = 2;
	public static final int KQ_CONG_BO   = 3;

	private static final int VAI_TRO_CHINH = 1;
	private static final int HIEU_LUC = 1;
	private static final int DA_DANG_KY = 1;

	// Lớp đang học (2) hoặc đã kết thúc (3) mới được nhập điểm
	private static final int LOP_DANG_HOC = 2;
	private static final int LOP_KET_THUC = 3;

	// Trọng số điểm tổng kết
	private static final BigDecimal TL_QUA_TRINH = new BigDecimal("0.2");
	private static final BigDecimal TL_GIUA_KY   = new BigDecimal("0.3");
	private static final BigDecimal TL_CUOI_KY   = new BigDecimal("0.5");

	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongRepository;
	private final DangKyHocPhanRepository dangKyRepository;
	private final KetQuaHocPhanRepository ketQuaRepository;
	private final LopHocPhanRepository lopRepository;

	/** Một dòng trong bảng điểm: lượt đăng ký của sinh viên kèm kết quả (có thể chưa có) */
	@Getter
	@AllArgsConstructor
	public static class DongDiem {
		private DangKyHocPhan dangKy;
		private KetQuaHocPhan ketQua;
	}

	// ===================== ĐỌC DỮ LIỆU =====================

	public GiangVien giangVien(String tenDangNhap) {
		return giangVienRepository.findByTaiKhoan_TenDangNhap(tenDangNhap).orElse(null);
	}

	/** Các lớp được phân công, học kỳ mới nhất trước */
	public List<PhanCongGiangDay> dsPhanCong(GiangVien gv) {
		return phanCongRepository.findByGiangVien_MaGiangVienAndTrangThai(gv.getMaGiangVien(), HIEU_LUC)
				.stream()
				.sorted(Comparator
						.comparing((PhanCongGiangDay pc) -> pc.getLopHocPhan().getHocKy().getNgayBatDau())
						.reversed()
						.thenComparing(pc -> pc.getLopHocPhan().getMaLop()))
				.toList();
	}

	/** Lấy lớp, đồng thời kiểm tra giảng viên có phải giảng viên chính của lớp */
	public LopHocPhan lopDuocDay(GiangVien gv, Integer maLop) {
		boolean duocDay = phanCongRepository
				.existsByLopHocPhan_MaLopHocPhanAndGiangVien_MaGiangVienAndVaiTroAndTrangThai(
						maLop, gv.getMaGiangVien(), VAI_TRO_CHINH, HIEU_LUC);
		if (!duocDay)
			throw new IllegalStateException("Bạn không phải giảng viên chính của lớp này.");
		return lopRepository.findById(maLop)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
	}

	public List<DongDiem> bangDiem(Integer maLop) {
		List<DangKyHocPhan> dsDangKy = dangKyRepository.findByLopHocPhan_MaLopHocPhanAndTrangThai(
				maLop, DA_DANG_KY, Sort.by("sinhVien.maSoSinhVien"));
		if (dsDangKy.isEmpty()) return List.of();

		Map<Integer, KetQuaHocPhan> theoDangKy = ketQuaRepository
				.findByDangKyHocPhan_MaDangKyHocPhanIn(
						dsDangKy.stream().map(DangKyHocPhan::getMaDangKyHocPhan).toList())
				.stream()
				.collect(Collectors.toMap(kq -> kq.getDangKyHocPhan().getMaDangKyHocPhan(), Function.identity()));

		return dsDangKy.stream()
				.map(dk -> new DongDiem(dk, theoDangKy.get(dk.getMaDangKyHocPhan())))
				.toList();
	}

	/** Bảng điểm còn sửa được không (chưa sinh viên nào bị gửi duyệt) */
	public boolean choPhepSua(List<DongDiem> ds) {
		return ds.stream().noneMatch(d -> d.getKetQua() != null && d.getKetQua().getTrangThai() >= KQ_CHO_DUYET);
	}

	/** Mô tả tình trạng bảng điểm để hiển thị ở danh sách lớp */
	public String tinhTrang(List<DongDiem> ds) {
		if (ds.isEmpty()) return "Chưa có sinh viên";
		long coTongKet = ds.stream()
				.filter(d -> d.getKetQua() != null && d.getKetQua().getDiemTongKet() != null).count();
		int thapNhat = ds.stream()
				.mapToInt(d -> d.getKetQua() == null ? -1 : d.getKetQua().getTrangThai())
				.min().orElse(-1);

		if (thapNhat >= KQ_CONG_BO)   return "Đã công bố";
		if (thapNhat >= KQ_DA_DUYET)  return "Đã duyệt";
		if (thapNhat >= KQ_CHO_DUYET) return "Chờ duyệt";
		if (coTongKet == 0)           return "Chưa nhập";
		return "Đang nhập (" + coTongKet + "/" + ds.size() + ")";
	}

	// ===================== LƯU ĐIỂM =====================

	/**
	 * Lưu điểm từ form. Tên ô nhập: qt_{maDangKy}, gk_{maDangKy}, ck_{maDangKy}.
	 * Đủ cả 3 điểm thì tự tính tổng kết, điểm chữ, hệ 4.
	 * @return số sinh viên được cập nhật
	 */
	@Transactional
	public int luuDiem(GiangVien gv, Integer maLop, Map<String, String> params) {
		LopHocPhan lop = lopDuocDay(gv, maLop);
		kiemTraLopChoNhapDiem(lop);

		List<DongDiem> ds = bangDiem(maLop);
		if (!choPhepSua(ds))
			throw new IllegalStateException("Bảng điểm đã gửi duyệt, không sửa được nữa.");

		int soDong = 0;
		for (DongDiem d : ds) {
			DangKyHocPhan dk = d.getDangKy();
			String mssv = dk.getSinhVien().getMaSoSinhVien();
			Integer id = dk.getMaDangKyHocPhan();

			BigDecimal qt = docDiem(params.get("qt_" + id), mssv);
			BigDecimal gk = docDiem(params.get("gk_" + id), mssv);
			BigDecimal ck = docDiem(params.get("ck_" + id), mssv);

			// Chưa có kết quả và cũng không nhập gì → bỏ qua
			if (d.getKetQua() == null && qt == null && gk == null && ck == null) continue;

			KetQuaHocPhan kq = d.getKetQua() != null ? d.getKetQua() : new KetQuaHocPhan();
			kq.setDangKyHocPhan(dk);
			kq.setDiemQuaTrinh(qt);
			kq.setDiemGiuaKy(gk);
			kq.setDiemCuoiKy(ck);

			if (qt != null && gk != null && ck != null) {
				BigDecimal tk = qt.multiply(TL_QUA_TRINH)
						.add(gk.multiply(TL_GIUA_KY))
						.add(ck.multiply(TL_CUOI_KY))
						.setScale(1, RoundingMode.HALF_UP);
				kq.setDiemTongKet(tk);
				quyDoi(kq, tk);
			} else {
				// Thiếu điểm thành phần → chưa có tổng kết
				kq.setDiemTongKet(null);
				kq.setDiemHe4(null);
				kq.setDiemChu(null);
				kq.setKetQua(null);
			}

			kq.setTrangThai(KQ_DANG_NHAP);
			kq.setNguoiNhap(gv.getTaiKhoan());
			kq.setNgayNhap(LocalDateTime.now());
			ketQuaRepository.save(kq);
			soDong++;
		}
		return soDong;
	}

	// ===================== GỬI DUYỆT =====================

	@Transactional
	public void guiDuyet(GiangVien gv, Integer maLop) {
		LopHocPhan lop = lopDuocDay(gv, maLop);
		kiemTraLopChoNhapDiem(lop);

		List<DongDiem> ds = bangDiem(maLop);
		if (ds.isEmpty())
			throw new IllegalStateException("Lớp chưa có sinh viên.");
		if (!choPhepSua(ds))
			throw new IllegalStateException("Bảng điểm đã được gửi duyệt trước đó.");

		long thieu = ds.stream()
				.filter(d -> d.getKetQua() == null || d.getKetQua().getDiemTongKet() == null).count();
		if (thieu > 0)
			throw new IllegalStateException("Còn " + thieu + " sinh viên chưa đủ 3 cột điểm. Nhập đủ rồi mới gửi duyệt.");

		for (DongDiem d : ds) {
			d.getKetQua().setTrangThai(KQ_CHO_DUYET);
			ketQuaRepository.save(d.getKetQua());
		}
	}

	// ===================== HỖ TRỢ =====================

	private void kiemTraLopChoNhapDiem(LopHocPhan lop) {
		if (lop.getTrangThai() != LOP_DANG_HOC && lop.getTrangThai() != LOP_KET_THUC)
			throw new IllegalStateException("Chỉ nhập điểm khi lớp đang học hoặc đã kết thúc. "
					+ "Phòng Đào tạo cần khóa đăng ký lớp " + lop.getMaLop() + " trước.");
	}

	/** Đọc một ô điểm: trống → null; chấp nhận dấu phẩy; phải từ 0 đến 10 */
	private BigDecimal docDiem(String giaTri, String mssv) {
		if (giaTri == null || giaTri.isBlank()) return null;
		try {
			BigDecimal d = new BigDecimal(giaTri.trim().replace(',', '.'));
			if (d.compareTo(BigDecimal.ZERO) < 0 || d.compareTo(BigDecimal.TEN) > 0)
				throw new IllegalStateException("Điểm của sinh viên " + mssv + " phải từ 0 đến 10.");
			return d.setScale(2, RoundingMode.HALF_UP);
		} catch (NumberFormatException e) {
			throw new IllegalStateException("Điểm của sinh viên " + mssv + " không hợp lệ: \"" + giaTri + "\".");
		}
	}

	/** Quy đổi điểm tổng kết thang 10 → điểm chữ, thang 4, đạt/không đạt */
	public static void quyDoi(KetQuaHocPhan kq, BigDecimal tongKet) {
		double tk = tongKet.doubleValue();
		String chu; double he4;
		if      (tk >= 8.5) { chu = "A";  he4 = 4.0; }
		else if (tk >= 8.0) { chu = "B+"; he4 = 3.5; }
		else if (tk >= 7.0) { chu = "B";  he4 = 3.0; }
		else if (tk >= 6.5) { chu = "C+"; he4 = 2.5; }
		else if (tk >= 5.5) { chu = "C";  he4 = 2.0; }
		else if (tk >= 5.0) { chu = "D+"; he4 = 1.5; }
		else if (tk >= 4.0) { chu = "D";  he4 = 1.0; }
		else                { chu = "F";  he4 = 0.0; }

		kq.setDiemChu(chu);
		kq.setDiemHe4(BigDecimal.valueOf(he4).setScale(2));
		kq.setKetQua(tk >= 4.0 ? 1 : 0);
	}
}
