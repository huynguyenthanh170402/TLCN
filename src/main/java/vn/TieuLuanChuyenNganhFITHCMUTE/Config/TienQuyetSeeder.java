package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhanTienQuyet;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanTienQuyetRepository;

/**
 * Sinh điều kiện tiên quyết từ tên học phần.
 * Môn có đuôi "2" cần môn cùng tên đuôi "1"; Anh văn 4 cần Anh văn 3;
 * khóa luận cần thực tập doanh nghiệp.
 */
@Component
@Order(8)
@RequiredArgsConstructor
@Slf4j
public class TienQuyetSeeder implements CommandLineRunner {

	private static final int TIEN_QUYET = 1;
	private static final int HOC_TRUOC = 2;

	/** Các chuỗi môn nối tiếp nhau: tên môn sau → tên môn trước */
	private static final String[][] NOI_TIEP_CO_DINH = {
		{"Anh văn 2", "Anh văn 1"},
		{"Anh văn 3", "Anh văn 2"},
		{"Anh văn 4", "Anh văn 3"},
		{"Toán cao cấp A2", "Toán cao cấp A1"},
		{"Toán cao cấp A3", "Toán cao cấp A2"},
		{"Xác suất thống kê ứng dụng", "Toán cao cấp A1"},
		{"Giáo dục thể chất 2", "Giáo dục thể chất 1"},
		{"Kinh tế chính trị Mác - Lênin", "Triết học Mác - Lênin"},
		{"Chủ nghĩa xã hội khoa học", "Kinh tế chính trị Mác - Lênin"},
		{"Tư tưởng Hồ Chí Minh", "Chủ nghĩa xã hội khoa học"},
		{"Lịch sử Đảng Cộng sản Việt Nam", "Chủ nghĩa xã hội khoa học"},
	};

	/** Với môn theo ngành: hậu tố " 2" cần hậu tố " 1" */
	private static final String[] CHUOI_THEO_NGANH = {
		"Cơ sở", "Thực hành", "Chuyên đề", "Đồ án"
	};

	private final HocPhanRepository hocPhanRepository;
	private final HocPhanTienQuyetRepository tienQuyetRepository;

	@Override
	@Transactional
	public void run(String... args) {
		if (tienQuyetRepository.count() > 0) return;

		List<HocPhan> tatCa = hocPhanRepository.findAll();
		if (tatCa.isEmpty()) return;

		Map<String, HocPhan> theoTen = new HashMap<>();
		for (HocPhan hp : tatCa) theoTen.put(hp.getTenHocPhan().trim(), hp);

		int soTao = 0;

		// 1. Các chuỗi cố định của khối đại cương
		for (String[] cap : NOI_TIEP_CO_DINH) {
			HocPhan sau = theoTen.get(cap[0]);
			HocPhan truoc = theoTen.get(cap[1]);
			if (sau != null && truoc != null && luu(sau, truoc, TIEN_QUYET)) soTao++;
		}

		// 2. Môn theo ngành: "... 2" cần "... 1"
		for (HocPhan hp : tatCa) {
			String ten = hp.getTenHocPhan().trim();
			if (!ten.endsWith(" 2")) continue;

			boolean thuocChuoi = false;
			for (String tienTo : CHUOI_THEO_NGANH)
				if (ten.startsWith(tienTo + " ")) { thuocChuoi = true; break; }
			if (!thuocChuoi) continue;

			HocPhan truoc = theoTen.get(ten.substring(0, ten.length() - 1) + "1");
			if (truoc != null && luu(hp, truoc, TIEN_QUYET)) soTao++;
		}

		// 3. Khóa luận tốt nghiệp cần thực tập doanh nghiệp của cùng ngành
		for (HocPhan hp : tatCa) {
			String ten = hp.getTenHocPhan().trim();
			if (!ten.startsWith("Khóa luận tốt nghiệp ngành ")) continue;

			String tenNganh = ten.substring("Khóa luận tốt nghiệp ngành ".length());
			HocPhan thucTap = theoTen.get("Thực tập doanh nghiệp ngành " + tenNganh);
			HocPhan nhapMon = theoTen.get("Nhập môn ngành " + tenNganh);

			if (thucTap != null && luu(hp, thucTap, HOC_TRUOC)) soTao++;
			if (nhapMon != null && luu(hp, nhapMon, TIEN_QUYET)) soTao++;
		}

		log.info("Đã tạo {} điều kiện tiên quyết cho các học phần.", soTao);
	}

	private boolean luu(HocPhan hocPhan, HocPhan dieuKien, int loai) {
		if (hocPhan.getMaHocPhanId().equals(dieuKien.getMaHocPhanId())) return false;
		if (tienQuyetRepository.existsByHocPhan_MaHocPhanIdAndHocPhanDieuKien_MaHocPhanId(
				hocPhan.getMaHocPhanId(), dieuKien.getMaHocPhanId())) return false;

		HocPhanTienQuyet tq = new HocPhanTienQuyet();
		tq.setHocPhan(hocPhan);
		tq.setHocPhanDieuKien(dieuKien);
		tq.setLoaiDieuKien(loai);
		tq.setTrangThai(1);
		tienQuyetRepository.save(tq);
		return true;
	}
}