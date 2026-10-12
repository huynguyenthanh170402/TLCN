package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MoLopHangLoatService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MoLopHangLoatService.KetQuaMoLop;

/** Phòng Đào tạo mở lớp chính khóa hàng loạt theo kế hoạch đào tạo */
@Controller
@RequestMapping("/quanly/molop")
@RequiredArgsConstructor
public class MoLopController {

	private final MoLopHangLoatService moLopService;
	private final HocKyService hocKyService;
	private final NganhRepository nganhRepository;
	private final DangKyHocPhanService dangKyHocPhanService;

	@GetMapping
	public String form(@RequestParam(required = false) Integer hocKy,
	                   @RequestParam(required = false) Integer nganh,
	                   Model model) {
		model.addAttribute("menu", "molop");

		List<HocKy> dsHocKy = hocKyService.tatCa().stream()
				.filter(hk -> hk.getTrangThai() != 2)
				.toList();
		model.addAttribute("dsHocKy", dsHocKy);
		model.addAttribute("dsNganh", nganhRepository.findAll(Sort.by("tenNganh")));

		Integer maHocKy = hocKy != null ? hocKy
				: dangKyHocPhanService.hocKyHienTai().map(HocKy::getMaHocKy).orElse(null);
		model.addAttribute("hocKyChon", maHocKy);
		model.addAttribute("nganhChon", nganh);

		if (maHocKy != null && nganh != null) {
			try {
				model.addAttribute("keHoach", moLopService.keHoach(maHocKy, nganh));
			} catch (IllegalStateException e) {
				model.addAttribute("loi", e.getMessage());
			}
		}
		return "phongdaotao/molop";
	}

	@PostMapping
	public String moLop(@RequestParam Integer hocKy,
	                    @RequestParam Integer nganh,
	                    @RequestParam(defaultValue = "false") boolean moDangKyNgay,
	                    RedirectAttributes ra) {
		try {
			KetQuaMoLop kq = moLopService.moLop(hocKy, nganh, moDangKyNgay);
			if (kq.getSoLopDaTao() == 0)
				ra.addFlashAttribute("thongBao", "Không có lớp nào cần mở thêm.");
			else
				ra.addFlashAttribute("thongBao", "Đã mở " + kq.getSoLopDaTao() + " lớp chính khóa."
						+ (moDangKyNgay ? " Sinh viên đăng ký được ngay." : " Lớp đang ở trạng thái Chưa mở."));

			if (!kq.getCanhBao().isEmpty())
				ra.addFlashAttribute("loi", String.join(" ", kq.getCanhBao()));
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/molop?hocKy=" + hocKy + "&nganh=" + nganh;
	}
}