package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.PhanCongService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.PhanCongService.DongPhanCong;

/** Phòng Đào tạo phân công và đổi giảng viên cho lớp học phần */
@Controller
@RequestMapping("/quanly/phancong")
@RequiredArgsConstructor
public class PhanCongController {

	private final PhanCongService phanCongService;
	private final HocKyService hocKyService;
	private final DangKyHocPhanService dangKyHocPhanService;

	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer hocKy,
	                       @RequestParam(defaultValue = "false") boolean chuaPhanCong,
	                       Model model) {
		model.addAttribute("menu", "phancong");
		model.addAttribute("dsHocKy", hocKyService.tatCa());

		Integer maHocKy = hocKy != null ? hocKy
				: dangKyHocPhanService.hocKyHienTai().map(HocKy::getMaHocKy).orElse(null);
		model.addAttribute("hocKyChon", maHocKy);
		model.addAttribute("chuaPhanCong", chuaPhanCong);

		if (maHocKy != null) {
			List<DongPhanCong> ds = phanCongService.dsLop(maHocKy, chuaPhanCong);
			model.addAttribute("ds", ds);

			// Danh sách giảng viên gợi ý cho từng lớp, theo khoa phụ trách học phần
			Map<Integer, List<GiangVien>> dsGv = new HashMap<>();
			for (DongPhanCong d : ds) {
				Integer maDonVi = d.getLop().getHocPhan().getDonVi() == null ? null
						: d.getLop().getHocPhan().getDonVi().getMaDonVi();
				dsGv.computeIfAbsent(d.getLop().getMaLopHocPhan(),
						k -> phanCongService.dsGiangVien(maDonVi));
			}
			model.addAttribute("dsGv", dsGv);
		}
		return "phongdaotao/phancong";
	}

	@PostMapping("/{maLop}")
	public String doiGiangVien(@PathVariable Integer maLop,
	                           @RequestParam Integer maGiangVien,
	                           @RequestParam Integer hocKy,
	                           @RequestParam(defaultValue = "false") boolean chuaPhanCong,
	                           RedirectAttributes ra) {
		try {
			phanCongService.doiGiangVien(maLop, maGiangVien);
			ra.addFlashAttribute("thongBao", "Đã cập nhật giảng viên phụ trách lớp.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/phancong?hocKy=" + hocKy + "&chuaPhanCong=" + chuaPhanCong;
	}
}