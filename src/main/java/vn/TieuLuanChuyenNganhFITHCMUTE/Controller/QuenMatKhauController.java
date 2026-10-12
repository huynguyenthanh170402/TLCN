package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.QuenMatKhauService;

/** Quên mật khẩu và đặt lại mật khẩu qua email */
@Controller
@RequiredArgsConstructor
public class QuenMatKhauController {

    private final QuenMatKhauService quenMatKhauService;

    /** Trang nhập email */
    @GetMapping("/quenmatkhau")
    public String formQuenMatKhau() {
        return "quenmatkhau";
    }

    /** Nhận email, gửi liên kết đặt lại */
    @PostMapping("/quenmatkhau")
    public String guiLienKet(@RequestParam String email, RedirectAttributes ra) {
        QuenMatKhauService.KetQua kq = quenMatKhauService.guiLienKet(email);
        ra.addFlashAttribute(kq.thanhCong() ? "thongBao" : "loi", kq.thongBao());
        return "redirect:/quenmatkhau";
    }

    /** Trang nhập mật khẩu mới, mở từ đường dẫn trong email */
    @GetMapping("/datlaimatkhau")
    public String formDatLai(@RequestParam(required = false) String token, Model model) {
        if (quenMatKhauService.kiemTraToken(token).isEmpty()) {
            model.addAttribute("tokenHong", true);
            return "datlaimatkhau";
        }
        model.addAttribute("token", token);
        return "datlaimatkhau";
    }

    /** Lưu mật khẩu mới */
    @PostMapping("/datlaimatkhau")
    public String datLai(@RequestParam String token,
                         @RequestParam String matKhauMoi,
                         @RequestParam String xacNhan,
                         RedirectAttributes ra) {

        QuenMatKhauService.KetQua kq =
                quenMatKhauService.datLaiMatKhau(token, matKhauMoi, xacNhan);

        if (kq.thanhCong()) {
            ra.addFlashAttribute("thongBao", kq.thongBao());
            return "redirect:/login";
        }

        ra.addFlashAttribute("loi", kq.thongBao());
        return "redirect:/datlaimatkhau?token=" + token;
    }
}