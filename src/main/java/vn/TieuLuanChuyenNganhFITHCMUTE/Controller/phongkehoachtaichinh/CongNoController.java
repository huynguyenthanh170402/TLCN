package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongkehoachtaichinh;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThuHocPhiService;

/** Công nợ học phí và lập phiếu thu */
@Controller
@RequestMapping("/taichinh")
@RequiredArgsConstructor
public class CongNoController {

	private static final int SO_DONG_MOI_TRANG = 20;

	private final ThuHocPhiService thuHocPhiService;
	private final HocKyRepository hocKyRepository;

	// ===================== DANH SÁCH CÔNG NỢ =====================

	@GetMapping("/congno")
	public String congNo(@RequestParam(required = false) Integer maHocKy,
	                     @RequestParam(required = false) Integer trangThai,
	                     @RequestParam(required = false) String tuKhoa,
	                     @RequestParam(defaultValue = "0") int trang,
	                     Model model) {

		model.addAttribute("menu", "congno");
		model.addAttribute("dsHocKy",
				hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau")));

		// Giữ lại điều kiện lọc để biểu mẫu hiện đúng cái người dùng vừa chọn
		model.addAttribute("maHocKy", maHocKy);
		model.addAttribute("trangThai", trangThai);
		model.addAttribute("tuKhoa", tuKhoa);

		model.addAttribute("ds", thuHocPhiService.traCuu(
				maHocKy, trangThai, tuKhoa,
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG)));
		model.addAttribute("tk", thuHocPhiService.thongKe(maHocKy));

		return "phongkehoachtaichinh/congno";
	}

	/** Tính lại công nợ cho cả học kỳ */
	@PostMapping("/congno/sinh")
	public String sinhCongNo(@RequestParam Integer maHocKy, RedirectAttributes ra) {
		try {
			int soSinhVien = thuHocPhiService.sinhCongNo(maHocKy);
			ra.addFlashAttribute("thongBao",
					"Đã tính lại công nợ cho " + soSinhVien + " sinh viên.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/taichinh/congno?maHocKy=" + maHocKy;
	}

	// ===================== LẬP PHIẾU THU =====================

	@GetMapping("/phieuthu/{maCongNo}")
	public String formPhieuThu(@PathVariable Integer maCongNo, Model model,
	                           RedirectAttributes ra) {
		model.addAttribute("menu", "phieuthu");
		try {
			model.addAttribute("cn", thuHocPhiService.chiTiet(maCongNo));
			model.addAttribute("dsPhieu", thuHocPhiService.dsPhieuCuaCongNo(maCongNo));
			return "phongkehoachtaichinh/phieuthu";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/taichinh/congno";
		}
	}

	@PostMapping("/phieuthu/{maCongNo}")
	public String luuPhieuThu(@PathVariable Integer maCongNo,
	                          @RequestParam BigDecimal soTien,
	                          @RequestParam(defaultValue = "1") Integer hinhThuc,
	                          @RequestParam(required = false) String ghiChu,
	                          Principal principal,
	                          RedirectAttributes ra) {
		try {
			var pt = thuHocPhiService.lapPhieuThu(
					maCongNo, soTien, hinhThuc, principal.getName(), ghiChu);
			ra.addFlashAttribute("thongBao", "Đã lập phiếu thu " + pt.getSoPhieu() + ".");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/taichinh/phieuthu/" + maCongNo;
	}

	@PostMapping("/phieuthu/huy/{maPhieuThu}")
	public String huyPhieuThu(@PathVariable Integer maPhieuThu,
	                          @RequestParam Integer maCongNo,
	                          RedirectAttributes ra) {
		try {
			thuHocPhiService.huyPhieuThu(maPhieuThu);
			ra.addFlashAttribute("thongBao", "Đã hủy phiếu thu.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/taichinh/phieuthu/" + maCongNo;
	}

	/** Sổ phiếu thu: mọi phiếu đã lập, mới nhất trước */
	@GetMapping("/phieuthu")
	public String soPhieuThu(@RequestParam(defaultValue = "0") int trang, Model model) {
		model.addAttribute("menu", "phieuthu");
		model.addAttribute("ds", thuHocPhiService.soPhieuThu(
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG)));
		return "phongkehoachtaichinh/sophieuthu";
	}
}
