package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHoatDong;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HoatDongCTXH;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DangKyHoatDongRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HoatDongCTXHRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;

/**
 * Công tác xã hội và điểm rèn luyện.
 *
 * Cách tính điểm ở đây là bản rút gọn của quy chế thật. Ngoài đời điểm rèn
 * luyện chấm trên năm tiêu chí, sinh viên tự chấm rồi lớp họp xét và khoa
 * duyệt. Hệ thống này chỉ làm phần đo được bằng dữ liệu: một mức điểm nền
 * cho sinh viên không vi phạm, cộng thêm điểm của các hoạt động đã thật sự
 * tham gia, chặn trần ở 100.
 *
 * Mọi điều kiện đều xét ở tầng này chứ không giao cho giao diện, vì chỉ cần
 * gõ thẳng đường dẫn là qua mặt được nút bấm.
 */
@Service
@RequiredArgsConstructor
public class RenLuyenService {

	/** Điểm khởi điểm của sinh viên không vi phạm, trước khi cộng hoạt động */
	public static final int DIEM_NEN = 60;
	public static final int DIEM_TOI_DA = 100;

	// Trạng thái hoạt động
	public static final int HD_NHAP = 0;
	public static final int HD_MO_DANG_KY = 1;
	public static final int HD_DA_CHOT = 2;
	public static final int HD_KET_THUC = 3;
	public static final int HD_DA_HUY = 4;

	// Trạng thái đăng ký
	public static final int DK_DA_DANG_KY = 1;
	public static final int DK_DA_THAM_GIA = 2;
	public static final int DK_VANG = 3;
	public static final int DK_DA_HUY = 4;

	private static final int SV_DANG_HOC = 1;

	private final HoatDongCTXHRepository hoatDongRepository;
	private final DangKyHoatDongRepository dangKyRepository;
	private final HocKyRepository hocKyRepository;
	private final SinhVienRepository sinhVienRepository;
	private final DangKyHocPhanService dangKyHocPhanService;

	// ===================== KIỂU DỮ LIỆU CHO GIAO DIỆN =====================

	/** Kết quả rèn luyện của một sinh viên trong một học kỳ */
	@Getter
	public static class KetQuaRenLuyen {
		private final HocKy hocKy;
		private final List<DangKyHoatDong> dsThamGia = new ArrayList<>();
		private int diemHoatDong;

		KetQuaRenLuyen(HocKy hocKy) { this.hocKy = hocKy; }

		public int getDiemNen()  { return DIEM_NEN; }
		public int getTongDiem() { return Math.min(DIEM_TOI_DA, DIEM_NEN + diemHoatDong); }

		/** Số hoạt động đã được xác nhận tham gia */
		public long getSoHoatDong() {
			return dsThamGia.stream().filter(DangKyHoatDong::isDuocCongDiem).count();
		}

		/** Có bị chặn trần 100 không, để giao diện nói rõ thay vì im lặng cắt điểm */
		public boolean isChamTran() { return DIEM_NEN + diemHoatDong > DIEM_TOI_DA; }

		public String getXepLoai()  { return RenLuyenService.xepLoai(getTongDiem()); }
		public String getMauXepLoai() { return RenLuyenService.mauXepLoai(getTongDiem()); }
	}

	/**
	 * Xếp loại theo thang điểm của Bộ Giáo dục và Đào tạo.
	 *
	 * Các mốc này là quy định chung, không phải con số tự đặt, nên để ở một
	 * chỗ duy nhất thay vì rải trong giao diện.
	 */
	public static String xepLoai(int diem) {
		if (diem >= 90) return "Xuất sắc";
		if (diem >= 80) return "Tốt";
		if (diem >= 65) return "Khá";
		if (diem >= 50) return "Trung bình";
		if (diem >= 35) return "Yếu";
		return "Kém";
	}

	public static String mauXepLoai(int diem) {
		if (diem >= 90) return "bg-success";
		if (diem >= 80) return "bg-primary";
		if (diem >= 65) return "bg-info text-dark";
		if (diem >= 50) return "bg-warning text-dark";
		return "bg-danger";
	}

	// ===================== ĐỌC DỮ LIỆU =====================

	@Transactional(readOnly = true)
	public List<HoatDongCTXH> dsHoatDong(Integer maHocKy) {
		return hoatDongRepository.findByHocKy_MaHocKy(maHocKy,
				Sort.by(Sort.Direction.DESC, "thoiGianBatDau"));
	}

	@Transactional(readOnly = true)
	public HoatDongCTXH timHoatDong(Integer maHoatDong) {
		return hoatDongRepository.findWithHocKyByMaHoatDong(maHoatDong)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy hoạt động."));
	}

	@Transactional(readOnly = true)
	public List<DangKyHoatDong> dsThamGia(Integer maHoatDong) {
		return dangKyRepository.dsTheoHoatDong(maHoatDong);
	}

	/** Số chỗ đã có người giữ, dùng để biết hoạt động còn nhận thêm được không */
	@Transactional(readOnly = true)
	public long soDaDangKy(Integer maHoatDong) {
		return hoatDongRepository.demDangGiuCho(maHoatDong);
	}

	@Transactional(readOnly = true)
	public Map<Integer, Long> soDaDangKyCacHoatDong(List<HoatDongCTXH> ds) {
		Map<Integer, Long> kq = new HashMap<>();
		for (HoatDongCTXH hd : ds) {
			kq.put(hd.getMaHoatDong(), hoatDongRepository.demDangGiuCho(hd.getMaHoatDong()));
		}
		return kq;
	}

	// ===================== PHÒNG CÔNG TÁC SINH VIÊN =====================

	@Transactional
	public HoatDongCTXH tao(Integer maHocKy, String tenHoatDong, String moTa,
	                        Integer loaiHoatDong, Integer diemCong,
	                        String donViToChuc, String diaDiem,
	                        LocalDateTime batDau, LocalDateTime ketThuc,
	                        LocalDateTime hanDangKy, Integer soLuongToiDa,
	                        String nguoiTao) {

		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));

		if (tenHoatDong == null || tenHoatDong.isBlank())
			throw new IllegalStateException("Chưa nhập tên hoạt động.");

		kiemTraThoiGian(batDau, ketThuc, hanDangKy);

		if (diemCong == null || diemCong < 1 || diemCong > 30)
			throw new IllegalStateException(
					"Điểm cộng phải từ 1 đến 30. Một hoạt động cộng quá nhiều sẽ làm "
					+ "điểm rèn luyện mất ý nghĩa so với các hoạt động khác.");

		if (soLuongToiDa != null && soLuongToiDa < 1)
			throw new IllegalStateException("Số lượng tối đa phải lớn hơn 0, "
					+ "hoặc để trống nếu không giới hạn.");

		HoatDongCTXH hd = new HoatDongCTXH();
		hd.setHocKy(hk);
		hd.setTenHoatDong(tenHoatDong.trim());
		hd.setMoTa(moTa == null || moTa.isBlank() ? null : moTa.trim());
		hd.setLoaiHoatDong(loaiHoatDong == null ? 1 : loaiHoatDong);
		hd.setDiemCong(diemCong);
		hd.setDonViToChuc(donViToChuc == null || donViToChuc.isBlank() ? null : donViToChuc.trim());
		hd.setDiaDiem(diaDiem == null || diaDiem.isBlank() ? null : diaDiem.trim());
		hd.setThoiGianBatDau(batDau);
		hd.setThoiGianKetThuc(ketThuc);
		hd.setHanDangKy(hanDangKy);
		hd.setSoLuongToiDa(soLuongToiDa);
		hd.setTrangThai(HD_NHAP);
		hd.setNguoiTao(nguoiTao);

		return hoatDongRepository.saveAndFlush(hd);
	}

	/**
	 * Hạn đăng ký phải trước lúc hoạt động bắt đầu.
	 *
	 * Cho đăng ký sau khi đã bắt đầu thì danh sách chốt lúc nào cũng sai, và
	 * người tổ chức không biết chuẩn bị bao nhiêu suất.
	 */
	private void kiemTraThoiGian(LocalDateTime batDau, LocalDateTime ketThuc,
	                             LocalDateTime hanDangKy) {
		if (batDau != null && ketThuc != null && !ketThuc.isAfter(batDau))
			throw new IllegalStateException("Thời gian kết thúc phải sau thời gian bắt đầu.");

		if (hanDangKy != null && batDau != null && hanDangKy.isAfter(batDau))
			throw new IllegalStateException(
					"Hạn đăng ký phải trước lúc hoạt động bắt đầu.");
	}

	/** Chuyển trạng thái hoạt động, chỉ theo đúng thứ tự của quy trình */
	@Transactional
	public void doiTrangThai(Integer maHoatDong, int trangThaiMoi) {
		HoatDongCTXH hd = timHoatDong(maHoatDong);
		int hienTai = hd.getTrangThai();

		if (hienTai == trangThaiMoi)
			throw new IllegalStateException("Hoạt động đang ở trạng thái này rồi.");

		boolean hopLe = switch (trangThaiMoi) {
			// Mở đăng ký: từ nháp, hoặc mở lại sau khi lỡ chốt sớm
			case HD_MO_DANG_KY -> hienTai == HD_NHAP || hienTai == HD_DA_CHOT;
			case HD_DA_CHOT    -> hienTai == HD_MO_DANG_KY;
			// Kết thúc: phải chốt danh sách trước, để không ai lọt vào sau khi đã điểm danh
			case HD_KET_THUC   -> hienTai == HD_DA_CHOT;
			case HD_DA_HUY     -> hienTai != HD_KET_THUC;
			default -> false;
		};

		if (!hopLe)
			throw new IllegalStateException("Không chuyển từ \"" + hd.getTenTrangThai()
					+ "\" sang trạng thái đó được.");

		// Đã kết thúc nghĩa là điểm đã chốt, không cho quay lui sửa nữa
		if (hienTai == HD_KET_THUC)
			throw new IllegalStateException(
					"Hoạt động đã kết thúc và điểm đã được tính, không đổi trạng thái nữa.");

		hd.setTrangThai(trangThaiMoi);
		hoatDongRepository.saveAndFlush(hd);
	}

	/**
	 * Điểm danh một sinh viên: có mặt hay vắng.
	 *
	 * Chỉ làm được sau khi hoạt động đã diễn ra xong. Điểm danh trước khi
	 * hoạt động kết thúc là ghi nhận một việc chưa xảy ra.
	 */
	@Transactional
	public void diemDanh(Integer maDangKy, boolean coMat, String nguoiDiemDanh) {
		DangKyHoatDong dk = dangKyRepository.findById(maDangKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt đăng ký."));

		HoatDongCTXH hd = dk.getHoatDong();

		if (hd.getTrangThai() == HD_DA_HUY)
			throw new IllegalStateException("Hoạt động đã hủy, không điểm danh được.");

		if (hd.getTrangThai() == HD_NHAP || hd.getTrangThai() == HD_MO_DANG_KY)
			throw new IllegalStateException(
					"Chốt danh sách trước rồi mới điểm danh được.");

		if (!hd.isDaDienRa())
			throw new IllegalStateException(
					"Hoạt động chưa diễn ra xong nên chưa điểm danh được.");

		if (Integer.valueOf(DK_DA_HUY).equals(dk.getTrangThai()))
			throw new IllegalStateException("Sinh viên này đã hủy đăng ký.");

		dk.setTrangThai(coMat ? DK_DA_THAM_GIA : DK_VANG);
		dk.setNguoiDiemDanh(nguoiDiemDanh);
		dk.setNgayDiemDanh(LocalDateTime.now());
		dangKyRepository.saveAndFlush(dk);
	}

	/** Điểm danh cả danh sách cùng lúc, cho những hoạt động đông người */
	@Transactional
	public int diemDanhHangLoat(Integer maHoatDong, List<Integer> dsCoMat, String nguoiDiemDanh) {
		HoatDongCTXH hd = timHoatDong(maHoatDong);

		if (hd.getTrangThai() == HD_NHAP || hd.getTrangThai() == HD_MO_DANG_KY)
			throw new IllegalStateException("Chốt danh sách trước rồi mới điểm danh được.");
		if (!hd.isDaDienRa())
			throw new IllegalStateException("Hoạt động chưa diễn ra xong nên chưa điểm danh được.");

		List<DangKyHoatDong> ds = dangKyRepository.dsTheoHoatDong(maHoatDong);
		LocalDateTime bayGio = LocalDateTime.now();
		int soDoi = 0;

		for (DangKyHoatDong dk : ds) {
			// Người đã tự hủy đăng ký thì không nằm trong diện điểm danh
			if (Integer.valueOf(DK_DA_HUY).equals(dk.getTrangThai())) continue;

			boolean coMat = dsCoMat != null && dsCoMat.contains(dk.getMaDangKyHoatDong());
			dk.setTrangThai(coMat ? DK_DA_THAM_GIA : DK_VANG);
			dk.setNguoiDiemDanh(nguoiDiemDanh);
			dk.setNgayDiemDanh(bayGio);
			soDoi++;
		}

		dangKyRepository.saveAll(ds);
		return soDoi;
	}

	/** Phòng Công tác Sinh viên thêm thẳng một sinh viên vào danh sách */
	@Transactional
	public void themSinhVien(Integer maHoatDong, String maSoSinhVien) {
		SinhVien sv = sinhVienRepository.timTheoMaSo(
				maSoSinhVien == null ? "" : maSoSinhVien.trim())
				.orElseThrow(() -> new IllegalStateException(
						"Không tìm thấy sinh viên có MSSV " + maSoSinhVien + "."));

		dangKyChoSinhVien(maHoatDong, sv, true);
	}

	// ===================== SINH VIÊN =====================

	@Transactional
	public void sinhVienDangKy(Integer maHoatDong, SinhVien sv) {
		dangKyChoSinhVien(maHoatDong, sv, false);
	}

	/**
	 * @param doCanBo true khi Phòng Công tác Sinh viên thêm hộ.
	 *
	 * Cán bộ được bỏ qua hạn đăng ký, vì thực tế hay có người tới tham gia
	 * trực tiếp mà chưa kịp đăng ký trên hệ thống. Nhưng giới hạn số lượng
	 * và các điều kiện còn lại thì vẫn giữ.
	 */
	private void dangKyChoSinhVien(Integer maHoatDong, SinhVien sv, boolean doCanBo) {
		HoatDongCTXH hd = timHoatDong(maHoatDong);

		if (sv == null)
			throw new IllegalStateException("Không xác định được sinh viên.");

		if (sv.getTrangThai() != null && sv.getTrangThai() != SV_DANG_HOC)
			throw new IllegalStateException("Sinh viên không ở trạng thái đang học "
					+ "nên không tham gia hoạt động được.");

		if (hd.getTrangThai() == HD_DA_HUY)
			throw new IllegalStateException("Hoạt động đã hủy.");

		if (!doCanBo && !hd.isDangMoDangKy())
			throw new IllegalStateException(hd.isHetHanDangKy()
					? "Đã quá hạn đăng ký hoạt động này."
					: "Hoạt động chưa mở đăng ký.");

		if (doCanBo && hd.getTrangThai() == HD_KET_THUC)
			throw new IllegalStateException(
					"Hoạt động đã kết thúc và điểm đã chốt, không thêm người được nữa.");

		/*
		 * Bảng có ràng buộc duy nhất trên cặp (hoạt động, sinh viên). Người
		 * từng hủy rồi đăng ký lại phải dùng lại dòng cũ, chèn dòng mới sẽ
		 * vướng ràng buộc và văng lỗi cơ sở dữ liệu ra màn hình.
		 */
		DangKyHoatDong dk = dangKyRepository
				.findByHoatDong_MaHoatDongAndSinhVien_MaSinhVien(maHoatDong, sv.getMaSinhVien())
				.orElseGet(DangKyHoatDong::new);

		if (dk.getMaDangKyHoatDong() != null
				&& !Integer.valueOf(DK_DA_HUY).equals(dk.getTrangThai()))
			throw new IllegalStateException(doCanBo
					? "Sinh viên này đã có trong danh sách."
					: "Bạn đã đăng ký hoạt động này rồi.");

		if (hd.getSoLuongToiDa() != null
				&& hoatDongRepository.demDangGiuCho(maHoatDong) >= hd.getSoLuongToiDa())
			throw new IllegalStateException("Hoạt động đã đủ "
					+ hd.getSoLuongToiDa() + " người đăng ký.");

		dk.setHoatDong(hd);
		dk.setSinhVien(sv);
		dk.setNgayDangKy(LocalDateTime.now());
		dk.setTrangThai(DK_DA_DANG_KY);
		dk.setNguoiDiemDanh(null);
		dk.setNgayDiemDanh(null);
		dangKyRepository.saveAndFlush(dk);
	}

	@Transactional
	public void sinhVienHuyDangKy(Integer maDangKy, SinhVien sv) {
		DangKyHoatDong dk = dangKyRepository.findById(maDangKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lượt đăng ký."));

		if (!dk.getSinhVien().getMaSinhVien().equals(sv.getMaSinhVien()))
			throw new IllegalStateException("Bạn không có quyền hủy đăng ký này.");

		if (!Integer.valueOf(DK_DA_DANG_KY).equals(dk.getTrangThai()))
			throw new IllegalStateException("Lượt đăng ký này không còn hủy được.");

		// Đã chốt danh sách thì người tổ chức đã chuẩn bị theo số đó
		if (!dk.getHoatDong().isDangMoDangKy())
			throw new IllegalStateException(
					"Đã quá hạn hoặc đã chốt danh sách, bạn liên hệ Phòng Công tác Sinh viên "
					+ "nếu không tham gia được.");

		dk.setTrangThai(DK_DA_HUY);
		dangKyRepository.saveAndFlush(dk);
	}

	// ===================== ĐIỂM RÈN LUYỆN =====================

	@Transactional(readOnly = true)
	public KetQuaRenLuyen ketQua(SinhVien sv, HocKy hk) {
		KetQuaRenLuyen kq = new KetQuaRenLuyen(hk);
		if (sv == null || hk == null) return kq;

		for (DangKyHoatDong dk : dangKyRepository.dsTheoSinhVienVaHocKy(
				sv.getMaSinhVien(), hk.getMaHocKy())) {
			kq.dsThamGia.add(dk);
			if (dk.isDuocCongDiem()) kq.diemHoatDong += dk.getHoatDong().getDiemCong();
		}
		return kq;
	}

	/** Kết quả của mọi học kỳ sinh viên có tham gia hoạt động */
	@Transactional(readOnly = true)
	public List<KetQuaRenLuyen> ketQuaMoiHocKy(SinhVien sv) {
		if (sv == null) return List.of();

		Map<Integer, KetQuaRenLuyen> theoHocKy = new java.util.LinkedHashMap<>();
		for (DangKyHoatDong dk : dangKyRepository.dsTheoSinhVien(sv.getMaSinhVien())) {
			HocKy hk = dk.getHoatDong().getHocKy();
			KetQuaRenLuyen kq = theoHocKy.computeIfAbsent(hk.getMaHocKy(),
					k -> new KetQuaRenLuyen(hk));
			kq.dsThamGia.add(dk);
			if (dk.isDuocCongDiem()) kq.diemHoatDong += dk.getHoatDong().getDiemCong();
		}
		return new ArrayList<>(theoHocKy.values());
	}

	/** Học kỳ đang diễn ra, dùng làm mặc định cho mọi màn hình */
	@Transactional(readOnly = true)
	public HocKy hocKyHienTai() {
		return dangKyHocPhanService.hocKyHienTai().orElse(null);
	}
}
