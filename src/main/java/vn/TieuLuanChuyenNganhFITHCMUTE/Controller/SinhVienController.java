package vn.TieuLuanChuyenNganhFITHCMUTE.Controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.DangKyHocPhanService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.HocPhiService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KetQuaHocTapService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichHocService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.ThongBaoService;

@Controller
@RequestMapping("/sinhvien")
@RequiredArgsConstructor
public class SinhVienController {

	private final SinhVienRepository sinhVienRepository;
	private final KetQuaHocTapService ketQuaHocTapService;
	private final DangKyHocPhanService dangKyHocPhanService;
	private final HocPhiService hocPhiService;
	private final LichHocService lichHocService;
	private final ThongBaoService thongBaoService;

	@ModelAttribute("sinhVien")
	public SinhVien sinhVienHienTai(Principal principal) {
		if (principal == null) return null;
		return sinhVienRepository.timTheoTenDangNhap(principal.getName()).orElse(null);
	}

	/** Số thông báo chưa đọc, hiện badge đỏ trên sidebar ở mọi trang */
	@ModelAttribute("soThongBaoChuaDoc")
	public long soThongBaoChuaDoc(@ModelAttribute("sinhVien") SinhVien sv) {
		if (sv == null) return 0;
		return thongBaoService.soChuaDoc(sv.getTaiKhoan());
	}

	@GetMapping
	public String trangChu() {
		return "redirect:/sinhvien/hoso";
	}

	// ===================== HỒ SƠ =====================
	@GetMapping("/hoso")
	public String hoSo(Model model) {
		model.addAttribute("menu", "hoso");
		return "sinhvien/hoso";
	}

	// ===================== KẾT QUẢ HỌC TẬP =====================
	@GetMapping("/ketqua")
	public String ketQua(@ModelAttribute("sinhVien") SinhVien sv,
	                     @RequestParam(defaultValue = "0") int page,
	                     @RequestParam(required = false) Integer hocKy,
	                     Model model) {
		model.addAttribute("menu", "ketqua");
		if (sv == null) return "sinhvien/ketqua";

		List<KetQuaHocPhan> tatCa = ketQuaHocTapService.tatCa(sv.getMaSinhVien());
		model.addAttribute("tongKet", ketQuaHocTapService.tinhTongKet(tatCa));
		model.addAttribute("dsHocKy", ketQuaHocTapService.dsHocKy(tatCa));

		Page<KetQuaHocPhan> trang = ketQuaHocTapService.phanTrang(sv.getMaSinhVien(), hocKy, page);
		model.addAttribute("trang", trang);
		model.addAttribute("hocKyChon", hocKy);

		if (hocKy != null) {
			List<KetQuaHocPhan> cuaHocKy = tatCa.stream()
					.filter(kq -> hocKy.equals(
							kq.getDangKyHocPhan().getLopHocPhan().getHocKy().getMaHocKy()))
					.toList();
			model.addAttribute("tongKetHocKy", ketQuaHocTapService.tinhTongKet(cuaHocKy));
		}
		return "sinhvien/ketqua";
	}

	// ===================== ĐĂNG KÝ HỌC PHẦN =====================
	@GetMapping("/dangky")
	public String dangKy(@ModelAttribute("sinhVien") SinhVien sv, Model model) {
		model.addAttribute("menu", "dangky");
		if (sv == null) return "sinhvien/dangky";

		HocKy hk = dangKyHocPhanService.hocKyHienTai().orElse(null);
		model.addAttribute("hocKy", hk);
		if (hk == null) return "sinhvien/dangky";

		List<LopHocPhan> dsLop = dangKyHocPhanService.dsLopDangMo(hk);
		List<DangKyHocPhan> daDangKy = dangKyHocPhanService.dsDaDangKy(sv, hk);

		model.addAttribute("dsLop", dsLop);
		model.addAttribute("siSo", dangKyHocPhanService.siSoCacLop(dsLop));
		model.addAttribute("daDangKy", daDangKy);
		model.addAttribute("tongTinChi", dangKyHocPhanService.tongTinChi(daDangKy));
		model.addAttribute("tinChiToiDa", DangKyHocPhanService.TIN_CHI_TOI_DA);

		// Lịch học của từng lớp đang mở, để sinh viên xem trước khi đăng ký
		model.addAttribute("lichTheoLop", lichHocService.lichTheoLop(
				dsLop.stream().map(LopHocPhan::getMaLopHocPhan).toList()));

		Set<Integer> lopDaDangKy = daDangKy.stream()
				.map(dk -> dk.getLopHocPhan().getMaLopHocPhan()).collect(Collectors.toSet());
		Set<Integer> monDaDangKy = daDangKy.stream()
				.map(dk -> dk.getLopHocPhan().getHocPhan().getMaHocPhanId()).collect(Collectors.toSet());
		model.addAttribute("lopDaDangKy", lopDaDangKy);
		model.addAttribute("monDaDangKy", monDaDangKy);

		return "sinhvien/dangky";
	}

	@PostMapping("/dangky/{maLop}")
	public String xuLyDangKy(@ModelAttribute("sinhVien") SinhVien sv,
	                         @PathVariable Integer maLop, RedirectAttributes ra) {
		try {
			dangKyHocPhanService.dangKy(sv, maLop);
			ra.addFlashAttribute("thongBao", "Đăng ký thành công.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/sinhvien/dangky";
	}

	@PostMapping("/dangky/huy/{maDangKy}")
	public String xuLyHuy(@ModelAttribute("sinhVien") SinhVien sv,
	                      @PathVariable Integer maDangKy, RedirectAttributes ra) {
		try {
			dangKyHocPhanService.huy(sv, maDangKy);
			ra.addFlashAttribute("thongBao", "Đã hủy đăng ký.");
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
		}
		return "redirect:/sinhvien/dangky";
	}

	// ===================== LỊCH HỌC =====================
	@GetMapping("/lichhoc")
	public String lichHoc(@ModelAttribute("sinhVien") SinhVien sv, Model model) {
		model.addAttribute("menu", "lichhoc");
		if (sv == null) return "sinhvien/lichhoc";

		HocKy hk = dangKyHocPhanService.hocKyHienTai().orElse(null);
		model.addAttribute("hocKy", hk);
		if (hk == null) return "sinhvien/lichhoc";

		List<LichHoc> dsLich = lichHocService.lichCuaCacDangKy(dangKyHocPhanService.dsDaDangKy(sv, hk));
		model.addAttribute("dsLich", dsLich);
		model.addAttribute("tkb", lichHocService.thoiKhoaBieu(dsLich));
		model.addAttribute("gioBatDau", LichHocService.GIO_BAT_DAU);

		return "sinhvien/lichhoc";
	}

	// ===================== THÔNG BÁO =====================
	@GetMapping("/thongbao")
	public String thongBao(@ModelAttribute("sinhVien") SinhVien sv,
	                       @RequestParam(defaultValue = "0") int page,
	                       Model model) {
		model.addAttribute("menu", "thongbao");
		if (sv == null) return "sinhvien/thongbao";

		Page<ThongBao> trang = thongBaoService.dsThongBao(sv.getTaiKhoan(), page);
		model.addAttribute("trang", trang);
		model.addAttribute("maDaDoc", thongBaoService.maDaDoc(sv.getTaiKhoan()));
		return "sinhvien/thongbao";
	}

	@GetMapping("/thongbao/{id}")
	public String chiTietThongBao(@ModelAttribute("sinhVien") SinhVien sv,
	                              @PathVariable Integer id,
	                              Model model, RedirectAttributes ra) {
		model.addAttribute("menu", "thongbao");
		try {
			model.addAttribute("tb", thongBaoService.moThongBao(sv.getTaiKhoan(), id));
		} catch (IllegalStateException e) {
			ra.addFlashAttribute("loi", e.getMessage());
			return "redirect:/sinhvien/thongbao";
		}
		// Vừa đánh dấu đã đọc → cập nhật lại badge
		model.addAttribute("soThongBaoChuaDoc", thongBaoService.soChuaDoc(sv.getTaiKhoan()));
		return "sinhvien/thongbao-chitiet";
	}

	// ===================== HỌC PHÍ =====================
	@GetMapping("/hocphi")
	public String hocPhi(@ModelAttribute("sinhVien") SinhVien sv, Model model) {
		model.addAttribute("menu", "hocphi");
		if (sv == null) return "sinhvien/hocphi";

		List<CongNoHocPhi> dsCongNo = hocPhiService.dsCongNo(sv).stream()
				.filter(cn -> cn.getTrangThai() != 3)
				.toList();
		model.addAttribute("dsCongNo", dsCongNo);

		model.addAttribute("tongPhaiDong", tong(dsCongNo, CongNoHocPhi::getSoTienPhaiDong));
		model.addAttribute("tongMienGiam", tong(dsCongNo, CongNoHocPhi::getSoTienMienGiam));
		model.addAttribute("tongDaDong",   tong(dsCongNo, CongNoHocPhi::getSoTienDaDong));
		model.addAttribute("tongConNo",    tong(dsCongNo, CongNoHocPhi::getSoTienConLai));

		dangKyHocPhanService.hocKyHienTai().ifPresent(hk -> {
			model.addAttribute("hocKyHienTai", hk);
			model.addAttribute("chiTiet", dangKyHocPhanService.dsDaDangKy(sv, hk));
		});
		model.addAttribute("donGia", HocPhiService.DON_GIA_TIN_CHI);

		return "sinhvien/hocphi";
	}

	private BigDecimal tong(List<CongNoHocPhi> ds, Function<CongNoHocPhi, BigDecimal> lay) {
		return ds.stream().map(lay).reduce(BigDecimal.ZERO, BigDecimal::add);
	}
}
