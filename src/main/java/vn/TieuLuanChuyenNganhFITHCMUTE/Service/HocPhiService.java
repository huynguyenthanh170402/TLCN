package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.CongNoHocPhiRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;

@Service
@RequiredArgsConstructor
public class HocPhiService {

	/** Đơn giá mỗi tín chỉ (VNĐ) — số mẫu, sửa theo quy định của trường */
	public static final BigDecimal DON_GIA_TIN_CHI = new BigDecimal("850000");

	/** Hạn đóng: 30 ngày sau ngày bắt đầu học kỳ */
	private static final int SO_NGAY_HAN_DONG = 30;

	private static final int DA_DANG_KY = 1;

	private final CongNoHocPhiRepository congNoRepository;
	private final DangKyHocPhanRepository dangKyRepository;

	/**
	 * Tính lại công nợ của sinh viên trong một học kỳ, dựa trên
	 * các học phần đang đăng ký. Gọi sau mỗi lần đăng ký/hủy.
	 */
	@Transactional
	public CongNoHocPhi capNhatCongNo(SinhVien sv, HocKy hk) {
		List<DangKyHocPhan> dsDk = dangKyRepository
				.findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThai(
						sv.getMaSinhVien(), hk.getMaHocKy(), DA_DANG_KY);

		int tongTinChi = dsDk.stream()
				.mapToInt(dk -> dk.getLopHocPhan().getHocPhan().getSoTinChi())
				.sum();

		CongNoHocPhi cn = congNoRepository
				.findBySinhVien_MaSinhVienAndHocKy_MaHocKy(sv.getMaSinhVien(), hk.getMaHocKy())
				.orElseGet(() -> {
					CongNoHocPhi moi = new CongNoHocPhi();
					moi.setSinhVien(sv);
					moi.setHocKy(hk);
					return moi;
				});

		cn.setSoTienPhaiDong(DON_GIA_TIN_CHI.multiply(BigDecimal.valueOf(tongTinChi)));
		cn.setNoiDung("Học phí " + hk.getTenHocKy() + " — " + tongTinChi + " tín chỉ");
		if (hk.getNgayBatDau() != null) {
			cn.setHanThanhToan(hk.getNgayBatDau().plusDays(SO_NGAY_HAN_DONG));
		}
		cn.setTrangThai(tinhTrangThai(cn, tongTinChi));

		return congNoRepository.save(cn);
	}

	public List<CongNoHocPhi> dsCongNo(SinhVien sv) {
		return congNoRepository.findBySinhVien_MaSinhVien(
				sv.getMaSinhVien(), Sort.by(Sort.Direction.DESC, "hocKy.ngayBatDau"));
	}

	private int tinhTrangThai(CongNoHocPhi cn, int tongTinChi) {
		if (tongTinChi == 0 && cn.getSoTienDaDong().signum() == 0) return 3;  // không phát sinh
		if (cn.getSoTienConLai().signum() == 0)                   return 2;  // đã đóng đủ
		if (cn.getSoTienDaDong().signum() > 0)                    return 1;  // đóng một phần
		return 0;                                                            // chưa đóng
	}
}