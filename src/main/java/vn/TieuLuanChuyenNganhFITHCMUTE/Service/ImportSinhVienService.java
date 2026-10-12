package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Quyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.QuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/**
 * Nhập danh sách sinh viên từ file Excel (.xlsx).
 * Mỗi dòng tạo một sinh viên kèm một tài khoản đăng nhập (tên đăng nhập = MSSV).
 */
@Service
@RequiredArgsConstructor
public class ImportSinhVienService {

	/** Mật khẩu mặc định cho tài khoản mới, sinh viên nên đổi sau lần đăng nhập đầu */
	public static final String MAT_KHAU_MAC_DINH = "123456";

	private static final int QUYEN_SINH_VIEN = 1;
	private static final int DONG_TIEU_DE = 0;

	private static final String[] TIEU_DE = {
		"MSSV", "Họ tên", "Ngày sinh (dd/MM/yyyy)", "Giới tính (Nam/Nữ)",
		"Tên ngành", "Khóa tuyển sinh", "Email trường", "Số điện thoại"
	};

	private static final DateTimeFormatter DINH_DANG_NGAY = DateTimeFormatter.ofPattern("d/M/yyyy");

	private final SinhVienRepository sinhVienRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final NganhRepository nganhRepository;
	private final QuyenRepository quyenRepository;
	private final PasswordEncoder passwordEncoder;

	/** Kết quả một lần nhập file */
	@Getter
	public static class KetQuaImport {
		private int tongDong;
		private int thanhCong;
		private final List<String> loi = new ArrayList<>();

		public int getSoLoi() { return loi.size(); }
	}

	// ===================== ĐỌC FILE =====================

	@Transactional
	public KetQuaImport nhapTuFile(MultipartFile file) {
		KetQuaImport kq = new KetQuaImport();

		if (file == null || file.isEmpty())
			throw new IllegalStateException("Chưa chọn file Excel.");
		String ten = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
		if (!ten.endsWith(".xlsx"))
			throw new IllegalStateException("Chỉ nhận file .xlsx. Nếu file là .xls, hãy lưu lại dạng .xlsx.");

		Quyen quyenSv = quyenRepository.findById(QUYEN_SINH_VIEN)
				.orElseThrow(() -> new IllegalStateException("Chưa có quyền Sinh viên trong hệ thống."));

		// MSSV đã xuất hiện trong chính file này, để bắt trùng ngay trong file
		Set<String> mssvTrongFile = new HashSet<>();

		// BCrypt chạy chậm có chủ đích (~0,1 giây/lần). Mọi tài khoản dùng chung
		// một mật khẩu mặc định nên chỉ mã hóa một lần rồi dùng lại cho cả file.
		String matKhauBam = passwordEncoder.encode(MAT_KHAU_MAC_DINH);

		try (InputStream in = file.getInputStream(); Workbook wb = new XSSFWorkbook(in)) {
			Sheet sheet = wb.getSheetAt(0);

			for (int i = DONG_TIEU_DE + 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null || laDongTrong(row)) continue;

				kq.tongDong++;
				int soDongHienThi = i + 1;   // Excel đếm từ 1
				try {
					docMotDong(row, quyenSv, matKhauBam, mssvTrongFile);
					kq.thanhCong++;
				} catch (IllegalStateException e) {
					kq.getLoi().add("Dòng " + soDongHienThi + ": " + e.getMessage());
				}
			}
		} catch (IllegalStateException e) {
			throw e;
		} catch (Exception e) {
			throw new IllegalStateException("Không đọc được file Excel: " + e.getMessage());
		}

		if (kq.getTongDong() == 0)
			throw new IllegalStateException("File không có dòng dữ liệu nào (dữ liệu bắt đầu từ dòng 2).");

		return kq;
	}

	private void docMotDong(Row row, Quyen quyenSv, String matKhauBam, Set<String> mssvTrongFile) {
		String mssv     = chuoi(row.getCell(0));
		String hoTen    = chuoi(row.getCell(1));
		LocalDate sinh  = ngay(row.getCell(2));
		String gioiTinh = chuoi(row.getCell(3));
		String tenNganh = chuoi(row.getCell(4));
		String khoa     = chuoi(row.getCell(5));
		String email    = chuoi(row.getCell(6));
		String dienThoai= chuoi(row.getCell(7));

		// --- Kiểm tra dữ liệu ---
		if (mssv.isBlank())    throw new IllegalStateException("thiếu MSSV.");
		if (hoTen.isBlank())   throw new IllegalStateException("thiếu họ tên.");
		if (tenNganh.isBlank())throw new IllegalStateException("thiếu tên ngành.");

		if (!mssvTrongFile.add(mssv))
			throw new IllegalStateException("MSSV " + mssv + " bị lặp trong file.");
		if (sinhVienRepository.existsByMaSoSinhVien(mssv))
			throw new IllegalStateException("MSSV " + mssv + " đã có trong hệ thống.");
		if (taiKhoanRepository.existsByTenDangNhap(mssv))
			throw new IllegalStateException("tên đăng nhập " + mssv + " đã tồn tại.");

		Nganh nganh = nganhRepository.findByTenNganh(tenNganh)
				.orElseThrow(() -> new IllegalStateException("không tìm thấy ngành \"" + tenNganh + "\"."));

		// --- Tài khoản ---
		TaiKhoan tk = new TaiKhoan();
		tk.setTenDangNhap(mssv);
		tk.setMatKhauBam(matKhauBam);
		tk.setQuyen(quyenSv);
		tk.setTrangThai(1);
		tk = taiKhoanRepository.save(tk);

		// --- Sinh viên ---
		SinhVien sv = new SinhVien();
		sv.setMaSoSinhVien(mssv);
		sv.setHoTen(hoTen);
		sv.setNgaySinh(sinh);
		sv.setGioiTinh(doiGioiTinh(gioiTinh));
		sv.setNganh(nganh);
		sv.setKhoaTuyenSinh(khoa.isBlank() ? null : khoa);
		sv.setEmailTruong(email.isBlank() ? null : email);
		sv.setSoDienThoai(dienThoai.isBlank() ? null : dienThoai);
		sv.setTaiKhoan(tk);
		sv.setTrangThai(1);          // đang học
		sv.setNgayNhapHoc(LocalDate.now());
		sinhVienRepository.save(sv);
	}

	// ===================== FILE MẪU =====================

	/** Sinh file Excel mẫu có sẵn tiêu đề và một dòng ví dụ */
	public byte[] taoFileMau() {
		try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sheet = wb.createSheet("SinhVien");

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

			// Dòng ví dụ, xóa đi trước khi nhập thật
			String tenNganhMau = nganhRepository.findAll().stream()
					.findFirst().map(Nganh::getTenNganh).orElse("Công nghệ thông tin");
			Row viDu = sheet.createRow(1);
			String[] mau = {"22110999", "Nguyễn Văn A", "15/08/2004", "Nam",
					tenNganhMau, "2022", "22110999@student.hcmute.edu.vn", "0901234567"};
			for (int i = 0; i < mau.length; i++) viDu.createCell(i).setCellValue(mau[i]);

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

	/** Đọc ô thành chuỗi; ô số nguyên (MSSV, khóa) không bị thành "22110123.0" */
	private String chuoi(Cell cell) {
		if (cell == null) return "";
		return switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue().trim();
			case NUMERIC -> DateUtil.isCellDateFormatted(cell)
					? cell.getLocalDateTimeCellValue().toLocalDate().format(DINH_DANG_NGAY)
					: String.valueOf((long) cell.getNumericCellValue());
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
			case FORMULA -> cell.getCellFormula();
			default -> "";
		};
	}

	/** Ngày: chấp nhận ô kiểu Date của Excel hoặc chuỗi dd/MM/yyyy */
	private LocalDate ngay(Cell cell) {
		if (cell == null) return null;
		if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell))
			return cell.getLocalDateTimeCellValue().toLocalDate();

		String s = chuoi(cell);
		if (s.isBlank()) return null;
		try {
			return LocalDate.parse(s.replace('-', '/'), DINH_DANG_NGAY);
		} catch (Exception e) {
			throw new IllegalStateException("ngày sinh \"" + s + "\" không đúng dạng dd/MM/yyyy.");
		}
	}

	/** 1 Nam, 2 Nữ, null nếu bỏ trống */
	private Integer doiGioiTinh(String s) {
		if (s == null || s.isBlank()) return null;
		String g = s.trim().toLowerCase();
		if (g.startsWith("nam") || g.equals("1") || g.equals("m")) return 1;
		if (g.startsWith("n")   || g.equals("2") || g.equals("f")) return 2;
		throw new IllegalStateException("giới tính \"" + s + "\" không hợp lệ (Nam hoặc Nữ).");
	}
}
