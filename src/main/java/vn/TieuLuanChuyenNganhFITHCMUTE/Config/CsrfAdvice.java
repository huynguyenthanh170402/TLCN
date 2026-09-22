package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Buộc Spring Security tạo CSRF token (và session) TRƯỚC khi render view.
 *
 * Mặc định token được tạo trễ — chỉ khi template gặp thẻ form.
 * Với trang dài, lúc đó phản hồi có thể đã gửi một phần cho trình duyệt,
 * không tạo session được nữa → lỗi "response has been committed", trang trắng.
 */
@ControllerAdvice
public class CsrfAdvice {

	@ModelAttribute
	public void napCsrfToken(CsrfToken csrfToken) {
		if (csrfToken != null) {
			csrfToken.getToken();   // gọi để token được tạo và lưu vào session ngay
		}
	}
}