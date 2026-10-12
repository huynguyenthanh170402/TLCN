package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichHocService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LopHocPhanService;

/** Phòng Đào tạo: danh sách lớp học phần, mở lớp mới, đổi trạng thái */
@Controller
@RequestMapping("/quanly/lophocphan")
@RequiredArgsConstructor
public class LopHocPhanController {

	private final LopHocPhanService lopService;
	private final HocKyService hocKyService;
	private final DangKyHocPhanService dangKyHocPhanService;
	private final LichHocService lichHocService;
	private final NganhRepository nganhRepository;

	/** Học kỳ đang xem: theo tham số, nếu không có thì lấy học kỳ đang diễn ra */
	private Integer chonHocKy(Integer hocKy, List<HocKy> dsHocKy) {
		if (hocKy != null) return hocKy;
		return dangKyHocPhanService.hocKyHienTai()
				.map(HocKy::getMaHocKy)
				.orElse(dsHocKy.isEmpty() ? null : dsHocKy.get(0).getMaHocKy());
	}

	// ===================== DANH SÁCH =====================
	@GetMapping
	public String danhSach(@RequestParam(required = false) Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa();
		Integer maHocKy = chonHocKy(hocKy, dsHocKy);

		model.addAttribute("menu", "lophocphan");
		model.addAttribute("dsHocKy", dsHocKy);
		model.addAttribute("hocKyChon", maHocKy);

		List<LopHocPhan> dsLop = maHocKy == null ? List.of() : lopService.dsTheoHocKy(maHocKy);
		model.addAttribute("dsLop", dsLop);
		model.addAttribute("siSo", dangKyHocPhanService.siSoCacLop(dsLop));
		model.addAttribute("lichTheoLop", lichHocService.lichTheoLop(
				dsLop.stream().map(LopHocPhan::getMaLopHocPhan).toList()));
		return "phongdaotao/lophocphan";
	}

	// ===================== MỞ LỚP MỚI =====================
	@GetMapping("/them")
	public String formThem(@RequestParam(required = false) Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa().stream()
				.filter(hk -> hk.getTrangThai() != 2)   // bỏ học kỳ đã kết thúc
				.toList();

		model.addAttribute("menu", "lophocphan");
		model.addAttribute("dsHocKy", dsHocKy);
		model.addAttribute("dsHocPhan", lopService.dsHocPhan());
		model.addAttribute("dsPhong", lopService.dsPhongDangDung());
		// Dữ liệu cho bộ lọc theo ngành ở ô chọn học phần
		model.addAttribute("dsNganh", nganhRepository.findAll(
				org.springframework.data.domain.Sort.by("tenNganh")));
		model.addAttribute("nganhCuaHocPhan", lopService.nganhCuaHocPhan());
		model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);
		if (!model.containsAttribute("maHocKy")) {
			model.addAttribute("maHocKy", chonHocKy(hocKy, dsHocKy));
		}
		return "phongdaotao/lophocphan-them";
	}

	@PostMapping("/them")
	public String xuLyThem(@RequestParam Integer maHocPhan,
	                       @RequestParam Integer maHocKy,
	                       @RequestParam int nhom,
	                       @RequestParam int siSoToiDa,
	                       @RequestParam int siSoToiThieu,
	                       @RequestParam int thu,
	                       @RequestParam int tietBatDau,
	                       @RequestParam int soTiet,
	                       @RequestParam Integer maPhong,
	                       @RequestParam(defaultValue = "1") int loaiLop,
	                       @RequestParam(required = false) String khoaApDung,
	                       RedirectAttributes ra) {
		try {
			LopHocPhan lop = lopService.taoLop(maHocPhan, maHocKy, nhom, siSoToiDa, siSoToiThieu,
					thu, tietBatDau, soTiet, maPhong, loaiLop, khoaApDung);
			ra.addFlashAttribute("thongBao", "Đã tạo lớp " + lop.getMaLop()
					+ ". Bấm \"Mở ĐK\" để sinh viên đăng ký được.");
			return "redirect:/quanly/lophocphan?hocKy=" + maHocKy;
		} catch (IllegalStateException e) {
			// Quay lại form, giữ dữ liệu đã nhập
			ra.addFlashAttribute("loi", e.getMessage());
			ra.addFlashAttribute("maHocPhan", maHocPhan);
			ra.addFlashAttribute("maHocKy", maHocKy);
			ra.addFlashAttribute("nhom", nhom);
			ra.addFlashAttribute("siSoToiDa", siSoToiDa);
			ra.addFlashAttribute("siSoToiThieu", siSoToiThieu);
			ra.addFlashAttribute("thu", thu);
			ra.addFlashAttribute("tietBatDau", tietBatDau);
			ra.addFlashAttribute("soTiet", soTiet);
			ra.addFlashAttribute("maPhong", maPhong);
			ra.addFlashAttribute("loaiLop", loaiLop);
			ra.addFlashAttribute("khoaApDung", khoaApDung);
			return "redirect:/quanly/lophocphan/them";
		}
	}

	// ===================== ĐỔI TRẠNG THÁI =====================
	@PostMapping("/{id}/trangthai")
	public String doiTrangThai(@PathVariable Integer id,
	                           @RequestParam int trangThai,
	                           @RequestParam(required = false) Integer hocKy,
	                           RedirectAttributes ra) {
		try {
			LopHocPhan lop = lopService.doiTrangThai(id, trangThai);
			ra.addFlashAttribute("thongBao", "Đã cập nhật trạng thái lớp " + lop.getMaLop() + ".");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/lophocphan" + (hocKy != null ? "?hocKy=" + hocKy : "");
	}
}