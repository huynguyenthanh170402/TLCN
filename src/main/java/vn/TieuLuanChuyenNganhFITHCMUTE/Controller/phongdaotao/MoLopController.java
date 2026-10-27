package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MoLopHangLoatService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MoLopHangLoatService.KetQuaMoLop;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.MoLopHangLoatService.PhamVi;

/**
 * Phòng Đào tạo mở lớp chính khóa hàng loạt theo kế hoạch đào tạo.
 *
 * Chia ba tầng, đi từ rộng xuống hẹp:
 *
 *   1. Khối đại cương và từng khoa
 *   2. Trong một khoa: toàn khoa hoặc từng ngành
 *   3. Bảng kế hoạch của riêng phần đã chọn, kèm nút mở lớp
 *
 * Gộp cả trường vào một bảng thì người phụ trách một khoa phải dò trong vài
 * trăm dòng, và nút "Mở các lớp còn thiếu" mở luôn lớp của khoa khác nên
 * không ai dám bấm. Trong một khoa cũng vậy: mỗi ngành một khung chương
 * trình riêng, trộn chung thì phải tự dò dòng nào của ngành nào.
 */
@Controller
@RequestMapping("/quanly/molop")
@RequiredArgsConstructor
public class MoLopController {

	private final MoLopHangLoatService moLopService;
	private final HocKyService hocKyService;
	private final DonViRepository donViRepository;
	private final NganhRepository nganhRepository;
	private final DangKyHocPhanService dangKyHocPhanService;

	// ===================== TẦNG 1: CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(@RequestParam(required = false) Integer hocKy, Model model) {
		model.addAttribute("menu", "molop");
		Integer maHocKy = napHocKy(hocKy, model);
		if (maHocKy == null) return "phongdaotao/molop";

		try {
			model.addAttribute("dsTomTat", moLopService.tomTat(maHocKy));
		} catch (IllegalStateException e) {
			model.addAttribute("loi", e.getMessage());
		}
		return "phongdaotao/molop";
	}

	// ===================== TẦNG 2: CHỌN NGÀNH TRONG KHOA =====================

	@GetMapping("/khoa/{maDonVi}")
	public String chonNganh(@PathVariable Integer maDonVi,
	                        @RequestParam(required = false) Integer hocKy,
	                        Model model, RedirectAttributes ra) {

		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy khoa.");
			return "redirect:/quanly/molop";
		}

		model.addAttribute("menu", "molop");
		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", khoa.getTenDonVi());

		Integer maHocKy = napHocKy(hocKy, model);
		if (maHocKy == null) return "phongdaotao/molop-khoa";

		try {
			model.addAttribute("dsNganh", moLopService.tomTatNganh(maHocKy, maDonVi));
		} catch (IllegalStateException e) {
			model.addAttribute("loi", e.getMessage());
		}
		return "phongdaotao/molop-khoa";
	}

	// ===================== TẦNG 3: BẢNG KẾ HOẠCH =====================

	// --- Khối đại cương ---

	@GetMapping("/daicuong")
	public String keHoachDaiCuong(@RequestParam(required = false) Integer hocKy, Model model) {
		return trangKeHoach(PhamVi.daiCuong(), "Khối đại cương",
				"Học phần đại cương gom chung toàn trường theo khóa, không thuộc khoa nào. "
				+ "Mỗi lớp nhận sinh viên của mọi ngành có học phần đó trong chương trình.",
				"/quanly/molop/daicuong", "/quanly/molop", hocKy, model);
	}

	@PostMapping("/daicuong")
	public String moLopDaiCuong(@RequestParam Integer hocKy,
	                            @RequestParam(defaultValue = "false") boolean moDangKyNgay,
	                            RedirectAttributes ra) {
		thucHien(PhamVi.daiCuong(), hocKy, moDangKyNgay, ra);
		return "redirect:/quanly/molop/daicuong?hocKy=" + hocKy;
	}

	// --- Toàn bộ một khoa ---

	@GetMapping("/khoa/{maDonVi}/tatca")
	public String keHoachKhoa(@PathVariable Integer maDonVi,
	                          @RequestParam(required = false) Integer hocKy,
	                          Model model, RedirectAttributes ra) {
		DonVi khoa = donViRepository.findById(maDonVi).orElse(null);
		if (khoa == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy khoa.");
			return "redirect:/quanly/molop";
		}
		return trangKeHoach(PhamVi.khoa(maDonVi),
				khoa.getTenDonVi() + " — tất cả các ngành",
				"Gồm học phần cơ sở ngành, chuyên ngành và tốt nghiệp của mọi ngành do "
				+ "khoa này quản lý. Học phần đại cương nằm ở mục riêng.",
				"/quanly/molop/khoa/" + maDonVi + "/tatca",
				"/quanly/molop/khoa/" + maDonVi, hocKy, model);
	}

	@PostMapping("/khoa/{maDonVi}/tatca")
	public String moLopKhoa(@PathVariable Integer maDonVi,
	                        @RequestParam Integer hocKy,
	                        @RequestParam(defaultValue = "false") boolean moDangKyNgay,
	                        RedirectAttributes ra) {
		thucHien(PhamVi.khoa(maDonVi), hocKy, moDangKyNgay, ra);
		return "redirect:/quanly/molop/khoa/" + maDonVi + "/tatca?hocKy=" + hocKy;
	}

	// --- Một ngành ---

	@GetMapping("/khoa/{maDonVi}/nganh/{maNganh}")
	public String keHoachNganh(@PathVariable Integer maDonVi,
	                           @PathVariable Integer maNganh,
	                           @RequestParam(required = false) Integer hocKy,
	                           Model model, RedirectAttributes ra) {
		Nganh nganh = nganhRepository.findById(maNganh).orElse(null);
		if (nganh == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy ngành.");
			return "redirect:/quanly/molop/khoa/" + maDonVi;
		}
		return trangKeHoach(PhamVi.nganh(maNganh), nganh.getTenNganh(),
				"Chỉ gồm học phần mở riêng cho ngành này. Học phần đại cương mà sinh viên "
				+ "ngành vẫn phải học được mở chung toàn trường ở mục Khối đại cương.",
				"/quanly/molop/khoa/" + maDonVi + "/nganh/" + maNganh,
				"/quanly/molop/khoa/" + maDonVi, hocKy, model);
	}

	@PostMapping("/khoa/{maDonVi}/nganh/{maNganh}")
	public String moLopNganh(@PathVariable Integer maDonVi,
	                         @PathVariable Integer maNganh,
	                         @RequestParam Integer hocKy,
	                         @RequestParam(defaultValue = "false") boolean moDangKyNgay,
	                         RedirectAttributes ra) {
		thucHien(PhamVi.nganh(maNganh), hocKy, moDangKyNgay, ra);
		return "redirect:/quanly/molop/khoa/" + maDonVi + "/nganh/" + maNganh
				+ "?hocKy=" + hocKy;
	}

	// ===================== DÙNG CHUNG =====================

	/**
	 * Trang kế hoạch, giống nhau cho đại cương, toàn khoa và từng ngành.
	 *
	 * Đường dẫn của chính trang này được truyền vào thay vì để giao diện tự
	 * dựng lại theo từng trường hợp: biểu mẫu đổi học kỳ và nút mở lớp đều
	 * gửi về đúng một chỗ, nên không có cách nào lỡ mở lớp của phạm vi khác.
	 */
	private String trangKeHoach(PhamVi phamVi, String tenPhamVi, String moTa,
	                            String duongDan, String duongDanVe,
	                            Integer hocKy, Model model) {
		model.addAttribute("menu", "molop");
		model.addAttribute("tenPhamVi", tenPhamVi);
		model.addAttribute("moTaPhamVi", moTa);
		model.addAttribute("duongDan", duongDan);
		model.addAttribute("duongDanVe", duongDanVe);

		Integer maHocKy = napHocKy(hocKy, model);
		if (maHocKy == null) return "phongdaotao/molop-chitiet";

		try {
			model.addAttribute("keHoach", moLopService.keHoach(maHocKy, phamVi));
		} catch (IllegalStateException e) {
			model.addAttribute("loi", e.getMessage());
		}
		return "phongdaotao/molop-chitiet";
	}

	/** Mở lớp rồi gắn thông báo kết quả vào lần chuyển trang kế tiếp */
	private void thucHien(PhamVi phamVi, Integer hocKy, boolean moDangKyNgay, RedirectAttributes ra) {
		try {
			KetQuaMoLop kq = moLopService.moLop(hocKy, phamVi, moDangKyNgay);

			if (kq.getSoLopDaTao() == 0)
				ra.addFlashAttribute("thongBao", "Không có lớp nào cần mở thêm.");
			else
				ra.addFlashAttribute("thongBao", "Đã mở " + kq.getSoLopDaTao() + " lớp chính khóa."
						+ (moDangKyNgay ? " Sinh viên đăng ký được ngay."
						                : " Lớp đang ở trạng thái Chưa mở."));

			if (!kq.getCanhBao().isEmpty())
				ra.addFlashAttribute("loi", String.join(" ", kq.getCanhBao()));

		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
	}

	/**
	 * Nạp danh sách học kỳ và chọn sẵn một học kỳ.
	 *
	 * Không truyền gì thì lấy học kỳ đang diễn ra, để vào trang là thấy số
	 * liệu ngay thay vì một màn hình trống chờ chọn.
	 */
	private Integer napHocKy(Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa().stream()
				.filter(hk -> hk.getTrangThai() != 2)
				.toList();
		model.addAttribute("dsHocKy", dsHocKy);

		Integer maHocKy = hocKy != null ? hocKy
				: dangKyHocPhanService.hocKyHienTai().map(HocKy::getMaHocKy).orElse(null);
		model.addAttribute("hocKyChon", maHocKy);
		return maHocKy;
	}
}
