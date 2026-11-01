package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiayXacNhanSinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiayXacNhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;

/**
 * Cấp giấy xác nhận sinh viên.
 *
 * Giấy này đi ra ngoài trường nên mọi điều kiện phải xét ở đây, không để
 * giao diện tự quyết: chỉ cần gõ thẳng đường dẫn là qua mặt được giao diện.
 */
@Service
@RequiredArgsConstructor
public class GiayXacNhanService {

	/** Số dòng mỗi trang, khớp với các màn hình khác */
	public static final int SO_DONG_MOI_TRANG = 10;

	/** Thời hạn mặc định, tính bằng tháng */
	public static final int SO_THANG_MAC_DINH = 6;
	private static final int SO_THANG_TOI_DA = 12;

	private static final int CON_HIEU_LUC = 1;
	private static final int DA_THU_HOI = 0;

	private static final int SV_THOI_HOC = 0;
	private static final int SV_TOT_NGHIEP = 5;

	private static final DateTimeFormatter NGAY_SO = DateTimeFormatter.ofPattern("yyyyMMdd");

	private final GiayXacNhanRepository giayRepository;
	private final SinhVienRepository sinhVienRepository;

	// ===================== TRA CỨU =====================

	/**
	 * Không bao giờ truyền null xuống tham số của truy vấn.
	 *
	 * Cờ bật tắt đã lo việc bỏ qua bộ lọc, nhưng tham số thì vẫn được gán.
	 * Gán null thì trình điều khiển SQL Server không đoán được kiểu và báo
	 * "data types are incompatible", dù mệnh đề đó không hề chạy. Nên khi
	 * tắt bộ lọc thì truyền một giá trị vô hại cùng kiểu.
	 */
	@Transactional(readOnly = true)
	public Page<GiayXacNhanSinhVien> traCuu(String tuKhoa, Integer trangThai, int trang) {
		String tk = (tuKhoa == null) ? "" : tuKhoa.trim();
		return giayRepository.traCuu(
				trangThai == null ? 0 : 1, trangThai == null ? -1 : trangThai,
				tk.isEmpty() ? 0 : 1, tk,
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG));
	}

	@Transactional(readOnly = true)
	public GiayXacNhanSinhVien timChiTiet(Integer maGiay) {
		return giayRepository.timChiTiet(maGiay)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy giấy xác nhận."));
	}

	// ===================== CẤP GIẤY =====================

	@Transactional
	public GiayXacNhanSinhVien cap(Integer maSinhVien, Integer mucDich, String mucDichKhac,
	                               String noiNhan, Integer soThang, String nguoiCap) {

		SinhVien sv = sinhVienRepository.timTheoMa(maSinhVien)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy sinh viên."));

		kiemTraDuocCap(sv);

		if (mucDich == null || mucDich < 1 || mucDich > 6)
			throw new IllegalStateException("Chưa chọn mục đích sử dụng giấy.");

		// Chọn "khác" mà bỏ trống thì tờ giấy in ra không nói được dùng để
		// làm gì, nơi nhận không biết căn cứ vào đâu.
		if (mucDich == 6 && (mucDichKhac == null || mucDichKhac.isBlank()))
			throw new IllegalStateException("Chọn mục đích khác thì phải ghi rõ nội dung.");

		int thang = (soThang == null || soThang < 1) ? SO_THANG_MAC_DINH : soThang;
		if (thang > SO_THANG_TOI_DA)
			throw new IllegalStateException("Thời hạn tối đa là " + SO_THANG_TOI_DA + " tháng.");

		LocalDate homNay = LocalDate.now();

		GiayXacNhanSinhVien g = new GiayXacNhanSinhVien();
		g.setSoGiay(sinhSoGiay(homNay));
		g.setSinhVien(sv);
		g.setMucDich(mucDich);
		g.setMucDichKhac(mucDich == 6 ? mucDichKhac.trim() : null);
		g.setNoiNhan(noiNhan == null || noiNhan.isBlank() ? null : noiNhan.trim());
		g.setNgayCap(homNay);
		g.setHanSuDung(homNay.plusMonths(thang));

		// Chụp lại hai thông tin dễ đổi, để bản in lại luôn khớp bản đã ký
		g.setLopLucCap(tenLop(sv));
		g.setTrangThaiLucCap(sv.getTrangThai());

		g.setNguoiCap(nguoiCap);
		g.setTrangThai(CON_HIEU_LUC);

		return giayRepository.saveAndFlush(g);
	}

	/**
	 * Trường chỉ xác nhận cho người đang là sinh viên của trường.
	 *
	 * Người đã thôi học hoặc đã tốt nghiệp không còn là sinh viên, cấp giấy
	 * ghi "hiện đang học" cho họ là xác nhận sai sự thật. Người tốt nghiệp
	 * cần bản sao bằng hoặc giấy chứng nhận tốt nghiệp, là giấy tờ khác.
	 *
	 * Bảo lưu, tạm nghỉ và đình chỉ vẫn cấp được, vì họ còn tên trong danh
	 * sách sinh viên; trạng thái thật được in thẳng lên giấy để nơi nhận tự
	 * đánh giá.
	 */
	private void kiemTraDuocCap(SinhVien sv) {
		Integer tt = sv.getTrangThai();
		if (tt == null) return;

		if (tt == SV_THOI_HOC)
			throw new IllegalStateException(
					"Sinh viên đã thôi học nên không cấp giấy xác nhận sinh viên được.");

		if (tt == SV_TOT_NGHIEP)
			throw new IllegalStateException(
					"Sinh viên đã tốt nghiệp. Trường hợp này cấp giấy chứng nhận tốt nghiệp "
					+ "hoặc bản sao bằng, không dùng giấy xác nhận sinh viên.");
	}

	// ===================== THU HỒI =====================

	@Transactional
	public void thuHoi(Integer maGiay, String lyDo) {
		GiayXacNhanSinhVien g = giayRepository.findById(maGiay)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy giấy xác nhận."));

		if (Integer.valueOf(DA_THU_HOI).equals(g.getTrangThai()))
			throw new IllegalStateException("Giấy này đã thu hồi trước đó.");

		if (lyDo == null || lyDo.isBlank())
			throw new IllegalStateException("Phải ghi lý do thu hồi.");

		g.setTrangThai(DA_THU_HOI);
		g.setLyDoThuHoi(lyDo.trim());
		giayRepository.saveAndFlush(g);
	}

	// ===================== HỖ TRỢ =====================

	/**
	 * Số giấy dạng GXN20261027-0003: ngày cấp cộng số thứ tự trong ngày.
	 *
	 * Đếm theo ngày nên số ngắn, nhìn vào biết ngay cấp hôm nào. Có kiểm tra
	 * trùng rồi tăng tiếp, phòng trường hợp một giấy trong ngày đã bị xóa
	 * tay dưới cơ sở dữ liệu làm số đếm lệch khỏi số thứ tự thật.
	 */
	private String sinhSoGiay(LocalDate ngay) {
		String tien = "GXN" + ngay.format(NGAY_SO) + "-";
		long thuTu = giayRepository.countByNgayCap(ngay) + 1;

		String so = tien + String.format("%04d", thuTu);
		while (giayRepository.traCuu(0, -1, 1, so, PageRequest.of(0, 1)).getTotalElements() > 0) {
			thuTu++;
			so = tien + String.format("%04d", thuTu);
		}
		return so;
	}

	/**
	 * Tên lớp của sinh viên.
	 *
	 * Hệ thống chưa có bảng lớp sinh hoạt riêng, nên ghép từ khóa tuyển sinh
	 * và ngành — đúng cách các trường đặt tên lớp khi chưa chia nhóm. Khi nào
	 * có bảng lớp thật thì chỉ cần sửa trong hàm này.
	 */
	public String tenLop(SinhVien sv) {
		String nganh = (sv.getNganh() == null) ? null : sv.getNganh().getTenNganh();
		String khoa = sv.getKhoaTuyenSinh();

		if (nganh == null && khoa == null) return "";
		if (nganh == null) return "Khóa " + khoa;
		if (khoa == null) return nganh;
		return nganh + " — Khóa " + khoa;
	}
}
