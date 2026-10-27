//package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;
package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ImportSinhVienService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ImportSinhVienService.KetQuaImport;

/** Phòng Đào tạo: nhập danh sách sinh viên từ file Excel */
@Controller
@RequestMapping("/quanly/sinhvien/import")
@RequiredArgsConstructor
public class SinhVienImportController {

	private final ImportSinhVienService importService;

	@GetMapping
	public String form(Model model) {
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("matKhauMacDinh", ImportSinhVienService.MAT_KHAU_MAC_DINH);
		return "phongdaotao/sinhvien-import";
	}

	@PostMapping
	public String nhap(@RequestParam("file") MultipartFile file,
	                   RedirectAttributes ra, Model model) {
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("matKhauMacDinh", ImportSinhVienService.MAT_KHAU_MAC_DINH);
		try {
			KetQuaImport kq = importService.nhapTuFile(file);
			model.addAttribute("kq", kq);
			model.addAttribute("tenFile", file.getOriginalFilename());
		} catch (IllegalStateException e) {
			model.addAttribute("loi", e.getMessage());
		}
		// Ở lại trang để xem danh sách dòng lỗi
		return "phongdaotao/sinhvien-import";
	}

	/** Tải file Excel mẫu do hệ thống sinh ra */
	@GetMapping("/mau")
	public ResponseEntity<Resource> taiFileMau() {
		byte[] dulieu = importService.taoFileMau();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-danh-sach-sinh-vien.xlsx\"")
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.contentLength(dulieu.length)
				.body(new ByteArrayResource(dulieu));
	}
}
