package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;

/** Chương trình đào tạo: xem theo ngành và nhập từ file Excel */
@Service
@RequiredArgsConstructor
public class ChuongTrinhService {

	private static final String[] TIEU_DE = {
		"Tên ngành", "Mã học phần", "Tên học phần", "Số tín chỉ",
		"Khoa phụ trách", "Học kỳ dự kiến", "Khối kiến thức", "Loại học phần"
	};

	private final EntityManager em;
	private final NganhRepository nganhRepository;
	private final HocPhanRepository hocPhanRepository;
	private final DonViRepository donViRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;

	/** Một dòng trong bảng tổng quan các ngành */
	public record NganhThongKe(Integer maNganh, String tenNganh, String tenKhoa,
	                           long soHocPhan, long tongTinChi) {}

	@Getter
	public static class KetQuaImport {
		private int tongDong;
		private int thanhCong;
		private int hocPhanMoi;
		private int boQuaTrung;
		private final List<String> loi = new ArrayList<>();

		public int getSoLoi() { return loi.size(); }
	}

	// ===================== XEM =====================

	@Transactional(readOnly = true)
	public List<NganhThongKe> tongQuanCacNganh() {
		return em.createQuery("""
				SELECT n.maNganh, n.tenNganh, n.donVi.tenDonVi,
				       COUNT(ct), COALESCE(SUM(ct.hocPhan.soTinChi), 0)
				FROM Nganh n LEFT JOIN ChuongTrinhDaoTao ct
				       ON ct.nganh = n AND ct.trangThai = 1
				GROUP BY n.maNganh, n.tenNganh, n.donVi.tenDonVi
				ORDER BY n.donVi.tenDonVi, n.tenNganh
				""", Object[].class)
				.getResultList().stream()
				.map(r -> new NganhThongKe((Integer) r[0], (String) r[1], (String) r[2],
						((Number) r[3]).longValue(), ((Number) r[4]).longValue()))
				.toList();
	}

	@Transactional(readOnly = true)
	public List<ChuongTrinhDaoTao> chiTiet(Integer maNganh) {
		return chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
				maNganh, 1, Sort.by("hocKyDuKien", "hocPhan.maHocPhan"));
	}

	// ===================== NHẬP EXCEL =====================

	@Transactional
	public KetQuaImport nhapTuFile(MultipartFile file) {
		KetQuaImport kq = new KetQuaImport();

		if (file == null || file.isEmpty())
			throw new IllegalStateException("Chưa chọn file Excel.");
		String ten = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
		if (!ten.endsWith(".xlsx"))
			throw new IllegalStateException("Chỉ nhận file .xlsx.");

		// Cặp ngành + học phần đã gặp trong chính file này
		Set<String> daGapTrongFile = new HashSet<>();

		try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
			Sheet sheet = wb.getSheetAt(0);

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null || laDongTrong(row)) continue;

				kq.tongDong++;
				try {
					docMotDong(row, kq, daGapTrongFile);
				} catch (IllegalStateException e) {
					kq.getLoi().add("Dòng " + (i + 1) + ": " + e.getMessage());
				}
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Không đọc được file Excel: " + e.getMessage());
		}

		if (kq.getTongDong() == 0)
			throw new IllegalStateException("File không có dòng dữ liệu nào.");
		return kq;
	}

	private void docMotDong(Row row, KetQuaImport kq, Set<String> daGapTrongFile) {
		String tenNganh = chuoi(row.getCell(0));
		String maHp     = chuoi(row.getCell(1));
		String tenHp    = chuoi(row.getCell(2));
		String tinChiS  = chuoi(row.getCell(3));
		String tenKhoa  = chuoi(row.getCell(4));
		String hocKyS   = chuoi(row.getCell(5));
		String khoiS    = chuoi(row.getCell(6));
		String loaiS    = chuoi(row.getCell(7));

		if (tenNganh.isBlank()) throw new IllegalStateException("thiếu tên ngành.");
		if (maHp.isBlank())     throw new IllegalStateException("thiếu mã học phần.");
		if (tenHp.isBlank())    throw new IllegalStateException("thiếu tên học phần.");

		int tinChi = soNguyen(tinChiS, "số tín chỉ");
		if (tinChi < 1 || tinChi > 15)
			throw new IllegalStateException("số tín chỉ phải từ 1 đến 15.");

		int hocKy = hocKyS.isBlank() ? 1 : soNguyen(hocKyS, "học kỳ dự kiến");
		if (hocKy < 1 || hocKy > 8)
			throw new IllegalStateException("học kỳ dự kiến phải từ 1 đến 8.");

		Nganh nganh = nganhRepository.findByTenNganh(tenNganh)
				.orElseThrow(() -> new IllegalStateException("không tìm thấy ngành \"" + tenNganh + "\"."));

		if (!daGapTrongFile.add(nganh.getMaNganh() + "|" + maHp))
			throw new IllegalStateException("cặp ngành và học phần " + maHp + " bị lặp trong file.");

		// --- Học phần: có rồi thì dùng lại, chưa có thì tạo mới ---
		HocPhan hp = hocPhanRepository.findByMaHocPhan(maHp).orElse(null);
		if (hp == null) {
			DonVi donVi = tenKhoa.isBlank() ? nganh.getDonVi()
					: donViRepository.findByTenDonVi(tenKhoa).orElse(nganh.getDonVi());

			hp = new HocPhan();
			hp.setMaHocPhan(maHp);
			hp.setTenHocPhan(tenHp);
			hp.setSoTinChi(tinChi);
			hp.setSoTietLyThuyet(tinChi * 15);
			hp.setSoTietThucHanh(tinChi >= 3 ? 30 : 15);
			hp.setDonVi(donVi);
			hp.setTrangThai(1);
			hp = hocPhanRepository.save(hp);
			kq.hocPhanMoi++;
		}

		// --- Dòng chương trình đào tạo ---
		if (chuongTrinhRepository.existsByNganh_MaNganhAndHocPhan_MaHocPhanId(
				nganh.getMaNganh(), hp.getMaHocPhanId())) {
			kq.boQuaTrung++;
			return;
		}

		ChuongTrinhDaoTao ct = new ChuongTrinhDaoTao();
		ct.setNganh(nganh);
		ct.setHocPhan(hp);
		ct.setHocKyDuKien(hocKy);
		ct.setKhoiKienThuc(doiKhoi(khoiS));
		ct.setLoaiHocPhan(loaiS.toLowerCase().startsWith("tự") ? 2 : 1);
		ct.setTrangThai(1);
		chuongTrinhRepository.save(ct);
		kq.thanhCong++;
	}

	// ===================== FILE MẪU =====================

	public byte[] taoFileMau() {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("ChuongTrinhDaoTao");

			CellStyle kieuTieuDe = wb.createCellStyle();
			Font chuDam = wb.createFont();
			chuDam.setBold(true);
			kieuTieuDe.setFont(chuDam);

			Row tieuDe = sheet.createRow(0);
			for (int i = 0; i < TIEU_DE.length; i++) {
				Cell c = tieuDe.createCell(i);
				c.setCellValue(TIEU_DE[i]);
				c.setCellStyle(kieuTieuDe);
			}

			String tenNganhMau = nganhRepository.findAll().stream()
					.findFirst().map(Nganh::getTenNganh).orElse("Công nghệ thông tin");
			Object[][] mau = {
				{tenNganhMau, "LLCT130105", "Triết học Mác - Lênin", 3, "Khoa Chính trị và Luật", 1, "Đại cương", "Bắt buộc"},
				{tenNganhMau, "CNTT05",     "Kỹ thuật lập trình nâng cao", 3, "", 5, "Chuyên ngành", "Tự chọn"},
			};
			for (int r = 0; r < mau.length; r++) {
				Row row = sheet.createRow(r + 1);
				for (int c = 0; c < mau[r].length; c++) {
					Object v = mau[r][c];
					if (v instanceof Integer n) row.createCell(c).setCellValue(n);
					else row.createCell(c).setCellValue(String.valueOf(v));
				}
			}
			for (int i = 0; i < TIEU_DE.length; i++) sheet.autoSizeColumn(i);

			wb.write(out);
			return out.toByteArray();
		} catch (Exception e) {
			throw new IllegalStateException("Không tạo được file mẫu: " + e.getMessage());
		}
	}

	// ===================== ĐỌC Ô =====================

	private boolean laDongTrong(Row row) {
		for (int i = 0; i < TIEU_DE.length; i++) {
			if (!chuoi(row.getCell(i)).isBlank()) return false;
		}
		return true;
	}

	private String chuoi(Cell cell) {
		if (cell == null) return "";
		return switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue().trim();
			case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			case FORMULA -> cell.getCellFormula();
			default -> "";
		};
	}

	private int soNguyen(String s, String ten) {
		try {
			return Integer.parseInt(s.trim());
		} catch (Exception e) {
			throw new IllegalStateException(ten + " \"" + s + "\" không phải số.");
		}
	}

	/** 1 đại cương, 2 cơ sở ngành, 3 chuyên ngành, 4 tốt nghiệp */
	private int doiKhoi(String s) {
		String k = s == null ? "" : s.toLowerCase();
		if (k.startsWith("cơ sở") || k.startsWith("co so")) return 2;
		if (k.startsWith("chuyên") || k.startsWith("chuyen")) return 3;
		if (k.startsWith("tốt") || k.startsWith("tot")) return 4;
		return 1;
	}
}