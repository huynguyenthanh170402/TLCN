package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichHocService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LopHocPhanService;

/**
 * Phòng Đào tạo: danh sách lớp học phần, mở lớp mới, đổi trạng thái.
 *
 * Chia ba tầng khoa → ngành → danh sách lớp, giống Mở lớp theo kế hoạch.
 * Một học kỳ có gần nghìn lớp nên gộp hết vào một bảng thì không ai tìm ra
 * phần việc của mình, và rất dễ bấm nhầm nút Hủy của lớp khoa khác.
 */
@Controller
@RequestMapping("/quanly/lophocphan")
@RequiredArgsConstructor
public class LopHocPhanController {

	private final LopHocPhanService lopService;
	private final HocKyService hocKyService;
	private final DangKyHocPhanService dangKyHocPhanService;
	private final LichHocService lichHocService;
	private final NganhRepository nganhRepository;
	private final DonViRepository donViRepository;

	/** Học kỳ đang xem: theo tham số, nếu không có thì lấy học kỳ đang diễn ra */
	private Integer chonHocKy(Integer hocKy, List<HocKy> dsHocKy) {
		if (hocKy != null) return hocKy;
		return dangKyHocPhanService.hocKyHienTai()
				.map(HocKy::getMaHocKy)
				.orElse(dsHocKy.isEmpty() ? null : dsHocKy.get(0).getMaHocKy());
	}

	private Integer napHocKy(Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa();
		Integer maHocKy = chonHocKy(hocKy, dsHocKy);
		model.addAttribute("menu", "lophocphan");
		model.addAttribute("dsHocKy", dsHocKy);
		model.addAttribute("hocKyChon", maHocKy);
		return maHocKy;
	}

	// ===================== TẦNG 1: CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(@RequestParam(required = false) Integer hocKy, Model model) {
		Integer maHocKy = napHocKy(hocKy, model);
		if (maHocKy != null)
			model.addAttribute("dsTomTat", lopService.tomTatTheoKhoa(maHocKy));
		return "phongdaotao/lophocphan";
	}

	// ===================== TẦNG 2: CHỌN NGÀNH TRONG KHOA =====================

	@GetMapping("/khoa/{maDonVi}")
	public String chonNganh(@PathVariable Integer maDonVi,
	                        @RequestParam(required = false) Integer hocKy,
	                        Model model, RedirectAttributes ra) {
		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy khoa.");
			return "redirect:/quanly/lophocphan";
		}

		Integer maHocKy = napHocKy(hocKy, model);
		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", khoa.getTenDonVi());

		if (maHocKy != null)
			model.addAttribute("dsNganh", lopService.tomTatTheoNganh(maHocKy, maDonVi));

		return "phongdaotao/lophocphan-khoa";
	}

	// ===================== TẦNG 3: DANH SÁCH LỚP =====================

	@GetMapping("/khoa/{maDonVi}/tatca")
	public String dsToanKhoa(@PathVariable Integer maDonVi,
	                         @RequestParam(required = false) Integer hocKy,
	                         Model model, RedirectAttributes ra) {
		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy khoa.");
			return "redirect:/quanly/lophocphan";
		}

		Integer maHocKy = napHocKy(hocKy, model);
		return bangLop(maHocKy,
				maHocKy == null ? List.of() : lopService.dsTheoKhoa(maHocKy, maDonVi),
				khoa.getTenDonVi() + " — tất cả các ngành",
				"/quanly/lophocphan/khoa/" + maDonVi, model);
	}

	@GetMapping("/khoa/{maDonVi}/nganh/{maNganh}")
	public String dsTheoNganh(@PathVariable Integer maDonVi,
	                          @PathVariable Integer maNganh,
	                          @RequestParam(required = false) Integer hocKy,
	                          Model model) {
		Integer maHocKy = napHocKy(hocKy, model);

		String ten = (maNganh == LopHocPhanService.NGANH_CHUNG)
				? "Lớp mở chung, không theo ngành"
				: nganhRepository.findById(maNganh).map(Nganh::getTenNganh).orElse("Ngành");

		return bangLop(maHocKy,
				maHocKy == null ? List.of() : lopService.dsTheoNganh(maHocKy, maDonVi, maNganh),
				ten, "/quanly/lophocphan/khoa/" + maDonVi, model);
	}

	/** Dựng bảng lớp, giống nhau cho toàn khoa và cho từng ngành */
	private String bangLop(Integer maHocKy, List<LopHocPhan> dsLop,
	                       String tenPhamVi, String duongDanVe, Model model) {
		model.addAttribute("tenPhamVi", tenPhamVi);
		model.addAttribute("duongDanVe", duongDanVe);
		model.addAttribute("dsLop", dsLop);
		model.addAttribute("siSo", dangKyHocPhanService.siSoCacLop(dsLop));
		model.addAttribute("lichTheoLop", lichHocService.lichTheoLop(
				dsLop.stream().map(LopHocPhan::getMaLopHocPhan).toList()));
		return "phongdaotao/lophocphan-ds";
	}

	// ===================== MỞ LỚP MỚI =====================

	@GetMapping("/them")
	public String formThem(@RequestParam(required = false) Integer hocKy,
	                       @RequestParam(required = false) String quayVe,
	                       Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa().stream()
				.filter(hk -> hk.getTrangThai() != 2)   // bỏ học kỳ đã kết thúc
				.toList();

		model.addAttribute("menu", "lophocphan");
		model.addAttribute("dsHocKy", dsHocKy);
		model.addAttribute("dsHocPhan", lopService.dsHocPhan());
		model.addAttribute("dsPhong", lopService.dsPhongDangDung());
		// Dữ liệu cho bộ lọc theo ngành ở ô chọn học phần
		model.addAttribute("dsNganh", nganhRepository.findAll(Sort.by("tenNganh")));
		model.addAttribute("nganhCuaHocPhan", lopService.nganhCuaHocPhan());
		model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);
		model.addAttribute("quayVe", noiBo(quayVe));
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
	                       @RequestParam(required = false) String quayVe,
	                       RedirectAttributes ra) {
		try {
			LopHocPhan lop = lopService.taoLop(maHocPhan, maHocKy, nhom, siSoToiDa, siSoToiThieu,
					thu, tietBatDau, soTiet, maPhong, loaiLop, khoaApDung);
			ra.addFlashAttribute("thongBao", "Đã tạo lớp " + lop.getMaLop()
					+ ". Bấm \"Mở ĐK\" để sinh viên đăng ký được.");

			String ve = noiBo(quayVe);
			return "redirect:" + (ve != null ? ve : "/quanly/lophocphan") + "?hocKy=" + maHocKy;

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
			return "redirect:/quanly/lophocphan/them"
					+ (noiBo(quayVe) != null ? "?quayVe=" + quayVe : "");
		}
	}

	// ===================== ĐỔI TRẠNG THÁI =====================

	@PostMapping("/{id}/trangthai")
	public String doiTrangThai(@PathVariable Integer id,
	                           @RequestParam int trangThai,
	                           @RequestParam(required = false) Integer hocKy,
	                           @RequestParam(required = false) String quayVe,
	                           RedirectAttributes ra) {
		try {
			LopHocPhan lop = lopService.doiTrangThai(id, trangThai);
			ra.addFlashAttribute("thongBao", "Đã cập nhật trạng thái lớp " + lop.getMaLop() + ".");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}

		String ve = noiBo(quayVe);
		if (ve == null) ve = "/quanly/lophocphan";
		return "redirect:" + ve + (hocKy != null ? "?hocKy=" + hocKy : "");
	}

	/**
	 * Chỉ nhận đường dẫn quay lại nằm trong chính màn hình này.
	 *
	 * Giá trị tới từ trình duyệt nên về nguyên tắc người dùng sửa được. Không
	 * kiểm thì nó thành chỗ để chuyển hướng ra trang ngoài, dùng cho lừa đảo
	 * đăng nhập. Chặn ngay từ đây rẻ hơn nhiều so với đi tin vào giao diện.
	 */
	private String noiBo(String duongDan) {
		if (duongDan == null) return null;
		if (!duongDan.startsWith("/quanly/lophocphan/")) return null;
		// "//" đầu chuỗi là đường dẫn tuyệt đối tới máy chủ khác
		if (duongDan.contains("//") || duongDan.contains("..")) return null;
		return duongDan;
	}
}
