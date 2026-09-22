package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;

@Service
@RequiredArgsConstructor
public class DangKyHocPhanService {

	public static final int TIN_CHI_TOI_DA = 25;

	private static final int HOC_KY_DANG_DIEN_RA = 1;
	private static final int LOP_DANG_MO = 1;
	private static final int DA_DANG_KY = 1;
	private static final int DA_HUY = 2;

	private final HocKyRepository hocKyRepository;
	private final LopHocPhanRepository lopHocPhanRepository;
	private final DangKyHocPhanRepository dangKyRepository;
	private final HocPhiService hocPhiService;
	private final LichHocService lichHocService;

	// ===================== ĐỌC DỮ LIỆU =====================

	public Optional<HocKy> hocKyHienTai() {
		return hocKyRepository.findFirstByTrangThaiOrderByNgayBatDauDesc(HOC_KY_DANG_DIEN_RA);
	}

	public List<LopHocPhan> dsLopDangMo(HocKy hk) {
		return lopHocPhanRepository.findByHocKy_MaHocKyAndTrangThai(
				hk.getMaHocKy(), LOP_DANG_MO, Sort.by("hocPhan.maHocPhan", "maLop"));
	}

	public List<DangKyHocPhan> dsDaDangKy(SinhVien sv, HocKy hk) {
		return dangKyRepository.findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThai(
				sv.getMaSinhVien(), hk.getMaHocKy(), DA_DANG_KY);
	}

	/** Mã lớp → số sinh viên đang đăng ký */
	public Map<Integer, Long> siSoCacLop(List<LopHocPhan> dsLop) {
		Map<Integer, Long> kq = new HashMap<>();
		for (LopHocPhan lop : dsLop) {
			kq.put(lop.getMaLopHocPhan(),
					dangKyRepository.countByLopHocPhan_MaLopHocPhanAndTrangThai(lop.getMaLopHocPhan(), DA_DANG_KY));
		}
		return kq;
	}

	public int tongTinChi(List<DangKyHocPhan> ds) {
		return ds.stream().mapToInt(dk -> dk.getLopHocPhan().getHocPhan().getSoTinChi()).sum();
	}

	// ===================== ĐĂNG KÝ =====================

	@Transactional
	public void dangKy(SinhVien sv, Integer maLopHocPhan) {
		LopHocPhan lop = lopHocPhanRepository.findById(maLopHocPhan)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
		HocKy hk = lop.getHocKy();

		if (!hk.isDangMoDangKy())
			throw new IllegalStateException("Hiện không trong thời gian đăng ký học phần.");
		if (lop.getTrangThai() != LOP_DANG_MO)
			throw new IllegalStateException("Lớp " + lop.getMaLop() + " không mở đăng ký.");

		List<DangKyHocPhan> daDangKy = dsDaDangKy(sv, hk);

		// Không đăng ký hai lớp cùng một học phần
		Integer maHp = lop.getHocPhan().getMaHocPhanId();
		boolean trungMon = daDangKy.stream()
				.anyMatch(dk -> dk.getLopHocPhan().getHocPhan().getMaHocPhanId().equals(maHp));
		if (trungMon)
			throw new IllegalStateException("Bạn đã đăng ký một lớp khác của học phần "
					+ lop.getHocPhan().getTenHocPhan() + ".");

		// Giới hạn tín chỉ
		int tcSau = tongTinChi(daDangKy) + lop.getHocPhan().getSoTinChi();
		if (tcSau > TIN_CHI_TOI_DA)
			throw new IllegalStateException("Vượt quá " + TIN_CHI_TOI_DA + " tín chỉ cho phép trong học kỳ.");

		// Sĩ số
		long siSo = dangKyRepository.countByLopHocPhan_MaLopHocPhanAndTrangThai(maLopHocPhan, DA_DANG_KY);
		if (lop.getSiSoToiDa() != null && siSo >= lop.getSiSoToiDa())
			throw new IllegalStateException("Lớp " + lop.getMaLop() + " đã đủ sĩ số.");

		// Trùng lịch với lớp đã đăng ký
		lichHocService.kiemTraTrungLich(lop, daDangKy);

		// Đã từng đăng ký rồi hủy → dùng lại dòng cũ (ràng buộc UNIQUE)
		DangKyHocPhan dk = dangKyRepository
				.findBySinhVien_MaSinhVienAndLopHocPhan_MaLopHocPhan(sv.getMaSinhVien(), maLopHocPhan)
				.orElseGet(DangKyHocPhan::new);

		dk.setSinhVien(sv);
		dk.setLopHocPhan(lop);
		dk.setLoaiDangKy(1);
		dk.setTrangThai(DA_DANG_KY);
		dk.setNgayDangKy(LocalDateTime.now());
		dk.setNgayHuy(null);
		dk.setLyDoHuy(null);
		dangKyRepository.saveAndFlush(dk);

		hocPhiService.capNhatCongNo(sv, hk);
	}

	// ===================== HỦY =====================

	@Transactional
	public void huy(SinhVien sv, Integer maDangKy) {
		DangKyHocPhan dk = dangKyRepository.findById(maDangKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt đăng ký."));

		if (!dk.getSinhVien().getMaSinhVien().equals(sv.getMaSinhVien()))
			throw new IllegalStateException("Bạn không có quyền hủy đăng ký này.");
		HocKy hk = dk.getLopHocPhan().getHocKy();
		if (!hk.isDangMoDangKy())
			throw new IllegalStateException("Đã hết thời gian đăng ký, không thể hủy.");
		if (dk.getTrangThai() != DA_DANG_KY)
			throw new IllegalStateException("Lượt đăng ký này không còn hiệu lực.");

		dk.setTrangThai(DA_HUY);
		dk.setNgayHuy(LocalDateTime.now());
		dk.setLyDoHuy("Sinh viên tự hủy");
		dangKyRepository.saveAndFlush(dk);

		hocPhiService.capNhatCongNo(sv, hk);
	}
}
