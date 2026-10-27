package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DuyetDiemService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService;

/**
 * Phòng Đào tạo: duyệt, trả lại, công bố bảng điểm.
 *
 * Chia hai tầng theo khoa giống màn hình phân công, vì cùng một lý do: mỗi
 * chuyên viên phụ trách một khoa, nên việc của ai phải nằm gọn trong màn
 * hình của người đó.
 */
@Controller
@RequestMapping("/quanly/duyetdiem")
@RequiredArgsConstructor
public class DuyetDiemController {

	private final DuyetDiemService duyetDiemService;
	private final NhapDiemService nhapDiemService;
	private final DonViRepository donViRepository;

	// ===================== MÀN HÌNH CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(Model model) {
		model.addAttribute("menu", "duyetdiem");
		model.addAttribute("dsTomTat", duyetDiemService.tomTatTheoKhoa());
		return "phongdaotao/duyetdiem";
	}

	// ===================== DANH SÁCH CỦA MỘT KHOA =====================

	/** tt = 1 chờ duyệt (mặc định), 2 đã duyệt, 3 đã công bố */
	@GetMapping("/khoa/{maDonVi}")
	public String danhSach(@PathVariable Integer maDonVi,
	                       @RequestParam(defaultValue = "1") int tt,
	                       Model model) {
		model.addAttribute("menu", "duyetdiem");
		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", tenKhoa(maDonVi));
		model.addAttribute("tt", tt);

		model.addAttribute("dsLop", duyetDiemService.dsLop(tt, maDonVi));

		// Số lớp mỗi trạng thái của riêng khoa này, hiện trên các tab
		model.addAttribute("soChoDuyet", duyetDiemService.dsLop(1, maDonVi).size());
		model.addAttribute("soDaDuyet",  duyetDiemService.dsLop(2, maDonVi).size());
		model.addAttribute("soCongBo",   duyetDiemService.dsLop(3, maDonVi).size());

		return "phongdaotao/duyetdiem-khoa";
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

			// Để nút quay lại trở về đúng khoa của lớp này
			model.addAttribute("maDonVi", DuyetDiemService.maKhoa(lop));
			model.addAttribute("tenKhoa", DuyetDiemService.tenKhoa(lop));

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
			LopHocPhan lop = duyetDiemService.lop(id);
			duyetDiemService.traLai(id, lyDo);
			ra.addFlashAttribute("thongBao", "Đã trả bảng điểm cho giảng viên sửa.");
			return "redirect:/quanly/duyetdiem/khoa/" + DuyetDiemService.maKhoa(lop);
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

	// ===================== DÙNG CHUNG =====================

	/** Mã -1 là nhóm gom các học phần chưa gắn khoa nào */
	private String tenKhoa(Integer maDonVi) {
		if (maDonVi != null && maDonVi == -1) return "Chưa rõ khoa phụ trách";
		return donViRepository.findById(maDonVi).map(DonVi::getTenDonVi).orElse("Khoa");
	}
}
