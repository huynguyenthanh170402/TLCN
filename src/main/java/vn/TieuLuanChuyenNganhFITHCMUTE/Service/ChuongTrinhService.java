package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;

/**
 * Chương trình đào tạo của các ngành: xem và chỉnh sửa trực tiếp.
 *
 * Chương trình đào tạo là gốc của gần như mọi thứ phía sau: sinh viên đăng
 * ký được môn nào, Phòng Đào tạo phải mở bao nhiêu lớp ở học kỳ nào, học phí
 * tính trên bao nhiêu tín chỉ. Sửa ở đây là sửa vào chỗ đó, nên mọi điều
 * kiện đều kiểm ở tầng này chứ không giao cho giao diện.
 */
@Service
@RequiredArgsConstructor
public class ChuongTrinhService {

	private static final int DANG_AP_DUNG = 1;
	private static final int NGUNG_AP_DUNG = 0;

	private static final int SO_HOC_KY_TOI_DA = 8;

	private final EntityManager em;
	private final NganhRepository nganhRepository;
	private final HocPhanRepository hocPhanRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;

	/** Một dòng trong bảng tổng quan các ngành */
	public record NganhThongKe(Integer maNganh, String tenNganh,
	                           Integer maDonVi, String tenKhoa,
	                           long soHocPhan, long tongTinChi) {

		public boolean coChuongTrinh() { return soHocPhan > 0; }
	}

	/** Một ô trên màn hình chọn khoa */
	@Getter
	public static class KhoaThongKe {
		private final Integer maDonVi;
		private final String ten;

		private int soNganh;
		private int soNganhCoChuongTrinh;
		private long tongHocPhan;

		KhoaThongKe(Integer maDonVi, String ten) { this.maDonVi = maDonVi; this.ten = ten; }

		public int getSoNganhChuaCo() { return soNganh - soNganhCoChuongTrinh; }
		public boolean isXong()       { return soNganh > 0 && getSoNganhChuaCo() == 0; }
	}

	/** Kết quả một lần xóa, để màn hình nói đúng chuyện vừa xảy ra */
	public record KetQuaXoa(boolean xoaHan, long soSinhVienDaHoc) {}

	// ===================== XEM =====================

	/**
	 * Thống kê chương trình của từng ngành, kèm khoa quản lý.
	 *
	 * Nối LEFT tới đơn vị chứ không viết n.donVi.tenDonVi. Cách viết tắt đó
	 * sinh ra phép nối trong, nên ngành chưa gắn khoa sẽ biến mất khỏi danh
	 * sách — đúng những ngành đang thiếu dữ liệu lại là thứ không ai nhìn
	 * thấy để sửa.
	 */
	@Transactional(readOnly = true)
	public List<NganhThongKe> tongQuanCacNganh() {
		return em.createQuery("""
				SELECT n.maNganh, n.tenNganh, dv.maDonVi, dv.tenDonVi,
				       COUNT(ct), COALESCE(SUM(ct.hocPhan.soTinChi), 0)
				FROM Nganh n
				LEFT JOIN n.donVi dv
				LEFT JOIN ChuongTrinhDaoTao ct
				       ON ct.nganh = n AND ct.trangThai = 1
				GROUP BY n.maNganh, n.tenNganh, dv.maDonVi, dv.tenDonVi
				ORDER BY dv.tenDonVi, n.tenNganh
				""", Object[].class)
				.getResultList().stream()
				.map(r -> new NganhThongKe(
						(Integer) r[0], (String) r[1],
						(Integer) r[2], (String) r[3],
						((Number) r[4]).longValue(), ((Number) r[5]).longValue()))
				.toList();
	}

	/** Gom theo khoa cho màn hình chọn khoa */
	@Transactional(readOnly = true)
	public List<KhoaThongKe> tongQuanCacKhoa() {
		Map<Integer, KhoaThongKe> theoKhoa = new LinkedHashMap<>();

		for (NganhThongKe n : tongQuanCacNganh()) {
			Integer ma = (n.maDonVi() == null) ? -1 : n.maDonVi();
			String ten = (n.tenKhoa() == null) ? "Chưa gắn khoa" : n.tenKhoa();

			KhoaThongKe k = theoKhoa.computeIfAbsent(ma, x -> new KhoaThongKe(x, ten));
			k.soNganh++;
			k.tongHocPhan += n.soHocPhan();
			if (n.coChuongTrinh()) k.soNganhCoChuongTrinh++;
		}
		return new ArrayList<>(theoKhoa.values());
	}

	/** Các ngành của một khoa; mã -1 là nhóm ngành chưa gắn khoa */
	@Transactional(readOnly = true)
	public List<NganhThongKe> dsNganhCuaKhoa(Integer maDonVi) {
		return tongQuanCacNganh().stream()
				.filter(n -> {
					Integer ma = (n.maDonVi() == null) ? -1 : n.maDonVi();
					return ma.equals(maDonVi);
				})
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ChuongTrinhDaoTao> chiTiet(Integer maNganh) {
		return chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				maNganh, DANG_AP_DUNG, Sort.by("hocKyDuKien", "hocPhan.maHocPhan"));
	}

	/**
	 * Các học phần còn có thể thêm vào chương trình của ngành này.
	 *
	 * Chỉ loại ra những học phần đang áp dụng. Học phần từng bị gỡ khỏi
	 * chương trình vẫn hiện lại trong danh sách, vì trường có thể đưa một
	 * môn trở lại chương trình ở lần chỉnh khung tiếp theo.
	 */
	@Transactional(readOnly = true)
	public List<HocPhan> hocPhanCoTheThem(Integer maNganh) {
		return em.createQuery("""
				SELECT hp FROM HocPhan hp
				WHERE hp.trangThai = 1
				  AND hp.maHocPhanId NOT IN (
				      SELECT ct.hocPhan.maHocPhanId FROM ChuongTrinhDaoTao ct
				      WHERE ct.nganh.maNganh = :maNganh AND ct.trangThai = 1
				  )
				ORDER BY hp.maHocPhan
				""", HocPhan.class)
				.setParameter("maNganh", maNganh)
				.getResultList();
	}

	// ===================== THÊM =====================

	@Transactional
	public void them(Integer maNganh, Integer maHocPhanId, Integer hocKyDuKien,
	                 Integer khoiKienThuc, Integer loaiHocPhan) {

		Nganh nganh = nganhRepository.findById(maNganh)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy ngành."));
		HocPhan hp = hocPhanRepository.findById(maHocPhanId)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học phần."));

		kiemTraGiaTri(hocKyDuKien, khoiKienThuc, loaiHocPhan);

		/*
		 * Bảng có ràng buộc duy nhất trên cặp (ngành, học phần), kể cả dòng
		 * đã ngừng áp dụng. Nên khi học phần từng nằm trong chương trình rồi
		 * bị gỡ, phải bật lại dòng cũ thay vì chèn dòng mới — chèn mới sẽ
		 * vướng ràng buộc và văng lỗi cơ sở dữ liệu khó hiểu ra màn hình.
		 */
		ChuongTrinhDaoTao ct = chuongTrinhRepository
				.findByNganh_MaNganhAndHocPhan_MaHocPhanId(maNganh, maHocPhanId)
				.orElseGet(ChuongTrinhDaoTao::new);

		if (ct.getMaChuongTrinh() != null
				&& Integer.valueOf(DANG_AP_DUNG).equals(ct.getTrangThai()))
			throw new IllegalStateException(
					"Học phần \"" + hp.getTenHocPhan() + "\" đã có trong chương trình của ngành này.");

		ct.setNganh(nganh);
		ct.setHocPhan(hp);
		ct.setHocKyDuKien(hocKyDuKien);
		ct.setKhoiKienThuc(khoiKienThuc);
		ct.setLoaiHocPhan(loaiHocPhan);
		ct.setTrangThai(DANG_AP_DUNG);
		chuongTrinhRepository.saveAndFlush(ct);
	}

	// ===================== SỬA =====================

	@Transactional
	public void sua(Integer maChuongTrinh, Integer hocKyDuKien,
	                Integer khoiKienThuc, Integer loaiHocPhan) {

		ChuongTrinhDaoTao ct = chuongTrinhRepository.findById(maChuongTrinh)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy dòng chương trình."));

		kiemTraGiaTri(hocKyDuKien, khoiKienThuc, loaiHocPhan);

		// Học phần và ngành không cho đổi: đổi hai thứ đó nghĩa là một dòng
		// khác hẳn, nên gỡ dòng cũ rồi thêm dòng mới cho rõ ràng.
		ct.setHocKyDuKien(hocKyDuKien);
		ct.setKhoiKienThuc(khoiKienThuc);
		ct.setLoaiHocPhan(loaiHocPhan);
		chuongTrinhRepository.saveAndFlush(ct);
	}

	// ===================== XÓA =====================

	/**
	 * Gỡ một học phần khỏi chương trình.
	 *
	 * Chưa sinh viên nào của ngành học môn đó thì xóa hẳn dòng, vì nó chỉ là
	 * một dòng khai báo khung chương trình, không có gì tham chiếu tới.
	 *
	 * Đã có sinh viên học thì chuyển sang ngừng áp dụng chứ không xóa. Điểm
	 * và lượt đăng ký của họ gắn vào lớp học phần nên không mất, nhưng xóa
	 * dòng khung đi thì không còn tra được "khóa đó học môn này ở học kỳ mấy,
	 * bắt buộc hay tự chọn" — đúng thứ cần đến khi xét tốt nghiệp.
	 */
	@Transactional
	public KetQuaXoa xoa(Integer maChuongTrinh) {
		ChuongTrinhDaoTao ct = chuongTrinhRepository.findById(maChuongTrinh)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy dòng chương trình."));

		long daHoc = soSinhVienDaHoc(ct.getNganh().getMaNganh(),
				ct.getHocPhan().getMaHocPhanId());

		if (daHoc == 0) {
			chuongTrinhRepository.delete(ct);
			return new KetQuaXoa(true, 0);
		}

		ct.setTrangThai(NGUNG_AP_DUNG);
		chuongTrinhRepository.saveAndFlush(ct);
		return new KetQuaXoa(false, daHoc);
	}

	/** Số lượt sinh viên của ngành này đã đăng ký học phần đó */
	private long soSinhVienDaHoc(Integer maNganh, Integer maHocPhanId) {
		return em.createQuery("""
				SELECT COUNT(dk) FROM DangKyHocPhan dk
				WHERE dk.lopHocPhan.hocPhan.maHocPhanId = :maHocPhanId
				  AND dk.sinhVien.nganh.maNganh = :maNganh
				""", Long.class)
				.setParameter("maHocPhanId", maHocPhanId)
				.setParameter("maNganh", maNganh)
				.getSingleResult();
	}

	// ===================== DÙNG CHUNG =====================

	private void kiemTraGiaTri(Integer hocKyDuKien, Integer khoiKienThuc, Integer loaiHocPhan) {
		if (hocKyDuKien == null || hocKyDuKien < 1 || hocKyDuKien > SO_HOC_KY_TOI_DA)
			throw new IllegalStateException(
					"Học kỳ dự kiến phải từ 1 đến " + SO_HOC_KY_TOI_DA + ".");

		if (khoiKienThuc == null || khoiKienThuc < 1 || khoiKienThuc > 4)
			throw new IllegalStateException("Chưa chọn khối kiến thức.");

		if (loaiHocPhan == null || (loaiHocPhan != 1 && loaiHocPhan != 2))
			throw new IllegalStateException("Chưa chọn loại học phần.");
	}
}
