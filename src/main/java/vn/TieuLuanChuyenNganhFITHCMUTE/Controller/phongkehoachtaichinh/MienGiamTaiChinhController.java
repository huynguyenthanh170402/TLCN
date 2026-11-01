package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongkehoachtaichinh;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MienGiamService;

/**
 * Miễn giảm học phí, nhìn từ phía Phòng Kế hoạch Tài chính.
 *
 * Chỉ đọc. Quyền quyết định ai được giảm bao nhiêu thuộc về Phòng Công tác
 * Sinh viên; ở đây chỉ xem căn cứ rồi bấm tính lại công nợ để quy ra tiền.
 */
@Controller
@RequestMapping("/taichinh/miengiam")
@RequiredArgsConstructor
public class MienGiamTaiChinhController {

	private static final int SO_DONG_MOI_TRANG = 20;
	private static final int DA_DUYET = MienGiamService.DA_DUYET;

	private final MienGiamService mienGiamService;
	private final HocKyRepository hocKyRepository;

	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer maHocKy,
	                       @RequestParam(required = false) String tuKhoa,
	                       @RequestParam(defaultValue = "0") int trang,
	                       Model model) {

		model.addAttribute("menu", "miengiam");
		model.addAttribute("dsHocKy",
				hocKyRepository.findAll(Sort.by(Sort.Direction.DESC, "ngayBatDau")));
		model.addAttribute("maHocKy", maHocKy);
		model.addAttribute("tuKhoa", tuKhoa);

		// Chỉ hiện hồ sơ đã duyệt: hồ sơ còn chờ duyệt chưa có giá trị
		// với Tài chính, và hiện ra chỉ gây hiểu nhầm là đã được giảm.
		model.addAttribute("ds", mienGiamService.traCuu(
				maHocKy, DA_DUYET, tuKhoa,
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG)));

		return "phongkehoachtaichinh/miengiam";
	}
}
