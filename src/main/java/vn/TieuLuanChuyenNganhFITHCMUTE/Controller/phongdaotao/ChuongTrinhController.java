package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.phongdaotao;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ChuongTrinhService.KetQuaXoa;

/** Phòng Đào tạo: xem và chỉnh sửa chương trình đào tạo của từng ngành */
@Controller
@RequestMapping("/quanly/chuongtrinh")
@RequiredArgsConstructor
public class ChuongTrinhController {

	private final ChuongTrinhService chuongTrinhService;
	private final NganhRepository nganhRepository;
	private final DonViRepository donViRepository;

	// ===================== TẦNG 1: CHỌN KHOA =====================

	@GetMapping
	public String chonKhoa(Model model) {
		model.addAttribute("menu", "chuongtrinh");
		model.addAttribute("dsKhoa", chuongTrinhService.tongQuanCacKhoa());
		return "phongdaotao/chuongtrinh";
	}

	// ===================== TẦNG 2: CÁC NGÀNH CỦA MỘT KHOA =====================

	@GetMapping("/khoa/{maDonVi}")
	public String theoKhoa(@PathVariable Integer maDonVi, Model model) {
		model.addAttribute("menu", "chuongtrinh");
		model.addAttribute("maDonVi", maDonVi);
		model.addAttribute("tenKhoa", tenKhoa(maDonVi));
		model.addAttribute("dsNganh", chuongTrinhService.dsNganhCuaKhoa(maDonVi));
		return "phongdaotao/chuongtrinh-khoa";
	}

	// ===================== TẦNG 3: KHUNG CHƯƠNG TRÌNH MỘT NGÀNH =====================

	@GetMapping("/nganh/{maNganh}")
	public String theoNganh(@PathVariable Integer maNganh, Model model, RedirectAttributes ra) {
		Nganh nganh = nganhRepository.findById(maNganh).orElse(null);
		if (nganh == null) {
			ra.addFlashAttribute("loi", "Không tìm thấy ngành.");
			return "redirect:/quanly/chuongtrinh";
		}

		// Cho đường dẫn quay lại trỏ về đúng khoa của ngành này
		model.addAttribute("maDonVi",
				nganh.getDonVi() == null ? -1 : nganh.getDonVi().getMaDonVi());
		model.addAttribute("tenKhoa",
				nganh.getDonVi() == null ? "Chưa gắn khoa" : nganh.getDonVi().getTenDonVi());

		List<ChuongTrinhDaoTao> ds = chuongTrinhService.chiTiet(maNganh);
		int tongTinChi = ds.stream().mapToInt(ct -> ct.getHocPhan().getSoTinChi()).sum();
		int tinChiBatBuoc = ds.stream().filter(ct -> ct.getLoaiHocPhan() == 1)
				.mapToInt(ct -> ct.getHocPhan().getSoTinChi()).sum();

		model.addAttribute("menu", "chuongtrinh");
		model.addAttribute("nganh", nganh);
		model.addAttribute("ds", ds);
		model.addAttribute("tongTinChi", tongTinChi);
		model.addAttribute("tinChiBatBuoc", tinChiBatBuoc);
		model.addAttribute("tinChiTuChon", tongTinChi - tinChiBatBuoc);

		// Danh sách cho ô chọn ở hộp thoại Thêm học phần
		model.addAttribute("dsHocPhanThem", chuongTrinhService.hocPhanCoTheThem(maNganh));
		return "phongdaotao/chuongtrinh-nganh";
	}

	// ===================== THÊM =====================

	@PostMapping("/nganh/{maNganh}/them")
	public String them(@PathVariable Integer maNganh,
	                   @RequestParam Integer maHocPhanId,
	                   @RequestParam Integer hocKyDuKien,
	                   @RequestParam Integer khoiKienThuc,
	                   @RequestParam Integer loaiHocPhan,
	                   RedirectAttributes ra) {
		try {
			chuongTrinhService.them(maNganh, maHocPhanId, hocKyDuKien, khoiKienThuc, loaiHocPhan);
			ra.addFlashAttribute("thongBao", "Đã thêm học phần vào chương trình.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/chuongtrinh/nganh/" + maNganh;
	}

	// ===================== SỬA =====================

	@PostMapping("/nganh/{maNganh}/sua")
	public String sua(@PathVariable Integer maNganh,
	                  @RequestParam Integer maChuongTrinh,
	                  @RequestParam Integer hocKyDuKien,
	                  @RequestParam Integer khoiKienThuc,
	                  @RequestParam Integer loaiHocPhan,
	                  RedirectAttributes ra) {
		try {
			chuongTrinhService.sua(maChuongTrinh, hocKyDuKien, khoiKienThuc, loaiHocPhan);
			ra.addFlashAttribute("thongBao", "Đã cập nhật học phần trong chương trình.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/chuongtrinh/nganh/" + maNganh;
	}

	// ===================== XÓA =====================

	@PostMapping("/nganh/{maNganh}/xoa")
	public String xoa(@PathVariable Integer maNganh,
	                  @RequestParam Integer maChuongTrinh,
	                  RedirectAttributes ra) {
		try {
			KetQuaXoa kq = chuongTrinhService.xoa(maChuongTrinh);

			// Nói rõ vừa xóa hẳn hay chỉ ngừng áp dụng, vì hai việc này khác
			// nhau và người dùng cần biết dữ liệu cũ còn hay mất.
			ra.addFlashAttribute("thongBao", kq.xoaHan()
					? "Đã gỡ học phần khỏi chương trình."
					: "Đã ngừng áp dụng học phần này. Không xóa hẳn vì đã có "
					  + kq.soSinhVienDaHoc() + " lượt sinh viên của ngành đăng ký học, "
					  + "cần giữ khung cũ để còn xét tốt nghiệp.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/quanly/chuongtrinh/nganh/" + maNganh;
	}

	// ===================== DÙNG CHUNG =====================

	/** Mã -1 là nhóm gom các ngành chưa gắn khoa nào */
	private String tenKhoa(Integer maDonVi) {
		if (maDonVi != null && maDonVi == -1) return "Chưa gắn khoa";
		return donViRepository.findById(maDonVi).map(DonVi::getTenDonVi).orElse("Khoa");
	}
}
