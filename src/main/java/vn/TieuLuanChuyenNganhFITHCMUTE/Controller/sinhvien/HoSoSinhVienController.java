package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.sinhvien;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.AnhDaiDienService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HoSoSinhVienService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HoSoSinhVienService.ThongTinLienHe;

/** Sinh viên sửa thông tin liên hệ, đổi mật khẩu và cập nhật ảnh đại diện */
@Controller
@RequestMapping("/sinhvien/hoso")
@RequiredArgsConstructor
public class HoSoSinhVienController {

	private final SinhVienRepository sinhVienRepository;
	private final HoSoSinhVienService hoSoService;
	private final AnhDaiDienService anhDaiDienService;

	private SinhVien laySinhVien(Principal principal) {
		if (principal == null) return null;
		return sinhVienRepository.timTheoTenDangNhap(principal.getName()).orElse(null);
	}

	@GetMapping("/sua")
	public String form(Principal principal, Model model) {
		model.addAttribute("menu", "hoso");
		model.addAttribute("sinhVien", laySinhVien(principal));
		return "sinhvien/hoso-sua";
	}

	@PostMapping("/capnhat")
	public String capNhat(Principal principal,
	                      @RequestParam(required = false) String emailCaNhan,
	                      @RequestParam(required = false) String soDienThoai,
	                      @RequestParam(required = false) String diaChiLienHe,
	                      @RequestParam(required = false) String hoTenNguoiLienHe,
	                      @RequestParam(required = false) String quanHeVoiSinhVien,
	                      @RequestParam(required = false) String soDienThoaiNguoiLienHe,
	                      @RequestParam(required = false) String diaChiNguoiLienHe,
	                      RedirectAttributes ra) {
		try {
			hoSoService.capNhatLienHe(laySinhVien(principal),
					new ThongTinLienHe(emailCaNhan, soDienThoai, diaChiLienHe,
							hoTenNguoiLienHe, quanHeVoiSinhVien,
							soDienThoaiNguoiLienHe, diaChiNguoiLienHe));
			ra.addFlashAttribute("thongBao", "Đã cập nhật thông tin liên hệ.");
			return "redirect:/sinhvien/hoso";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/sinhvien/hoso/sua";
		}
	}

	/** Trang đổi mật khẩu riêng, mở từ menu bên trái */
	@GetMapping("/matkhau")
	public String formMatKhau(Principal principal, Model model) {
		model.addAttribute("menu", "matkhau");
		model.addAttribute("sinhVien", laySinhVien(principal));
		return "sinhvien/doimatkhau";
	}

	@PostMapping("/doimatkhau")
	public String doiMatKhau(Principal principal,
	                         @RequestParam String matKhauCu,
	                         @RequestParam String matKhauMoi,
	                         @RequestParam String xacNhan,
	                         RedirectAttributes ra) {
		SinhVien sv = laySinhVien(principal);
		try {
			hoSoService.doiMatKhau(sv == null ? null : sv.getTaiKhoan(),
					matKhauCu, matKhauMoi, xacNhan);
			ra.addFlashAttribute("thongBao", "Đã đổi mật khẩu. Lần đăng nhập sau dùng mật khẩu mới.");
			return "redirect:/sinhvien/hoso";
		} catch (IllegalStateException e) {
			// Quay lại đúng trang đổi mật khẩu, không đẩy về trang sửa hồ sơ
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/sinhvien/hoso/matkhau";
		}
	}

	/** Tải ảnh đại diện mới */
	@PostMapping("/anh")
	public String capNhatAnh(Principal principal,
	                         @RequestParam("file") MultipartFile file,
	                         RedirectAttributes ra) {
		SinhVien sv = laySinhVien(principal);
		if (sv == null) {
			ra.addFlashAttribute("loi", "Tài khoản này chưa được gắn với hồ sơ sinh viên nào.");
			return "redirect:/sinhvien/hoso";
		}

		AnhDaiDienService.KetQua kq = anhDaiDienService.luuAnh(sv, file);
		ra.addFlashAttribute(kq.thanhCong() ? "thongBao" : "loi", kq.thongBao());
		return "redirect:/sinhvien/hoso";
	}

	/** Xóa ảnh đại diện, quay về ảnh mặc định */
	@PostMapping("/anh/xoa")
	public String xoaAnh(Principal principal, RedirectAttributes ra) {
		SinhVien sv = laySinhVien(principal);
		if (sv == null) {
			ra.addFlashAttribute("loi", "Tài khoản này chưa được gắn với hồ sơ sinh viên nào.");
			return "redirect:/sinhvien/hoso";
		}

		AnhDaiDienService.KetQua kq = anhDaiDienService.xoaAnh(sv);
		ra.addFlashAttribute(kq.thanhCong() ? "thongBao" : "loi", kq.thongBao());
		return "redirect:/sinhvien/hoso";
	}
}