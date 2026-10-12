package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaodao;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DuyetDiemService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService;

/** Phòng Đào tạo: duyệt, trả lại, công bố bảng điểm */
@Controller
@RequestMapping("/quanly/duyetdiem")
@RequiredArgsConstructor
public class DuyetDiemController {

	private final DuyetDiemService duyetDiemService;
	private final NhapDiemService nhapDiemService;

	// ===================== DANH SÁCH =====================
	/** tt = 1 chờ duyệt (mặc định), 2 đã duyệt, 3 đã công bố */
	@GetMapping
	public String danhSach(@RequestParam(defaultValue = "1") int tt, Model model) {
		model.addAttribute("menu", "duyetdiem");
		model.addAttribute("tt", tt);
		model.addAttribute("dsLop", duyetDiemService.dsLop(tt));
		// Số lớp mỗi trạng thái, hiện trên các tab
		model.addAttribute("soChoDuyet", duyetDiemService.dsLop(1).size());
		model.addAttribute("soDaDuyet",  duyetDiemService.dsLop(2).size());
		model.addAttribute("soCongBo",   duyetDiemService.dsLop(3).size());
		return "phongdaotao/duyetdiem";
	}

	// ===================== CHI TIẾT =====================
	@GetMapping("/{id}")
	public String chiTiet(@PathVariable Integer id, Model model, RedirectAttributes ra) {
		try {
			LopHocPhan lop = duyetDiemService.lop(id);
			var bang = nhapDiemService.bangDiem(id);

			model.addAttribute("menu", "duyetdiem");
			model.addAttribute("lop", lop);
			model.addAttribute("bang", bang);
			model.addAttribute("tinhTrang", nhapDiemService.tinhTrang(bang));
			model.addAttribute("giangVien", duyetDiemService.giangVienChinh(id));
			return "phongdaotao/duyetdiem-chitiet";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/quanly/duyetdiem";
		}
	}

	// ===================== THAO TÁC =====================
	@PostMapping("/{id}/duyet")
	public String duyet(@PathVariable Integer id, RedirectAttributes ra) {
		try {
			duyetDiemService.duyet(id);
			ra.addFlashAttribute("thongBao", "Đã duyệt bảng điểm. Bấm \"Công bố\" để sinh viên xem được.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/duyetdiem/" + id;
	}

	@PostMapping("/{id}/tralai")
	public String traLai(@PathVariable Integer id, @RequestParam String lyDo, RedirectAttributes ra) {
		try {
			duyetDiemService.traLai(id, lyDo);
			ra.addFlashAttribute("thongBao", "Đã trả bảng điểm cho giảng viên sửa.");
			return "redirect:/quanly/duyetdiem";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/quanly/duyetdiem/" + id;
		}
	}

	@PostMapping("/{id}/congbo")
	public String congBo(@PathVariable Integer id, RedirectAttributes ra) {
		try {
			duyetDiemService.congBo(id);
			ra.addFlashAttribute("thongBao", "Đã công bố điểm. Sinh viên đã xem được kết quả.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/duyetdiem/" + id;
	}
}
