package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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

	/**
	 * Giữ lại cho những chỗ gọi cũ. Đơn giá thật nay lấy từ bảng DonGiaTinChi
	 * qua DonGiaService, vì mỗi học kỳ và mỗi khóa có thể một giá khác nhau.
	 */
	@Deprecated
	public static final BigDecimal DON_GIA_TIN_CHI = DonGiaService.DON_GIA_MAC_DINH;

	/** Hạn đóng: 30 ngày sau ngày bắt đầu học kỳ */
	private static final int SO_NGAY_HAN_DONG = 30;

	private static final int DA_DANG_KY = 1;
	private static final BigDecimal MOT_TRAM = new BigDecimal("100");

	private final CongNoHocPhiRepository congNoRepository;
	private final DangKyHocPhanRepository dangKyRepository;
	private final DonGiaService donGiaService;
	private final MienGiamService mienGiamService;

	/**
	 * Tính lại công nợ của sinh viên trong một học kỳ.
	 *
	 * Ba yếu tố ghép lại: số tín chỉ đang đăng ký, đơn giá của học kỳ,
	 * và tỉ lệ miễn giảm mà Phòng Công tác Sinh viên đã duyệt.
	 *
	 * Gọi sau mỗi lần sinh viên đăng ký hoặc hủy học phần, và mỗi lần
	 * Phòng Kế hoạch Tài chính bấm tính lại cho cả học kỳ.
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

		BigDecimal donGia = donGiaService.timDonGia(hk.getMaHocKy(), sv.getKhoaTuyenSinh());
		BigDecimal phaiDong = donGia.multiply(BigDecimal.valueOf(tongTinChi));

		cn.setSoTienPhaiDong(phaiDong);
		cn.setSoTienMienGiam(tinhMienGiam(phaiDong, sv.getMaSinhVien(), hk.getMaHocKy()));
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

	/**
	 * Quy tỉ lệ miễn giảm ra số tiền.
	 *
	 * Tỉ lệ do Phòng Công tác Sinh viên duyệt, Tài chính chỉ đọc. Lưu tỉ lệ
	 * thay vì số tiền nên sinh viên rút bớt học phần thì số tiền giảm cũng
	 * tự co lại theo, không phải sửa quyết định.
	 *
	 * Làm tròn tới đồng, theo hướng có lợi cho người học.
	 */
	private BigDecimal tinhMienGiam(BigDecimal phaiDong, Integer maSinhVien, Integer maHocKy) {
		if (phaiDong.signum() == 0) return BigDecimal.ZERO;

		BigDecimal tyLe = mienGiamService.tongTyLeDaDuyet(maSinhVien, maHocKy);
		if (tyLe.signum() == 0) return BigDecimal.ZERO;

		return phaiDong.multiply(tyLe)
				.divide(MOT_TRAM, 0, RoundingMode.HALF_UP);
	}

	/**
	 * Tính lại trạng thái sau khi số tiền thay đổi.
	 * Để public vì khi thu tiền cũng cần gọi lại.
	 */
	public int tinhTrangThai(CongNoHocPhi cn, int tongTinChi) {
		if (tongTinChi == 0 && cn.getSoTienDaDong().signum() == 0) return 3;  // không phát sinh
		if (cn.getSoTienConLai().signum() == 0)                   return 2;  // đã đóng đủ
		if (cn.getSoTienDaDong().signum() > 0)                    return 1;  // đóng một phần
		return 0;                                                            // chưa đóng
	}
}
