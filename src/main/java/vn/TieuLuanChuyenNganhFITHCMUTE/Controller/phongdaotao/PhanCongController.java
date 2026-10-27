package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.PhanCongService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.PhanCongService.DongPhanCong;

/**
 * Phòng Đào tạo phân công và đổi giảng viên cho lớp học phần.
 *
 * Chia hai tầng theo khoa. Mỗi chuyên viên phụ trách một khoa, nên gộp gần
 * nghìn lớp của cả trường vào một bảng thì ai cũng phải cuộn qua phần việc
 * của người khác để tìm phần của mình, và rất dễ sửa nhầm lớp của khoa bạn.
 */
@Controller
@RequestMapping("/quanly/phancong")
@RequiredArgsConstructor
public class PhanCongController {

	private final PhanCongService phanCongService;
	private final HocKyService hocKyService;
	private final DonViRepository donViRepository;
	private final DangKyHocPhanService dangKyHocPhanService;

	// ===================== MÀN HÌNH CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(@RequestParam(required = false) Integer hocKy, Model model) {
		model.addAttribute("menu", "phancong");
		Integer maHocKy = napHocKy(hocKy, model);

		if (maHocKy != null)
			model.addAttribute("dsTomTat", phanCongService.tomTatTheoKhoa(maHocKy));

		return "phongdaotao/phancong";
	}

	// ===================== DANH SÁCH LỚP CỦA MỘT KHOA =====================

	@GetMapping("/khoa/{maDonVi}")
	public String danhSach(@PathVariable Integer maDonVi,
	                       @RequestParam(required = false) Integer hocKy,
	                       @RequestParam(defaultValue = "false") boolean chuaPhanCong,
	                       Model model) {

		model.addAttribute("menu", "phancong");
		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", tenKhoa(maDonVi));
		model.addAttribute("chuaPhanCong", chuaPhanCong);

		Integer maHocKy = napHocKy(hocKy, model);
		if (maHocKy == null) return "phongdaotao/phancong-khoa";

		List<DongPhanCong> ds = phanCongService.dsLop(maHocKy, chuaPhanCong, maDonVi);
		model.addAttribute("ds", ds);

		/*
		 * Giảng viên gợi ý cho từng lớp, đã gom sẵn theo khoa công tác.
		 *
		 * Nhiều lớp chung một khoa phụ trách nên nhớ lại kết quả theo mã
		 * khoa, thay vì hỏi lại cho từng lớp.
		 */
		Map<Integer, Map<String, List<GiangVien>>> theoKhoaPhuTrach = new HashMap<>();
		Map<Integer, Map<String, List<GiangVien>>> dsGv = new HashMap<>();

		for (DongPhanCong d : ds) {
			Integer maKhoaHp = d.getLop().getHocPhan().getDonVi() == null ? null
					: d.getLop().getHocPhan().getDonVi().getMaDonVi();
			Integer khoa = (maKhoaHp == null) ? -1 : maKhoaHp;

			dsGv.put(d.getLop().getMaLopHocPhan(),
					theoKhoaPhuTrach.computeIfAbsent(khoa,
							k -> phanCongService.dsGiangVien(maKhoaHp)));
		}
		model.addAttribute("dsGv", dsGv);

		return "phongdaotao/phancong-khoa";
	}

	// ===================== GIAO LỚP =====================

	@PostMapping("/khoa/{maDonVi}/{maLop}")
	public String doiGiangVien(@PathVariable Integer maDonVi,
	                           @PathVariable Integer maLop,
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
		return "redirect:/quanly/phancong/khoa/" + maDonVi
				+ "?hocKy=" + hocKy + "&chuaPhanCong=" + chuaPhanCong;
	}

	// ===================== DÙNG CHUNG =====================

	/** Mã -1 là nhóm gom các học phần chưa gắn khoa nào */
	private String tenKhoa(Integer maDonVi) {
		if (maDonVi != null && maDonVi == -1) return "Chưa rõ khoa phụ trách";
		return donViRepository.findById(maDonVi).map(DonVi::getTenDonVi).orElse("Khoa");
	}

	/**
	 * Nạp danh sách học kỳ và chọn sẵn một học kỳ.
	 *
	 * Không truyền gì thì lấy học kỳ đang diễn ra, để vào trang là thấy số
	 * liệu ngay thay vì một màn hình trống chờ chọn.
	 */
	private Integer napHocKy(Integer hocKy, Model model) {
		model.addAttribute("dsHocKy", hocKyService.tatCa());

		Integer maHocKy = hocKy != null ? hocKy
				: dangKyHocPhanService.hocKyHienTai().map(HocKy::getMaHocKy).orElse(null);
		model.addAttribute("hocKyChon", maHocKy);
		return maHocKy;
	}
}
