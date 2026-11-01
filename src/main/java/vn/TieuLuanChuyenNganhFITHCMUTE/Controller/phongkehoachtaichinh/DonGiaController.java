package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongkehoachtaichinh;

import java.math.BigDecimal;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DonGiaService;

/** Quản lý đơn giá tín chỉ */
@Controller
@RequestMapping("/taichinh/dongia")
@RequiredArgsConstructor
public class DonGiaController {

	private final DonGiaService donGiaService;
	private final HocKyRepository hocKyRepository;

	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer sua, Model model) {
		model.addAttribute("menu", "dongia");
		model.addAttribute("ds", donGiaService.danhSach());
		model.addAttribute("dsHocKy",
				hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau")));

		// Bấm Sửa thì nạp dòng đó lên biểu mẫu, không mở trang riêng
		model.addAttribute("dangSua", sua == null ? null : donGiaService.tim(sua));
		return "phongkehoachtaichinh/dongia";
	}

	@PostMapping("/luu")
	public String luu(@RequestParam(required = false) Integer maDonGia,
	                  @RequestParam Integer maHocKy,
	                  @RequestParam(required = false) String khoaTuyenSinh,
	                  @RequestParam BigDecimal donGia,
	                  @RequestParam(required = false) String ghiChu,
	                  RedirectAttributes ra) {
		try {
			donGiaService.luu(maDonGia, maHocKy, khoaTuyenSinh, donGia, ghiChu);
			ra.addFlashAttribute("thongBao",
					maDonGia == null ? "Đã thêm đơn giá." : "Đã cập nhật đơn giá.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/taichinh/dongia";
	}

	@PostMapping("/xoa/{maDonGia}")
	public String xoa(@PathVariable Integer maDonGia, RedirectAttributes ra) {
		try {
			donGiaService.xoa(maDonGia);
			ra.addFlashAttribute("thongBao", "Đã xóa đơn giá.");
		} catch (RuntimeException e) {
			ra.addFlashAttribute("loi", "Không xóa được đơn giá này.");
		}
		return "redirect:/taichinh/dongia";
	}
}
