package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.sinhvien;

import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhSinhVienService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhSinhVienService.DongChuongTrinh;

/** Sinh viên xem chương trình đào tạo của ngành mình */
@Controller
@RequestMapping("/sinhvien/chuongtrinh")
@RequiredArgsConstructor
public class ChuongTrinhSinhVienController {

	private final SinhVienRepository sinhVienRepository;
	private final ChuongTrinhSinhVienService chuongTrinhService;

	@GetMapping
	public String xem(Principal principal, Model model) {
		model.addAttribute("menu", "chuongtrinh");

		SinhVien sv = principal == null ? null
				: sinhVienRepository.timTheoTenDangNhap(principal.getName()).orElse(null);
		model.addAttribute("sinhVien", sv);
		if (sv == null) return "sinhvien/chuongtrinh";

		List<DongChuongTrinh> ds = chuongTrinhService.chuongTrinhCuaSinhVien(sv);
		model.addAttribute("ds", ds);
		model.addAttribute("tienDo", chuongTrinhService.tienDo(ds));

		// Gom theo học kỳ ngay ở đây, template chỉ việc duyệt
		Map<Integer, List<DongChuongTrinh>> nhom = new LinkedHashMap<>();
		for (DongChuongTrinh d : ds) {
			nhom.computeIfAbsent(d.getChuongTrinh().getHocKyDuKien(),
					k -> new java.util.ArrayList<>()).add(d);
		}
		model.addAttribute("nhom", nhom);

		return "sinhvien/chuongtrinh";
	}
}