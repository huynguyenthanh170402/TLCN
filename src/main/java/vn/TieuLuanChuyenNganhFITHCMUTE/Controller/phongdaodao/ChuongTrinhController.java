package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import java.util.List;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService.KetQuaImport;

/** Phòng Đào tạo: chương trình đào tạo của các ngành, nhập từ Excel */
@Controller
@RequestMapping("/quanly/chuongtrinh")
@RequiredArgsConstructor
public class ChuongTrinhController {

	private final ChuongTrinhService chuongTrinhService;
	private final NganhRepository nganhRepository;

	// ===================== TỔNG QUAN =====================
	@GetMapping
	public String tongQuan(Model model) {
		model.addAttribute("menu", "chuongtrinh");
		model.addAttribute("dsNganh", chuongTrinhService.tongQuanCacNganh());
		return "phongdaotao/chuongtrinh";
	}

	// ===================== CHI TIẾT MỘT NGÀNH =====================
	@GetMapping("/nganh/{maNganh}")
	public String theoNganh(@PathVariable Integer maNganh, Model model, RedirectAttributes ra) {
		Nganh nganh = nganhRepository.findById(maNganh).orElse(null);
		if (nganh == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy ngành.");
			return "redirect:/quanly/chuongtrinh";
		}

		List<ChuongTrinhDaoTao> ds = chuongTrinhService.chiTiet(maNganh);
		int tongTinChi = ds.stream().mapToInt(ct -> ct.getHocPhan().getSoTinChi()).sum();
		int tinChiBatBuoc = ds.stream().filter(ct -> ct.getLoaiHocPhan() == 1)
				.mapToInt(ct -> ct.getHocPhan().getSoTinChi()).sum();

		model.addAttribute("menu", "chuongtrinh");
		model.addAttribute("nganh", nganh);
		model.addAttribute("ds", ds);
		model.addAttribute("tongTinChi", tongTinChi);
		model.addAttribute("tinChiBatBuoc", tinChiBatBuoc);
		model.addAttribute("tinChiTuChon", tongTinChi - tinChiBatBuoc);
		return "phongdaotao/chuongtrinh-nganh";
	}

	// ===================== NHẬP EXCEL =====================
	@GetMapping("/import")
	public String formImport(Model model) {
		model.addAttribute("menu", "chuongtrinh");
		return "phongdaotao/chuongtrinh-import";
	}

	@PostMapping("/import")
	public String nhap(@RequestParam("file") MultipartFile file, Model model) {
		model.addAttribute("menu", "chuongtrinh");
		try {
			KetQuaImport kq = chuongTrinhService.nhapTuFile(file);
			model.addAttribute("kq", kq);
			model.addAttribute("tenFile", file.getOriginalFilename());
		} catch (IllegalStateException e) {
			model.addAttribute("loi", e.getMessage());
		}
		return "phongdaotao/chuongtrinh-import";
	}

	@GetMapping("/import/mau")
	public ResponseEntity<Resource> taiFileMau() {
		byte[] dulieu = chuongTrinhService.taoFileMau();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"mau-chuong-trinh-dao-tao.xlsx\"")
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.contentLength(dulieu.length)
				.body(new ByteArrayResource(dulieu));
	}
}