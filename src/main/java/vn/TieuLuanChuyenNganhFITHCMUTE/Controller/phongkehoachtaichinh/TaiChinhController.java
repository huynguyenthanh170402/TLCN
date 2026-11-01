package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongkehoachtaichinh;

import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThuHocPhiService;

/** Trang chủ của Phòng Kế hoạch Tài chính */
@Controller
@RequestMapping("/taichinh")
@RequiredArgsConstructor
public class TaiChinhController {

	private static final int HOC_KY_DANG_DIEN_RA = 1;

	private final HocKyRepository hocKyRepository;
	private final ThuHocPhiService thuHocPhiService;

	@GetMapping
	public String trangChu(Model model) {
		model.addAttribute("menu", "tongquan");
		model.addAttribute("homNay", LocalDate.now());

		// Số liệu của học kỳ đang diễn ra. Chưa mở học kỳ nào thì để trống,
		// trang vẫn hiện bình thường nhờ th:if bên template.
		HocKy hocKy = hocKyRepository
				.findFirstByTrangThaiOrderByNgayBatDauDesc(HOC_KY_DANG_DIEN_RA)
				.orElse(null);

		model.addAttribute("hocKy", hocKy);
		if (hocKy != null) {
			model.addAttribute("tk", thuHocPhiService.thongKe(hocKy.getMaHocKy()));
		}
		return "phongkehoachtaichinh/trangchu";
	}
}
