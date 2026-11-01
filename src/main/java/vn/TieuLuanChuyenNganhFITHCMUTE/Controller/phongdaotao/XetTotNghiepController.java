package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonXetTotNghiep;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DotXetTotNghiep;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.XetTotNghiepService;

/** Phòng Đào tạo mở đợt xét tốt nghiệp và công nhận tốt nghiệp */
@Controller
@RequestMapping("/quanly/totnghiep")
@RequiredArgsConstructor
public class XetTotNghiepController {

	private final XetTotNghiepService xetService;
	private final HocKyService hocKyService;

	// ===================== DANH SÁCH ĐỢT =====================

	@GetMapping
	public String danhSach(Model model) {
		model.addAttribute("menu", "totnghiep");
		model.addAttribute("dsDot", xetService.dsDot());
		model.addAttribute("dsHocKy", hocKyService.tatCa());
		return "phongdaotao/totnghiep";
	}

	@PostMapping("/them")
	public String themDot(@RequestParam Integer maHocKy,
	                      @RequestParam String tenDot,
	                      @RequestParam(required = false)
	                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngayMoDangKy,
	                      @RequestParam(required = false)
	                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime ngayDongDangKy,
	                      @RequestParam(required = false)
	                      @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngayXet,
	                      Principal principal,
	                      RedirectAttributes ra) {
		try {
			DotXetTotNghiep dot = xetService.taoDot(maHocKy, tenDot, ngayMoDangKy, ngayDongDangKy,
					ngayXet, principal == null ? null : principal.getName());
			ra.addFlashAttribute("thongBao", "Đã tạo đợt xét. Đợt đang ở trạng thái Nháp, "
					+ "bấm \"Mở nhận đơn\" để sinh viên nộp được.");
			return "redirect:/quanly/totnghiep/" + dot.getMaDot();
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/quanly/totnghiep";
		}
	}

	// ===================== CHI TIẾT MỘT ĐỢT =====================

	@GetMapping("/{maDot}")
	public String chiTiet(@PathVariable Integer maDot, Model model, RedirectAttributes ra) {
		try {
			DotXetTotNghiep dot = xetService.timDot(maDot);
			List<DonXetTotNghiep> dsDon = xetService.dsDon(maDot);

			model.addAttribute("menu", "totnghiep");
			model.addAttribute("dot", dot);
			model.addAttribute("dsDon", dsDon);
			// Xét lại điều kiện theo số liệu hiện tại, vì từ lúc nộp tới lúc
			// họp hội đồng có thể đã khác
			model.addAttribute("ketQua", xetService.xetCacDon(dsDon));
			return "phongdaotao/totnghiep-chitiet";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/quanly/totnghiep";
		}
	}

	@PostMapping("/{maDot}/trangthai")
	public String doiTrangThai(@PathVariable Integer maDot,
	                           @RequestParam int trangThai,
	                           RedirectAttributes ra) {
		try {
			xetService.doiTrangThaiDot(maDot, trangThai);
			ra.addFlashAttribute("thongBao", "Đã cập nhật trạng thái đợt xét.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/totnghiep/" + maDot;
	}

	// ===================== XÉT TỪNG ĐƠN =====================

	@PostMapping("/{maDot}/don/{maDon}/duyet")
	public String duyet(@PathVariable Integer maDot, @PathVariable Integer maDon,
	                    Principal principal, RedirectAttributes ra) {
		try {
			xetService.duyet(maDon, principal == null ? null : principal.getName());
			ra.addFlashAttribute("thongBao",
					"Đã công nhận tốt nghiệp. Trạng thái sinh viên chuyển sang Đã tốt nghiệp.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/totnghiep/" + maDot;
	}

	@PostMapping("/{maDot}/don/{maDon}/tuchoi")
	public String tuChoi(@PathVariable Integer maDot, @PathVariable Integer maDon,
	                     @RequestParam String lyDo,
	                     Principal principal, RedirectAttributes ra) {
		try {
			xetService.tuChoi(maDon, lyDo, principal == null ? null : principal.getName());
			ra.addFlashAttribute("thongBao", "Đã ghi nhận không đủ điều kiện kèm lý do.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/totnghiep/" + maDot;
	}
}
