package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

import java.security.Principal;
import java.util.List;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DuyetDangKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocKyService;

/**
 * Phòng Đào tạo xác nhận đăng ký học phần của sinh viên.
 *
 * Chia hai tầng theo khoa giống màn hình duyệt điểm và phân công giảng dạy,
 * vì cùng một lý do: mỗi chuyên viên phụ trách một khoa nên việc của ai phải
 * nằm gọn trong màn hình của người đó.
 */
@Controller
@RequestMapping("/quanly/duyetdangky")
@RequiredArgsConstructor
public class DuyetDangKyController {

	private final DuyetDangKyService duyetDangKyService;
	private final DangKyHocPhanService dangKyHocPhanService;
	private final HocKyService hocKyService;
	private final DonViRepository donViRepository;

	// ===================== MÀN HÌNH CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(@RequestParam(required = false) Integer hocKy, Model model) {
		model.addAttribute("menu", "duyetdangky");
		Integer maHocKy = napHocKy(hocKy, model);
		model.addAttribute("dsTomTat", duyetDangKyService.tomTatTheoKhoa(maHocKy));
		return "phongdaotao/duyetdangky";
	}

	// ===================== DANH SÁCH CỦA MỘT KHOA =====================

	/** tt = 0 chờ xác nhận (mặc định), 1 đã xác nhận, 2 bị từ chối */
	@GetMapping("/khoa/{maDonVi}")
	public String danhSach(@PathVariable Integer maDonVi,
	                       @RequestParam(required = false) Integer hocKy,
	                       @RequestParam(defaultValue = "0") int tt,
	                       Model model) {
		model.addAttribute("menu", "duyetdangky");
		Integer maHocKy = napHocKy(hocKy, model);

		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", tenKhoa(maDonVi));
		model.addAttribute("tt", tt);

		model.addAttribute("ds", duyetDangKyService.ds(maHocKy, tt, maDonVi));

		// Sĩ số từng lớp, để bảng hiện được "38/50" ngay cạnh mã lớp
		model.addAttribute("siSo", duyetDangKyService.siSoCacLop(maHocKy));
		model.addAttribute("siSoToiDa", DangKyHocPhanService.SI_SO_TOI_DA);

		// Số lượt mỗi trạng thái của riêng khoa này, hiện trên các tab
		model.addAttribute("soChoDuyet", duyetDangKyService.ds(maHocKy, 0, maDonVi).size());
		model.addAttribute("soDaDuyet",  duyetDangKyService.ds(maHocKy, 1, maDonVi).size());
		model.addAttribute("soTuChoi",   duyetDangKyService.ds(maHocKy, 2, maDonVi).size());

		return "phongdaotao/duyetdangky-khoa";
	}

	// ===================== THAO TÁC =====================

	@PostMapping("/{id}/duyet")
	public String duyet(@PathVariable Integer id,
	                    @RequestParam Integer maDonVi,
	                    @RequestParam Integer hocKy,
	                    Principal principal,
	                    RedirectAttributes ra) {
		try {
			duyetDangKyService.duyet(id, ten(principal));
			ra.addFlashAttribute("thongBao", "Đã xác nhận lượt đăng ký. "
					+ "Sinh viên thấy trạng thái mới ngay khi mở trang đăng ký học phần.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return ve(maDonVi, hocKy, 0);
	}

	@PostMapping("/{id}/tuchoi")
	public String tuChoi(@PathVariable Integer id,
	                     @RequestParam String lyDo,
	                     @RequestParam Integer maDonVi,
	                     @RequestParam Integer hocKy,
	                     Principal principal,
	                     RedirectAttributes ra) {
		try {
			duyetDangKyService.tuChoi(id, lyDo, ten(principal));
			ra.addFlashAttribute("thongBao", "Đã từ chối lượt đăng ký. "
					+ "Chỗ trong lớp và khoản học phí tương ứng đã được trả lại.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return ve(maDonVi, hocKy, 0);
	}

	@PostMapping("/duyethangloat")
	public String duyetHangLoat(@RequestParam(required = false) List<Integer> chon,
	                            @RequestParam Integer maDonVi,
	                            @RequestParam Integer hocKy,
	                            Principal principal,
	                            RedirectAttributes ra) {
		var kq = duyetDangKyService.duyetHangLoat(chon, ten(principal));

		// Nói rõ từng phần, đừng để chuyên viên tự đếm rồi hoang mang vì sao
		// tick 40 dòng mà chỉ 35 dòng đổi trạng thái
		if (kq.soDayLop() > 0)
			ra.addFlashAttribute("loi", kq.soDayLop() + " lượt không xác nhận được vì lớp "
					+ "đã đủ " + DangKyHocPhanService.SI_SO_TOI_DA + " sinh viên. "
					+ "Mở thêm lớp hoặc hướng dẫn những sinh viên này chọn lớp khác.");

		if (kq.soDuyet() == 0 && kq.soDayLop() == 0) {
			ra.addFlashAttribute("loi", "Chưa chọn dòng nào, hoặc những dòng đã chọn "
					+ "không còn ở trạng thái chờ xác nhận.");
		} else if (kq.soDuyet() > 0) {
			String tb = "Đã xác nhận " + kq.soDuyet() + " lượt đăng ký.";
			if (kq.soBoQua() > 0)
				tb += " " + kq.soBoQua() + " dòng bị bỏ qua vì sinh viên vừa hủy "
						+ "hoặc đã được xử lý trước đó.";
			ra.addFlashAttribute("thongBao", tb);
		}
		return ve(maDonVi, hocKy, 0);
	}

	// ===================== DÙNG CHUNG =====================

	/** Nạp danh sách học kỳ và chọn sẵn học kỳ đang diễn ra */
	private Integer napHocKy(Integer hocKy, Model model) {
		List<HocKy> dsHocKy = hocKyService.tatCa();
		model.addAttribute("dsHocKy", dsHocKy);

		Integer maHocKy = hocKy;
		if (maHocKy == null) {
			maHocKy = dangKyHocPhanService.hocKyHienTai()
					.map(HocKy::getMaHocKy)
					.orElse(dsHocKy.isEmpty() ? null : dsHocKy.get(0).getMaHocKy());
		}
		model.addAttribute("hocKyChon", maHocKy);
		return maHocKy;
	}

	private String ve(Integer maDonVi, Integer hocKy, int tt) {
		return "redirect:/quanly/duyetdangky/khoa/" + maDonVi + "?hocKy=" + hocKy + "&tt=" + tt;
	}

	private String ten(Principal principal) {
		return principal == null ? null : principal.getName();
	}

	/** Mã -1 là nhóm gom sinh viên chưa gắn ngành hoặc ngành chưa gắn khoa */
	private String tenKhoa(Integer maDonVi) {
		if (maDonVi != null && maDonVi == -1) return "Chưa rõ khoa quản lý";
		return donViRepository.findById(maDonVi).map(DonVi::getTenDonVi).orElse("Khoa");
	}
}
