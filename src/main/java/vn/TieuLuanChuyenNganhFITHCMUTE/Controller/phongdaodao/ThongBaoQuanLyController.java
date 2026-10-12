package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThongBaoQuanLyService;

/** Phòng ban soạn, đăng, gỡ thông báo */
@Controller
@RequestMapping("/quanly/thongbao")
@RequiredArgsConstructor
public class ThongBaoQuanLyController {

	private final ThongBaoQuanLyService service;

	// ===================== DANH SÁCH ĐÃ GỬI =====================
	@GetMapping
	public String danhSach(Principal principal,
	                       @RequestParam(defaultValue = "0") int page,
	                       Model model) {
		TaiKhoan tk = service.taiKhoan(principal.getName());
		Page<ThongBao> trang = service.dsDaGui(tk, page);

		model.addAttribute("menu", "thongbao");
		model.addAttribute("trang", trang);
		model.addAttribute("nguoiNhan", service.moTaNguoiNhan(trang.getContent()));
		model.addAttribute("daDoc", service.soNguoiDaDoc(trang.getContent()));
		return "phongdaotao/thongbao";
	}

	// ===================== SOẠN =====================
	@GetMapping("/soan")
	public String formSoan(Model model) {
		model.addAttribute("menu", "thongbao");
		model.addAttribute("dsQuyen", service.dsQuyen());
		model.addAttribute("dsPhongBan", service.dsPhongBan());
		return "phongdaotao/thongbao-soan";
	}

	@PostMapping("/soan")
	public String xuLySoan(Principal principal,
	                       @RequestParam String tieuDe,
	                       @RequestParam String noiDung,
	                       @RequestParam(defaultValue = "1") int loai,
	                       @RequestParam(defaultValue = "1") int cheDo,
	                       @RequestParam(required = false) List<Integer> dsQuyen,
	                       @RequestParam(required = false) Integer maDonVi,
	                       @RequestParam(required = false)
	                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayHetHan,
	                       @RequestParam(defaultValue = "dang") String hanhDong,
	                       RedirectAttributes ra) {
		try {
			TaiKhoan tk = service.taiKhoan(principal.getName());
			boolean dangNgay = "dang".equals(hanhDong);
			service.tao(tk, tieuDe, noiDung, loai, cheDo, dsQuyen, maDonVi, ngayHetHan, dangNgay);
			ra.addFlashAttribute("thongBao", dangNgay ? "Đã đăng thông báo." : "Đã lưu bản nháp.");
			return "redirect:/quanly/thongbao";
		} catch (IllegalStateException e) {
			// Lỗi nhập liệu: quay lại form, giữ nội dung đã gõ
			ra.addFlashAttribute("loi", e.getMessage());
			ra.addFlashAttribute("tieuDe", tieuDe);
			ra.addFlashAttribute("noiDung", noiDung);
			ra.addFlashAttribute("loai", loai);
			ra.addFlashAttribute("cheDo", cheDo);
			ra.addFlashAttribute("dsQuyenChon", dsQuyen);
			ra.addFlashAttribute("maDonVi", maDonVi);
			return "redirect:/quanly/thongbao/soan";
		}
	}

	// ===================== ĐĂNG NHÁP / GỠ =====================
	@PostMapping("/{id}/dang")
	public String dang(Principal principal, @PathVariable Integer id, RedirectAttributes ra) {
		try {
			service.dang(service.taiKhoan(principal.getName()), id);
			ra.addFlashAttribute("thongBao", "Đã đăng thông báo.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/thongbao";
	}

	@PostMapping("/{id}/go")
	public String go(Principal principal, @PathVariable Integer id, RedirectAttributes ra) {
		try {
			service.go(service.taiKhoan(principal.getName()), id);
			ra.addFlashAttribute("thongBao", "Đã gỡ thông báo. Người nhận sẽ không còn thấy nữa.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/thongbao";
	}
}
