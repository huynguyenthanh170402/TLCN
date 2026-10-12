package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.giangvien;

import java.security.Principal;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService;

/**
 * Dữ liệu dùng chung cho mọi controller trong package giangvien:
 * nạp giảng viên đang đăng nhập để layout hiện tên và các trang dùng lại.
 */
@ControllerAdvice(basePackageClasses = GiangVienAdvice.class)
@RequiredArgsConstructor
public class GiangVienAdvice {

	private final NhapDiemService nhapDiemService;

	@ModelAttribute("giangVien")
	public GiangVien giangVien(Principal principal) {
		if (principal == null) return null;
		return nhapDiemService.giangVien(principal.getName());
	}
}
