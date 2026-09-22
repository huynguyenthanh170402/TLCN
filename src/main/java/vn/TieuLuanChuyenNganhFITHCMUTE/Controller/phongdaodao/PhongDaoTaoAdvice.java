package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;


import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * Dữ liệu dùng chung cho mọi controller trong package phongdaotao.
 * basePackageClasses giới hạn phạm vi: chỉ áp dụng cho package này,
 * không ảnh hưởng controller của sinh viên hay khách.
 */
@ControllerAdvice(basePackageClasses = PhongDaoTaoAdvice.class)
public class PhongDaoTaoAdvice {

	/** Tên đăng nhập hiện ở thanh trên cùng của layout */
	@ModelAttribute("tenDangNhap")
	public String tenDangNhap(Principal principal) {
		return principal != null ? principal.getName() : "";
	}
}