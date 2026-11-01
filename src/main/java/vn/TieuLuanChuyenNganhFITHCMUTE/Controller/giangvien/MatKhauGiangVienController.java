package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.giangvien;

import java.security.Principal;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/** Đổi mật khẩu của giảng viên đang đăng nhập */
@Controller
@RequestMapping("/giangvien/matkhau")
@RequiredArgsConstructor
public class MatKhauGiangVienController {

	private static final int DO_DAI_TOI_THIEU = 6;

	private final TaiKhoanRepository taiKhoanRepository;
	private final PasswordEncoder passwordEncoder;

	@GetMapping
	public String form(Model model) {
		model.addAttribute("menu", "matkhau");
		return "giangvien/matkhau";
	}

	@PostMapping
	public String doiMatKhau(@RequestParam String matKhauCu,
	                         @RequestParam String matKhauMoi,
	                         @RequestParam String xacNhan,
	                         Principal principal,
	                         RedirectAttributes ra) {

		TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(principal.getName()).orElse(null);

		if (tk == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy tài khoản.");
			return "redirect:/giangvien/matkhau";
		}

		// So mật khẩu cũ bằng matches chứ không so chuỗi: BCrypt sinh muối
		// ngẫu nhiên nên cùng một mật khẩu vẫn ra hai chuỗi băm khác nhau.
		if (!passwordEncoder.matches(matKhauCu, tk.getMatKhauBam())) {
			ra.addFlashAttribute("loi", "Mật khẩu hiện tại không đúng.");
			return "redirect:/giangvien/matkhau";
		}

		if (matKhauMoi == null || matKhauMoi.length() < DO_DAI_TOI_THIEU) {
			ra.addFlashAttribute("loi",
					"Mật khẩu mới phải có ít nhất " + DO_DAI_TOI_THIEU + " ký tự.");
			return "redirect:/giangvien/matkhau";
		}

		if (!matKhauMoi.equals(xacNhan)) {
			ra.addFlashAttribute("loi", "Hai ô mật khẩu mới không khớp nhau.");
			return "redirect:/giangvien/matkhau";
		}

		if (passwordEncoder.matches(matKhauMoi, tk.getMatKhauBam())) {
			ra.addFlashAttribute("loi", "Mật khẩu mới trùng với mật khẩu cũ.");
			return "redirect:/giangvien/matkhau";
		}

		tk.setMatKhauBam(passwordEncoder.encode(matKhauMoi));
		taiKhoanRepository.save(tk);

		ra.addFlashAttribute("thongBao", "Đã đổi mật khẩu.");
		return "redirect:/giangvien/matkhau";
	}
}
