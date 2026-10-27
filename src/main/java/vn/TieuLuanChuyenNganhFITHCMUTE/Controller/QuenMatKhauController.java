package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.QuenMatKhauService;

/** Quên mật khẩu: nhận email, cấp mật khẩu tạm gửi về hộp thư */
@Controller
@RequiredArgsConstructor
public class QuenMatKhauController {

    private final QuenMatKhauService quenMatKhauService;

    @GetMapping("/quenmatkhau")
    public String form() {
        return "quenmatkhau";
    }

    @PostMapping("/quenmatkhau")
    public String gui(@RequestParam String email, RedirectAttributes ra) {
        QuenMatKhauService.KetQua kq = quenMatKhauService.capMatKhauTam(email);
        ra.addFlashAttribute(kq.thanhCong() ? "thongBao" : "loi", kq.thongBao());
        return "redirect:/quenmatkhau";
    }
}
