package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.giangvien;

import java.security.Principal;
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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService.DongDiem;

/** Cổng giảng viên: danh sách lớp giảng dạy, nhập điểm, gửi duyệt */
@Controller
@RequestMapping("/giangvien")
@RequiredArgsConstructor
public class GiangVienController {

	private final NhapDiemService nhapDiemService;

	/**
	 * Lấy giảng viên theo tài khoản đang đăng nhập.
	 * Không dùng @ModelAttribute để tránh Spring tự tạo GiangVien rỗng
	 * và tự đổ tham số request vào entity.
	 */
	private GiangVien layGiangVien(Principal principal) {
		if (principal == null) return null;
		return nhapDiemService.giangVien(principal.getName());
	}

	@GetMapping
	public String trangChu() {
		return "redirect:/giangvien/lop";
	}

	// ===================== LỚP GIẢNG DẠY =====================
	@GetMapping("/lop")
	public String dsLop(Principal principal, Model model) {
		model.addAttribute("menu", "lop");
		GiangVien gv = layGiangVien(principal);
		if (gv == null) return "giangvien/lop";   // layout tự báo "chưa gắn hồ sơ giảng viên"

		List<PhanCongGiangDay> dsPhanCong = nhapDiemService.dsPhanCong(gv);

		// Sĩ số và tình trạng bảng điểm của từng lớp
		Map<Integer, Integer> siSo = new HashMap<>();
		Map<Integer, String> tinhTrang = new HashMap<>();
		for (PhanCongGiangDay pc : dsPhanCong) {
			Integer maLop = pc.getLopHocPhan().getMaLopHocPhan();
			List<DongDiem> bang = nhapDiemService.bangDiem(maLop);
			siSo.put(maLop, bang.size());
			tinhTrang.put(maLop, nhapDiemService.tinhTrang(bang));
		}

		model.addAttribute("dsPhanCong", dsPhanCong);
		model.addAttribute("siSo", siSo);
		model.addAttribute("tinhTrang", tinhTrang);
		return "giangvien/lop";
	}

	// ===================== NHẬP ĐIỂM =====================
	@GetMapping("/lop/{id}/diem")
	public String bangDiem(Principal principal,
	                       @PathVariable Integer id,
	                       Model model, RedirectAttributes ra) {
		model.addAttribute("menu", "lop");
		GiangVien gv = layGiangVien(principal);
		if (gv == null) return "redirect:/giangvien/lop";

		try {
			LopHocPhan lop = nhapDiemService.lopDuocDay(gv, id);
			List<DongDiem> bang = nhapDiemService.bangDiem(id);

			model.addAttribute("lop", lop);
			model.addAttribute("bang", bang);
			model.addAttribute("choPhepSua", nhapDiemService.choPhepSua(bang));
			model.addAttribute("tinhTrang", nhapDiemService.tinhTrang(bang));
			// Lớp chỉ nhập điểm được khi đang học (2) hoặc đã kết thúc (3)
			model.addAttribute("lopChoNhap", lop.getTrangThai() == 2 || lop.getTrangThai() == 3);
			return "giangvien/nhapdiem";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/giangvien/lop";
		}
	}

	@PostMapping("/lop/{id}/diem")
	public String luuDiem(Principal principal,
	                      @PathVariable Integer id,
	                      @RequestParam Map<String, String> params,
	                      RedirectAttributes ra) {
		GiangVien gv = layGiangVien(principal);
		if (gv == null) return "redirect:/giangvien/lop";

		try {
			int n = nhapDiemService.luuDiem(gv, id, params);
			ra.addFlashAttribute("thongBao", "Đã lưu điểm cho " + n + " sinh viên.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/giangvien/lop/" + id + "/diem";
	}

	@PostMapping("/lop/{id}/guiduyet")
	public String guiDuyet(Principal principal,
	                       @PathVariable Integer id,
	                       RedirectAttributes ra) {
		GiangVien gv = layGiangVien(principal);
		if (gv == null) return "redirect:/giangvien/lop";

		try {
			nhapDiemService.guiDuyet(gv, id);
			ra.addFlashAttribute("thongBao", "Đã gửi bảng điểm cho Phòng Đào tạo duyệt.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/giangvien/lop/" + id + "/diem";
	}
}