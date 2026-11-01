package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;

/**
 * Lịch giảng dạy của giảng viên.
 *
 * Lịch được suy ra từ phân công: giảng viên dạy lớp nào thì nhận lịch của
 * lớp đó. Không có bảng lịch riêng cho giảng viên, nên Phòng Đào tạo đổi
 * giờ một lớp là thời khóa biểu của giảng viên tự đổi theo.
 */
@Service
@RequiredArgsConstructor
public class LichDayService {

	/** Học kỳ đang diễn ra */
	private static final int HOC_KY_DANG_DIEN_RA = 1;

	/** Vai trò giảng viên chính, người duy nhất được nhập điểm */
	private static final int VAI_TRO_CHINH = 1;

	private final NhapDiemService nhapDiemService;
	private final LichHocService lichHocService;

	// ===================== PHÂN CÔNG =====================

	/** Các học kỳ mà giảng viên có lớp, mới nhất trước — dùng cho ô chọn học kỳ */
	@Transactional(readOnly = true)
	public List<HocKy> dsHocKyCoLop(GiangVien gv) {
		return nhapDiemService.dsPhanCong(gv).stream()
				.map(pc -> pc.getLopHocPhan().getHocKy())
				.distinct()
				.sorted(Comparator.comparing(HocKy::getNgayBatDau,
						Comparator.nullsLast(Comparator.reverseOrder())))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<PhanCongGiangDay> phanCongTheoHocKy(GiangVien gv, Integer maHocKy) {
		List<PhanCongGiangDay> tatCa = nhapDiemService.dsPhanCong(gv);
		if (maHocKy == null) return tatCa;
		return tatCa.stream()
				.filter(pc -> maHocKy.equals(pc.getLopHocPhan().getHocKy().getMaHocKy()))
				.toList();
	}

	/**
	 * Học kỳ mặc định khi mở trang: học kỳ đang diễn ra nếu giảng viên có lớp
	 * trong đó, nếu không thì học kỳ gần nhất có lớp. Trả về null khi giảng
	 * viên chưa được phân công lớp nào.
	 */
	@Transactional(readOnly = true)
	public HocKy hocKyMacDinh(GiangVien gv) {
		List<HocKy> ds = dsHocKyCoLop(gv);
		if (ds.isEmpty()) return null;

		return ds.stream()
				.filter(hk -> HOC_KY_DANG_DIEN_RA == hk.getTrangThai())
				.findFirst()
				.orElse(ds.get(0));
	}

	/**
	 * Lớp mà giảng viên có quyền xem, không nhất thiết phải là giảng viên chính.
	 * Trợ giảng xem được danh sách sinh viên và lịch, nhưng không nhập điểm.
	 */
	@Transactional(readOnly = true)
	public PhanCongGiangDay phanCongCuaLop(GiangVien gv, Integer maLop) {
		return nhapDiemService.dsPhanCong(gv).stream()
				.filter(pc -> pc.getLopHocPhan().getMaLopHocPhan().equals(maLop))
				.findFirst()
				.orElseThrow(() -> new IllegalStateException(
						"Bạn không được phân công dạy lớp này."));
	}

	public boolean laGiangVienChinh(PhanCongGiangDay pc) {
		return pc != null && pc.getVaiTro() != null && pc.getVaiTro() == VAI_TRO_CHINH;
	}

	// ===================== LỊCH DẠY =====================

	/** Toàn bộ buổi dạy của một học kỳ, xếp theo thứ rồi theo tiết */
	@Transactional(readOnly = true)
	public List<LichHoc> lichDay(GiangVien gv, Integer maHocKy) {
		List<Integer> dsMaLop = phanCongTheoHocKy(gv, maHocKy).stream()
				.map(pc -> pc.getLopHocPhan().getMaLopHocPhan())
				.toList();
		if (dsMaLop.isEmpty()) return List.of();

		return lichHocService.lichTheoLop(dsMaLop).values().stream()
				.flatMap(List::stream)
				.sorted(Comparator.comparing(LichHoc::getThu)
						.thenComparing(LichHoc::getTietBatDau))
				.toList();
	}

	public LichHocService.ThoiKhoaBieu thoiKhoaBieu(List<LichHoc> dsLich) {
		return lichHocService.thoiKhoaBieu(dsLich);
	}

	/**
	 * Các buổi phải lên lớp hôm nay.
	 *
	 * Lọc theo thứ, rồi loại những buổi mà hôm nay nằm ngoài khoảng
	 * ngày bắt đầu và ngày kết thúc — lớp học hè hay lớp ghép giữa kỳ
	 * không chạy suốt học kỳ.
	 */
	@Transactional(readOnly = true)
	public List<LichHoc> buoiDayHomNay(GiangVien gv) {
		HocKy hk = hocKyMacDinh(gv);
		if (hk == null) return List.of();

		LocalDate homNay = LocalDate.now();
		int thuHomNay = thuCuaNgay(homNay);

		return lichDay(gv, hk.getMaHocKy()).stream()
				.filter(lh -> thuHomNay == lh.getThu())
				.filter(lh -> trongKhoang(lh, homNay))
				.sorted(Comparator.comparing(LichHoc::getTietBatDau))
				.toList();
	}

	/**
	 * Quy ước trong bảng LichHoc: 2 là thứ Hai ... 7 là thứ Bảy, 8 là Chủ nhật.
	 * Java đánh số thứ Hai là 1 và Chủ nhật là 7, nên phải dịch lại.
	 */
	private int thuCuaNgay(LocalDate ngay) {
		int javaThu = ngay.getDayOfWeek().getValue();   // 1 = Hai ... 7 = Chủ nhật
		return (javaThu == 7) ? 8 : javaThu + 1;
	}

	private boolean trongKhoang(LichHoc lh, LocalDate ngay) {
		if (lh.getNgayBatDau() != null && ngay.isBefore(lh.getNgayBatDau())) return false;
		if (lh.getNgayKetThuc() != null && ngay.isAfter(lh.getNgayKetThuc())) return false;
		return true;
	}

	// ===================== SỐ LIỆU TRANG CHỦ =====================

	/** Mấy con số hiện trên trang Tổng quan */
	public record TongQuan(int soLop, int soLopChinh, int soBuoiTrongTuan, long soBangDiemChuaXong) { }

	@Transactional(readOnly = true)
	public TongQuan tongQuan(GiangVien gv, Integer maHocKy) {
		List<PhanCongGiangDay> ds = phanCongTheoHocKy(gv, maHocKy);

		int soLopChinh = (int) ds.stream().filter(this::laGiangVienChinh).count();

		// Bảng điểm coi là chưa xong khi còn ở trạng thái chưa nhập hoặc đang nhập.
		// Chỉ tính các lớp mà giảng viên là người nhập điểm.
		long chuaXong = ds.stream()
				.filter(this::laGiangVienChinh)
				.filter(pc -> {
					String tt = nhapDiemService.tinhTrang(
							nhapDiemService.bangDiem(pc.getLopHocPhan().getMaLopHocPhan()));
					return tt.startsWith("Chưa nhập") || tt.startsWith("Đang nhập");
				})
				.count();

		return new TongQuan(ds.size(), soLopChinh, lichDay(gv, maHocKy).size(), chuaXong);
	}
}
