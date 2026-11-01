package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.QuenMatKhauService;

/**
 * Màn hình bắt buộc đổi mật khẩu ngay sau khi đăng nhập bằng mật khẩu tạm.
 *
 * Người dùng không vào được trang nào khác cho tới khi đổi xong, nhờ
 * MatKhauTamConfig chặn lại ở mọi yêu cầu.
 */
@Controller
@RequiredArgsConstructor
public class DoiMatKhauTamController {

    /** Cờ trong phiên, đặt sau khi đổi xong để khỏi tra cơ sở dữ liệu mỗi lần */
    public static final String CO_DA_DOI = "matKhauTamDaDoi";

    private final QuenMatKhauService quenMatKhauService;

    @GetMapping("/doimatkhautam")
    public String form(Model model) {
        model.addAttribute("page", "doimatkhautam");
        return "doimatkhautam";
    }

    @PostMapping("/doimatkhautam")
    public String doi(@RequestParam String matKhauMoi,
                      @RequestParam String xacNhan,
                      Principal principal,
                      HttpSession session,
                      RedirectAttributes ra) {

        QuenMatKhauService.KetQua kq =
                quenMatKhauService.doiMatKhauTam(principal.getName(), matKhauMoi, xacNhan);

        if (!kq.thanhCong()) {
            ra.addFlashAttribute("loi", kq.thongBao());
            return "redirect:/doimatkhautam";
        }

        session.setAttribute(CO_DA_DOI, Boolean.TRUE);
        ra.addFlashAttribute("thongBao", kq.thongBao());

        // Về lại ngã ba điều hướng để đi đúng khu theo vai trò
        return "redirect:/dashboard";
    }
}
