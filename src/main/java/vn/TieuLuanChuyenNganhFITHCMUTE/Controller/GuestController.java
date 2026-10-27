package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService.NganhThongKe;

/**
 * Trang giới thiệu dành cho khách chưa đăng nhập.
 *
 * Phần khoa, ngành và chương trình đào tạo lấy thẳng từ cơ sở dữ liệu chứ
 * không viết cứng trong giao diện. Viết cứng thì Phòng Đào tạo mở ngành mới
 * hay sửa khung chương trình xong, trang giới thiệu vẫn hiện số liệu cũ —
 * đúng chỗ người ngoài trường nhìn vào để chọn trường.
 */
@Controller
@RequiredArgsConstructor
public class GuestController {

	private final ChuongTrinhService chuongTrinhService;
	private final NganhRepository nganhRepository;
	private final DonViRepository donViRepository;

	@GetMapping({"/", "/home"})
	public String home(Model model) {
		model.addAttribute("page", "home");
		return "guest/home";
	}

	@GetMapping("/about")
	public String about(Model model) {
		model.addAttribute("page", "about");
		return "guest/about";
	}

	// ===================== KHOA =====================

	@GetMapping("/faculty")
	public String faculty(Model model) {
		model.addAttribute("page", "faculty");
		model.addAttribute("dsKhoa", chuongTrinhService.tongQuanCacKhoa());
		return "guest/faculty";
	}

	@GetMapping("/faculty/{maDonVi}")
	public String facultyChiTiet(@PathVariable Integer maDonVi, Model model) {
		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) return "redirect:/faculty";

		model.addAttribute("page", "faculty");
		model.addAttribute("khoa", khoa);
		model.addAttribute("dsNganh", chuongTrinhService.dsNganhCuaKhoa(maDonVi));
		// Trạng thái tuyển sinh nằm ở bảng Nganh, bảng thống kê không mang theo
		model.addAttribute("tuyenSinh", trangThaiTuyenSinh(maDonVi));
		return "guest/faculty-chitiet";
	}

	// ===================== NGÀNH =====================

	@GetMapping("/major")
	public String major(Model model) {
		model.addAttribute("page", "major");

		/*
		 * Gom ngành theo khoa ngay ở đây thay vì để giao diện tự nhóm.
		 * Thymeleaf không có cú pháp gom nhóm gọn gàng, làm trong template
		 * sẽ thành vòng lặp lồng nhau rồi lọc lại từng lần.
		 */
		Map<String, List<NganhThongKe>> theoKhoa = new java.util.LinkedHashMap<>();
		for (NganhThongKe n : chuongTrinhService.tongQuanCacNganh()) {
			String ten = (n.tenKhoa() == null) ? "Chưa gắn khoa" : n.tenKhoa();
			theoKhoa.computeIfAbsent(ten, k -> new ArrayList<>()).add(n);
		}
		model.addAttribute("theoKhoa", theoKhoa);
		return "guest/major";
	}

	@GetMapping("/major/{maNganh}")
	public String majorChiTiet(@PathVariable Integer maNganh, Model model) {
		Nganh nganh = nganhRepository.findWithDonViByMaNganh(maNganh).orElse(null);
		if (nganh == null) return "redirect:/major";

		List<ChuongTrinhDaoTao> ds = chuongTrinhService.chiTiet(maNganh);

		model.addAttribute("page", "major");
		model.addAttribute("nganh", nganh);
		model.addAttribute("ds", ds);
		model.addAttribute("tongTinChi", ds.stream()
				.mapToInt(ct -> ct.getHocPhan().getSoTinChi() == null
						? 0 : ct.getHocPhan().getSoTinChi())
				.sum());
		return "guest/major-chitiet";
	}

	// ===================== CÁC TRANG TĨNH =====================

	@GetMapping("/center")
	public String center(Model model) {
		model.addAttribute("page", "center");
		return "guest/center";
	}

	@GetMapping("/news")
	public String news(Model model) {
		model.addAttribute("page", "news");
		return "guest/news";
	}

	@GetMapping("/research")
	public String research(Model model) {
		model.addAttribute("page", "research");
		return "guest/research";
	}

	// ===================== HỖ TRỢ =====================

	/** Mã ngành → còn tuyển sinh hay không */
	private Map<Integer, Boolean> trangThaiTuyenSinh(Integer maDonVi) {
		Map<Integer, Boolean> kq = new HashMap<>();
		for (Nganh n : nganhRepository.findByDonVi_MaDonViOrderByTenNganh(maDonVi)) {
			kq.put(n.getMaNganh(),
					n.getTrangThai() == null || n.getTrangThai() == 1);
		}
		return kq;
	}
}
