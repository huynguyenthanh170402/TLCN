package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHocPhanRepository;

/**
 * Phòng Đào tạo xác nhận hoặc từ chối lượt đăng ký học phần của sinh viên.
 *
 * Sinh viên bấm đăng ký là chỗ trong lớp được giữ ngay, nhưng lượt đăng ký
 * chưa có hiệu lực cho tới khi Phòng Đào tạo xác nhận. Giữ chỗ trước rồi mới
 * duyệt, chứ không phải duyệt xong mới giữ chỗ: nếu chờ duyệt mới tính sĩ số
 * thì trong lúc chờ, một lớp bốn mươi chỗ có thể nhận hai trăm lượt đăng ký,
 * đến lúc duyệt mới biết phải loại ai.
 *
 * Từ chối thì trả chỗ về cho lớp và gỡ luôn khoản học phí tương ứng, nên
 * trangThai quay về 2 (đã hủy) giống như sinh viên tự hủy. Cái phân biệt hai
 * trường hợp là trangThaiDuyet: tự hủy thì nó vẫn là 0 hoặc 1, bị từ chối thì
 * là 2 kèm lý do.
 */
@Service
@RequiredArgsConstructor
public class DuyetDangKyService {

	public static final int CHO_DUYET = 0;
	public static final int DA_DUYET = 1;
	public static final int TU_CHOI = 2;

	private static final int DK_CON_HIEU_LUC = 1;
	private static final int DK_DA_HUY = 2;

	private final DangKyHocPhanRepository dangKyRepository;
	private final HocPhiService hocPhiService;

	/**
	 * Sĩ số của một lớp học phần, nhìn từ màn hình duyệt.
	 *
	 * Tách "đã xác nhận" và "chờ xác nhận" vì hai con số trả lời hai câu khác
	 * nhau. Đã xác nhận là số sinh viên chắc chắn ngồi trong lớp, dùng để xét
	 * còn nhận thêm được không. Chờ xác nhận là phần chuyên viên đang cầm trên
	 * tay — nhìn vào đó mới biết duyệt hết chỗ này thì lớp có vỡ trần không.
	 */
	@Getter
	public static class SiSoLop {
		private final String maLop;
		private final int tran;
		private int daXacNhan;
		private int choXacNhan;

		SiSoLop(String maLop, int tran) { this.maLop = maLop; this.tran = tran; }

		public int getTong()    { return daXacNhan + choXacNhan; }
		public int getConLai()  { return Math.max(0, tran - daXacNhan); }

		/** Hết chỗ: xác nhận thêm một người nữa là vượt trần */
		public boolean isDay()  { return daXacNhan >= tran; }

		/** Số lượt chờ nhiều hơn chỗ trống, duyệt hết là vỡ trần */
		public boolean isVuot() { return getTong() > tran; }
	}

	/** Một ô trên màn hình chọn khoa */
	@Getter
	public static class TomTatKhoa {
		private final Integer maDonVi;
		private final String ten;
		private int soChoDuyet;
		private int soDaDuyet;
		private int soTuChoi;

		TomTatKhoa(Integer maDonVi, String ten) { this.maDonVi = maDonVi; this.ten = ten; }

		public boolean isXong() { return soChoDuyet == 0; }
	}

	// ===================== ĐỌC =====================

	/**
	 * Khoa quản lý sinh viên, chứ không phải khoa phụ trách học phần.
	 *
	 * Khác với duyệt điểm. Duyệt điểm là việc của khoa dạy môn đó, vì người
	 * chấm là giảng viên của khoa. Còn xác nhận đăng ký là xét xem sinh viên
	 * này học như vậy có đúng chương trình của ngành mình không, nên phải gom
	 * theo khoa đang quản lý sinh viên. Một sinh viên Cơ khí đăng ký môn đại
	 * cương vẫn là việc của chuyên viên phụ trách khoa Cơ khí.
	 */
	public static Integer maKhoa(DangKyHocPhan dk) {
		SinhVien sv = dk.getSinhVien();
		if (sv.getNganh() == null || sv.getNganh().getDonVi() == null) return -1;
		return sv.getNganh().getDonVi().getMaDonVi();
	}

	public static String tenKhoa(DangKyHocPhan dk) {
		SinhVien sv = dk.getSinhVien();
		if (sv.getNganh() == null || sv.getNganh().getDonVi() == null)
			return "Chưa rõ khoa quản lý";
		return sv.getNganh().getDonVi().getTenDonVi();
	}

	/** Lượt đăng ký này đang ở trạng thái xác nhận nào */
	private static int trangThaiCua(DangKyHocPhan dk) {
		if (dk.isTuChoi()) return TU_CHOI;
		// Sinh viên tự hủy thì không còn là việc của Phòng Đào tạo nữa
		if (dk.getTrangThai() != DK_CON_HIEU_LUC) return -1;
		return dk.isChoDuyet() ? CHO_DUYET : DA_DUYET;
	}

	/**
	 * @param tt       CHO_DUYET, DA_DUYET hoặc TU_CHOI
	 * @param maDonVi  null thì lấy cả trường
	 */
	@Transactional(readOnly = true)
	public List<DangKyHocPhan> ds(Integer maHocKy, int tt, Integer maDonVi) {
		if (maHocKy == null) return List.of();
		return dangKyRepository.dsCuaHocKy(maHocKy).stream()
				.filter(dk -> trangThaiCua(dk) == tt)
				.filter(dk -> maDonVi == null || maDonVi.equals(maKhoa(dk)))
				.toList();
	}

	/**
	 * Sĩ số mọi lớp có người đăng ký trong học kỳ.
	 *
	 * Đếm ngay trên danh sách vừa nạp chứ không bắn một truy vấn COUNT cho mỗi
	 * lớp: bảng tám chục dòng mà hỏi riêng từng lớp thì thành tám chục lượt
	 * gọi cơ sở dữ liệu cho một lần mở trang.
	 */
	@Transactional(readOnly = true)
	public Map<Integer, SiSoLop> siSoCacLop(Integer maHocKy) {
		Map<Integer, SiSoLop> kq = new HashMap<>();
		if (maHocKy == null) return kq;

		for (DangKyHocPhan dk : dangKyRepository.dsCuaHocKy(maHocKy)) {
			// Chỉ lượt còn giữ chỗ mới chiếm một suất trong lớp
			if (dk.getTrangThai() != DK_CON_HIEU_LUC) continue;

			var lop = dk.getLopHocPhan();
			SiSoLop s = kq.computeIfAbsent(lop.getMaLopHocPhan(),
					k -> new SiSoLop(lop.getMaLop(), DangKyHocPhanService.tranSiSo(lop)));

			if (dk.isChoDuyet()) s.choXacNhan++;
			else s.daXacNhan++;
		}
		return kq;
	}

	/** Số liệu cho màn hình chọn khoa */
	@Transactional(readOnly = true)
	public List<TomTatKhoa> tomTatTheoKhoa(Integer maHocKy) {
		Map<Integer, TomTatKhoa> theoKhoa = new HashMap<>();
		if (maHocKy == null) return List.of();

		for (DangKyHocPhan dk : dangKyRepository.dsCuaHocKy(maHocKy)) {
			int tt = trangThaiCua(dk);
			if (tt < 0) continue;

			TomTatKhoa t = theoKhoa.computeIfAbsent(maKhoa(dk),
					k -> new TomTatKhoa(k, tenKhoa(dk)));
			switch (tt) {
				case CHO_DUYET -> t.soChoDuyet++;
				case DA_DUYET  -> t.soDaDuyet++;
				case TU_CHOI   -> t.soTuChoi++;
				default -> { }
			}
		}

		List<TomTatKhoa> kq = new ArrayList<>(theoKhoa.values());
		// Khoa còn việc tồn xếp lên trước, chuyên viên mở trang là thấy ngay
		kq.sort((a, b) -> {
			int c = Integer.compare(b.getSoChoDuyet(), a.getSoChoDuyet());
			return c != 0 ? c : a.getTen().compareTo(b.getTen());
		});
		return kq;
	}

	// ===================== THAO TÁC =====================

	@Transactional
	public void duyet(Integer maDangKy, String nguoiDuyet) {
		DangKyHocPhan dk = tim(maDangKy);

		if (!dk.isChoDuyet())
			throw new IllegalStateException("Lượt đăng ký này không còn chờ xác nhận.");
		if (dk.getTrangThai() != DK_CON_HIEU_LUC)
			throw new IllegalStateException("Sinh viên đã hủy lượt đăng ký này.");

		String loiSiSo = loiNeuDayLop(dk);
		if (loiSiSo != null) throw new IllegalStateException(loiSiSo);

		dk.setTrangThaiDuyet(DA_DUYET);
		dk.setNgayDuyet(LocalDateTime.now());
		dk.setNguoiDuyet(nguoiDuyet);
		dk.setLyDoTuChoi(null);
		dangKyRepository.saveAndFlush(dk);
	}

	@Transactional
	public void tuChoi(Integer maDangKy, String lyDo, String nguoiDuyet) {
		if (lyDo == null || lyDo.isBlank())
			throw new IllegalStateException("Nhập lý do từ chối để sinh viên biết vì sao.");

		DangKyHocPhan dk = tim(maDangKy);
		if (!dk.isChoDuyet())
			throw new IllegalStateException("Lượt đăng ký này không còn chờ xác nhận.");

		dk.setTrangThaiDuyet(TU_CHOI);
		dk.setNgayDuyet(LocalDateTime.now());
		dk.setNguoiDuyet(nguoiDuyet);
		dk.setLyDoTuChoi(lyDo.trim());

		// Trả chỗ về cho lớp. Để nguyên trangThai = 1 thì lớp vẫn coi như đầy
		// một suất, và sinh viên vẫn bị tính học phí của môn mình không được học.
		dk.setTrangThai(DK_DA_HUY);
		dk.setNgayHuy(LocalDateTime.now());
		dk.setLyDoHuy("Phòng Đào tạo không duyệt: " + lyDo.trim());
		dangKyRepository.saveAndFlush(dk);

		HocKy hk = dk.getLopHocPhan().getHocKy();
		hocPhiService.capNhatCongNo(dk.getSinhVien(), hk);
	}

	/** Kết quả một lượt duyệt hàng loạt, để màn hình nói đúng chuyện vừa xảy ra */
	public record KetQuaHangLoat(int soDuyet, int soDayLop, int soBoQua) {
		public int getTong() { return soDuyet + soDayLop + soBoQua; }
	}

	/**
	 * Xác nhận nhiều lượt một lúc.
	 *
	 * Bỏ qua lặng lẽ những dòng không còn chờ xác nhận thay vì ném lỗi: danh
	 * sách tick chọn có thể đã cũ vài phút, sinh viên vừa tự hủy một dòng mà
	 * làm hỏng cả lượt duyệt tám chục dòng còn lại thì vô lý. Nhưng có đếm lại
	 * và báo số, chứ không để chuyên viên tưởng đã duyệt hết.
	 *
	 * Sĩ số đếm dồn trong bộ nhớ chứ không hỏi lại cơ sở dữ liệu sau mỗi dòng:
	 * tick chọn 40 sinh viên của cùng một lớp thì dòng thứ 51 phải bị chặn,
	 * mà hỏi lại từng lần vừa chậm vừa dễ lệch nếu giao dịch chưa ghi xong.
	 */
	@Transactional
	public KetQuaHangLoat duyetHangLoat(List<Integer> dsMa, String nguoiDuyet) {
		if (dsMa == null || dsMa.isEmpty()) return new KetQuaHangLoat(0, 0, 0);

		int soDuyet = 0, soDayLop = 0, soBoQua = 0;
		Map<Integer, Integer> daXacNhan = new HashMap<>();   // mã lớp → sĩ số đang có

		for (Integer ma : dsMa) {
			DangKyHocPhan dk = dangKyRepository.findById(ma).orElse(null);
			if (dk == null || !dk.isChoDuyet() || dk.getTrangThai() != DK_CON_HIEU_LUC) {
				soBoQua++;
				continue;
			}

			var lop = dk.getLopHocPhan();
			int tran = DangKyHocPhanService.tranSiSo(lop);
			int hienTai = daXacNhan.computeIfAbsent(lop.getMaLopHocPhan(),
					k -> (int) dangKyRepository.demDaXacNhan(k));

			if (hienTai >= tran) {
				soDayLop++;
				continue;
			}

			dk.setTrangThaiDuyet(DA_DUYET);
			dk.setNgayDuyet(LocalDateTime.now());
			dk.setNguoiDuyet(nguoiDuyet);
			dk.setLyDoTuChoi(null);
			dangKyRepository.save(dk);

			daXacNhan.put(lop.getMaLopHocPhan(), hienTai + 1);
			soDuyet++;
		}
		dangKyRepository.flush();
		return new KetQuaHangLoat(soDuyet, soDayLop, soBoQua);
	}

	/**
	 * Lớp đã đủ sĩ số xác nhận hay chưa, null nghĩa là còn nhận được.
	 *
	 * Kiểm lại ở bước duyệt chứ không tin vào lần kiểm lúc sinh viên bấm đăng
	 * ký. Lúc đó con số đếm cả lượt đang chờ, mà những lượt chờ ấy có thể bị
	 * từ chối hoặc sinh viên tự hủy, nên tới đây sĩ số đã khác. Quan trọng hơn:
	 * trần cứng 50 là ranh giới cuối, đã là ranh giới cuối thì phải chặn ngay
	 * chỗ ghi dữ liệu, không chặn bằng cách ẩn nút trên màn hình.
	 */
	private String loiNeuDayLop(DangKyHocPhan dk) {
		var lop = dk.getLopHocPhan();
		int tran = DangKyHocPhanService.tranSiSo(lop);
		long daXacNhan = dangKyRepository.demDaXacNhan(lop.getMaLopHocPhan());

		if (daXacNhan < tran) return null;

		return "Lớp " + lop.getMaLop() + " đã đủ " + daXacNhan + "/" + tran
				+ " sinh viên được xác nhận, không nhận thêm được. "
				+ "Mở thêm lớp hoặc hướng dẫn sinh viên chọn lớp khác.";
	}

	private DangKyHocPhan tim(Integer maDangKy) {
		return dangKyRepository.findById(maDangKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt đăng ký."));
	}
}
