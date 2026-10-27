package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.giangvien;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichDayService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichHocService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService.DongDiem;

/** Cổng giảng viên: tổng quan, lịch dạy, lớp giảng dạy, nhập điểm */
@Controller
@RequestMapping("/giangvien")
@RequiredArgsConstructor
public class GiangVienController {

	private final NhapDiemService nhapDiemService;
	private final LichDayService lichDayService;

	// ===================== TỔNG QUAN =====================

	@GetMapping
	public String trangChu(@ModelAttribute("giangVien") GiangVien gv, Model model) {
		model.addAttribute("menu", "tongquan");
		model.addAttribute("homNay", LocalDate.now());
		if (gv == null) return "giangvien/trangchu";

		HocKy hocKy = lichDayService.hocKyMacDinh(gv);
		model.addAttribute("hocKy", hocKy);
		model.addAttribute("buoiHomNay", lichDayService.buoiDayHomNay(gv));
		model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);

		if (hocKy != null) {
			model.addAttribute("tq", lichDayService.tongQuan(gv, hocKy.getMaHocKy()));
		}
		return "giangvien/trangchu";
	}

	// ===================== LỊCH DẠY =====================

	@GetMapping("/lichday")
	public String lichDay(@ModelAttribute("giangVien") GiangVien gv,
	                      @RequestParam(required = false) Integer maHocKy,
	                      Model model) {
		model.addAttribute("menu", "lichday");
		if (gv == null) return "giangvien/lichday";

		List<HocKy> dsHocKy = lichDayService.dsHocKyCoLop(gv);
		model.addAttribute("dsHocKy", dsHocKy);

		// Chưa chọn gì thì mở học kỳ đang diễn ra
		if (maHocKy == null) {
			HocKy macDinh = lichDayService.hocKyMacDinh(gv);
			maHocKy = (macDinh == null) ? null : macDinh.getMaHocKy();
		}
		model.addAttribute("maHocKy", maHocKy);

		List<LichHoc> dsLich = lichDayService.lichDay(gv, maHocKy);
		model.addAttribute("dsLich", dsLich);
		model.addAttribute("tkb", lichDayService.thoiKhoaBieu(dsLich));
		model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);

		return "giangvien/lichday";
	}

	// ===================== LỚP GIẢNG DẠY =====================

	@GetMapping("/lop")
	public String dsLop(@ModelAttribute("giangVien") GiangVien gv,
	                    @RequestParam(required = false) Integer maHocKy,
	                    Model model) {
		model.addAttribute("menu", "lop");
		if (gv == null) return "giangvien/lop";

		model.addAttribute("dsHocKy", lichDayService.dsHocKyCoLop(gv));
		model.addAttribute("maHocKy", maHocKy);

		List<PhanCongGiangDay> dsPhanCong = lichDayService.phanCongTheoHocKy(gv, maHocKy);

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

	/** Chi tiết một lớp: thông tin, lịch học, danh sách sinh viên */
	@GetMapping("/lop/{id}")
	public String chiTietLop(@ModelAttribute("giangVien") GiangVien gv,
	                         @PathVariable Integer id,
	                         Model model, RedirectAttributes ra) {
		model.addAttribute("menu", "lop");
		if (gv == null) return "redirect:/giangvien/lop";

		try {
			PhanCongGiangDay pc = lichDayService.phanCongCuaLop(gv, id);
			LopHocPhan lop = pc.getLopHocPhan();

			List<DongDiem> bang = nhapDiemService.bangDiem(id);

			model.addAttribute("pc", pc);
			model.addAttribute("lop", lop);
			model.addAttribute("bang", bang);
			model.addAttribute("tinhTrang", nhapDiemService.tinhTrang(bang));
			model.addAttribute("laChinh", lichDayService.laGiangVienChinh(pc));
			model.addAttribute("dsLich", lichDayService.lichDay(gv, lop.getHocKy().getMaHocKy())
					.stream().filter(lh -> lh.getLopHocPhan().getMaLopHocPhan().equals(id)).toList());
			model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);

			return "giangvien/lopchitiet";
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/giangvien/lop";
		}
	}

	// ===================== NHẬP ĐIỂM =====================

	@GetMapping("/lop/{id}/diem")
	public String bangDiem(@ModelAttribute("giangVien") GiangVien gv,
	                       @PathVariable Integer id,
	                       Model model, RedirectAttributes ra) {
		model.addAttribute("menu", "lop");
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
	public String luuDiem(@ModelAttribute("giangVien") GiangVien gv,
	                      @PathVariable Integer id,
	                      @RequestParam Map<String, String> params,
	                      RedirectAttributes ra) {
		try {
			int n = nhapDiemService.luuDiem(gv, id, params);
			ra.addFlashAttribute("thongBao", "Đã lưu điểm cho " + n + " sinh viên.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/giangvien/lop/" + id + "/diem";
	}

	@PostMapping("/lop/{id}/guiduyet")
	public String guiDuyet(@ModelAttribute("giangVien") GiangVien gv,
	                       @PathVariable Integer id,
	                       RedirectAttributes ra) {
		try {
			nhapDiemService.guiDuyet(gv, id);
			ra.addFlashAttribute("thongBao", "Đã gửi bảng điểm cho Phòng Đào tạo duyệt.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/giangvien/lop/" + id + "/diem";
	}

	// ===================== HỒ SƠ =====================

	@GetMapping("/hoso")
	public String hoSo(Model model) {
		model.addAttribute("menu", "hoso");
		return "giangvien/hoso";
	}
}
