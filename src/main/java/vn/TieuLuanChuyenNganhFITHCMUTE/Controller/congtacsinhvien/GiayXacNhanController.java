package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.congtacsinhvien;

import java.security.Principal;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiayXacNhanSinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.GiayXacNhanService;

/** Phòng Công tác Sinh viên cấp và quản lý giấy xác nhận sinh viên */
@Controller
@RequestMapping("/ctsv/giayxacnhan")
@RequiredArgsConstructor
public class GiayXacNhanController {

	private final GiayXacNhanService giayService;
	private final SinhVienRepository sinhVienRepository;

	// ===================== SỔ CẤP GIẤY =====================

	@GetMapping
	public String so(@RequestParam(required = false) String tuKhoa,
	                 @RequestParam(required = false) Integer trangThai,
	                 @RequestParam(required = false) String timSv,
	                 @RequestParam(defaultValue = "0") int page,
	                 Model model) {

		model.addAttribute("menu", "giayxacnhan");
		model.addAttribute("tuKhoa", tuKhoa);
		model.addAttribute("trangThaiChon", trangThai);
		model.addAttribute("trang", giayService.traCuu(tuKhoa, trangThai, page));

		// Ô tìm sinh viên để cấp giấy mới, nằm chung trang cho đỡ phải
		// nhảy qua lại giữa hai màn hình
		model.addAttribute("timSv", timSv);
		if (timSv != null && !timSv.isBlank()) {
			model.addAttribute("ketQuaSv",
					sinhVienRepository.timNhanh(timSv.trim(), PageRequest.of(0, 10)));
		}
		return "congtacsinhvien/giayxacnhan";
	}

	// ===================== CẤP GIẤY =====================

	@GetMapping("/cap")
	public String formCap(@RequestParam Integer maSinhVien, Model model, RedirectAttributes ra) {
		SinhVien sv = sinhVienRepository.timTheoMa(maSinhVien).orElse(null);
		if (sv == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy sinh viên.");
			return "redirect:/ctsv/giayxacnhan";
		}

		model.addAttribute("menu", "giayxacnhan");
		model.addAttribute("sv", sv);
		model.addAttribute("tenLop", giayService.tenLop(sv));
		model.addAttribute("soThangMacDinh", GiayXacNhanService.SO_THANG_MAC_DINH);
		return "congtacsinhvien/giayxacnhan-cap";
	}

	@PostMapping("/cap")
	public String cap(@RequestParam Integer maSinhVien,
	                  @RequestParam Integer mucDich,
	                  @RequestParam(required = false) String mucDichKhac,
	                  @RequestParam(required = false) String noiNhan,
	                  @RequestParam(required = false) Integer soThang,
	                  Principal principal,
	                  RedirectAttributes ra) {
		try {
			GiayXacNhanSinhVien g = giayService.cap(maSinhVien, mucDich, mucDichKhac,
					noiNhan, soThang, principal == null ? null : principal.getName());

			// Cấp xong mở thẳng bản in: cán bộ cấp giấy là để in ra ký,
			// bắt quay về danh sách rồi tự tìm lại là thừa một bước.
			ra.addFlashAttribute("thongBao", "Đã cấp giấy số " + g.getSoGiay() + ".");
			return "redirect:/ctsv/giayxacnhan/" + g.getMaGiay() + "/in";

		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/ctsv/giayxacnhan/cap?maSinhVien=" + maSinhVien;
		}
	}

	// ===================== BẢN IN =====================

	/** Trang in đứng riêng, không dùng khung sidebar, để in ra đúng khổ A4 */
	@GetMapping("/{maGiay}/in")
	public String ban_in(@PathVariable Integer maGiay, Model model, RedirectAttributes ra) {
		try {
			GiayXacNhanSinhVien g = giayService.timChiTiet(maGiay);
			model.addAttribute("g", g);
			model.addAttribute("sv", g.getSinhVien());
			return "congtacsinhvien/giayxacnhan-in";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/ctsv/giayxacnhan";
		}
	}

	// ===================== THU HỒI =====================

	@PostMapping("/{maGiay}/thuhoi")
	public String thuHoi(@PathVariable Integer maGiay,
	                     @RequestParam String lyDo,
	                     RedirectAttributes ra) {
		try {
			giayService.thuHoi(maGiay, lyDo);
			ra.addFlashAttribute("thongBao", "Đã thu hồi giấy xác nhận.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/ctsv/giayxacnhan";
	}
}
