package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocPhiService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KetQuaHocTapService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThongKeSinhVienService;

/**
 * Phòng Đào tạo tra cứu sinh viên theo cấp:
 * Khoa → Ngành → Khóa → Danh sách, kèm ô tìm nhanh toàn trường.
 */
@Controller
@RequestMapping("/quanly/sinhvien")
@RequiredArgsConstructor
public class SinhVienQuanLyController {

	private static final int SO_DONG_MOI_TRANG = 20;

	private final SinhVienRepository sinhVienRepository;
	private final NganhRepository nganhRepository;
	private final DonViRepository donViRepository;
	private final ThongKeSinhVienService thongKeService;
	private final KetQuaHocTapService ketQuaHocTapService;
	private final HocPhiService hocPhiService;

	// ===================== CẤP 1: KHOA =====================
	@GetMapping
	public String dsKhoa(Model model) {
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("dsKhoa", thongKeService.theoKhoa());
		model.addAttribute("tongSinhVien", thongKeService.tongSinhVien());
		return "phongdaotao/sinhvien";
	}

	// ===================== CẤP 2: NGÀNH TRONG KHOA =====================
	@GetMapping("/khoa/{maDonVi}")
	public String dsNganh(@PathVariable Integer maDonVi, Model model, RedirectAttributes ra) {
		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy khoa.");
			return "redirect:/quanly/sinhvien";
		}
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("khoa", khoa);
		model.addAttribute("dsNganh", thongKeService.theoNganh(maDonVi));
		return "phongdaotao/sinhvien-nganh";
	}

	// ===================== CẤP 3: KHÓA TUYỂN SINH =====================
	@GetMapping("/nganh/{maNganh}")
	public String dsKhoaTuyenSinh(@PathVariable Integer maNganh, Model model, RedirectAttributes ra) {
		Nganh nganh = nganhRepository.findById(maNganh).orElse(null);
		if (nganh == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy ngành.");
			return "redirect:/quanly/sinhvien";
		}
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("nganh", nganh);
		model.addAttribute("dsKhoaTuyenSinh", thongKeService.theoKhoaTuyenSinh(maNganh));
		return "phongdaotao/sinhvien-khoatuyensinh";
	}

	// ===================== CẤP 4: DANH SÁCH SINH VIÊN =====================
	@GetMapping("/nganh/{maNganh}/khoa/{khoaTuyenSinh}")
	public String dsSinhVien(@PathVariable Integer maNganh,
	                         @PathVariable String khoaTuyenSinh,
	                         @RequestParam(defaultValue = "0") int page,
	                         Model model, RedirectAttributes ra) {
		Nganh nganh = nganhRepository.findById(maNganh).orElse(null);
		if (nganh == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy ngành.");
			return "redirect:/quanly/sinhvien";
		}

		Page<SinhVien> trang = sinhVienRepository.findByNganh_MaNganhAndKhoaTuyenSinhOrderByMaSoSinhVien(
				maNganh, khoaTuyenSinh, PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG));

		model.addAttribute("menu", "sinhvien");
		model.addAttribute("nganh", nganh);
		model.addAttribute("khoaTuyenSinh", khoaTuyenSinh);
		model.addAttribute("trang", trang);
		return "phongdaotao/sinhvien-danhsach";
	}

	// ===================== TÌM NHANH =====================
	@GetMapping("/tim")
	public String timNhanh(@RequestParam(required = false) String tuKhoa,
	                       @RequestParam(defaultValue = "0") int page,
	                       Model model) {
		model.addAttribute("menu", "sinhvien");
		String tk = (tuKhoa == null || tuKhoa.isBlank()) ? null : tuKhoa.trim();
		model.addAttribute("tuKhoa", tk);

		if (tk != null) {
			model.addAttribute("trang", sinhVienRepository.timNhanh(tk,
					PageRequest.of(Math.max(page, 0), SO_DONG_MOI_TRANG)));
		}
		return "phongdaotao/sinhvien-tim";
	}

	// ===================== CHI TIẾT =====================
	@GetMapping("/chitiet/{id}")
	public String chiTiet(@PathVariable Integer id, Model model, RedirectAttributes ra) {
		SinhVien sv = sinhVienRepository.timTheoMa(id).orElse(null);
		if (sv == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy sinh viên.");
			return "redirect:/quanly/sinhvien";
		}

		List<KetQuaHocPhan> dsKetQua = ketQuaHocTapService.tatCa(sv.getMaSinhVien());

		model.addAttribute("menu", "sinhvien");
		model.addAttribute("sv", sv);
		model.addAttribute("tongKet", ketQuaHocTapService.tinhTongKet(dsKetQua));
		model.addAttribute("dsKetQua", dsKetQua);
		model.addAttribute("dsCongNo", hocPhiService.dsCongNo(sv).stream()
				.filter(cn -> cn.getTrangThai() != 3)
				.toList());
		return "phongdaotao/sinhvien-chitiet";
	}
}
