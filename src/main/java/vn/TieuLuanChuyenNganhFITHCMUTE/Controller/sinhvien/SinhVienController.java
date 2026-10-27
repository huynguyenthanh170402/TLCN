package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.sinhvien;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KeHoachDangKyService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.KetQuaHocTapService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.LichHocService;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.PhieuDangKyService;
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
	private final PhieuDangKyService phieuDangKyService;
	private final KeHoachDangKyService keHoachDangKyService;

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
	                     @RequestParam(required = false) Integer hocKy,
	                     Model model) {
		model.addAttribute("menu", "ketqua");
		if (sv == null) return "sinhvien/ketqua";

		List<KetQuaHocPhan> tatCa = ketQuaHocTapService.tatCa(sv.getMaSinhVien());

		// Thống kê tích lũy: tính trên điểm được công nhận của mỗi học phần
		model.addAttribute("tongKet", ketQuaHocTapService.tinhTongKet(tatCa));
		model.addAttribute("dsHocKy", ketQuaHocTapService.dsHocKy(tatCa));
		model.addAttribute("hocKyChon", hocKy);

		// Bảng điểm nhóm theo học kỳ, lọc nếu người dùng chọn một học kỳ
		var nhom = ketQuaHocTapService.theoHocKy(tatCa);
		if (hocKy != null)
			nhom = nhom.stream()
					.filter(n -> hocKy.equals(n.getHocKy().getMaHocKy()))
					.toList();
		model.addAttribute("nhomHocKy", nhom);

		// Bảng tổng hợp theo học phần, thấy rõ môn nào học lại mấy lần
		model.addAttribute("tongHop", ketQuaHocTapService.tongHopTheoHocPhan(tatCa));
		model.addAttribute("soLanToiDa", ketQuaHocTapService.tongHopTheoHocPhan(tatCa).stream()
				.mapToInt(t -> t.getSoLan()).max().orElse(1));

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

		// Bối cảnh gom sẵn mọi thứ cần để xét: điểm đã có, môn đã đạt, tiên
		// quyết, tín chỉ đã đăng ký. Tính một lần rồi dùng lại cho cả trang.
		var bc = dangKyHocPhanService.boiCanh(sv, hk);
		List<DangKyHocPhan> daDangKy = bc.getDaDangKy();

		model.addAttribute("daDangKy", daDangKy);
		model.addAttribute("tongTinChi", bc.getTongTinChiDaDangKy());
		model.addAttribute("tinChiToiDa", bc.getGioiHanTinChi());
		model.addAttribute("hocKyThu", bc.getHocKyThu());
		model.addAttribute("loiChung", bc.getLoiChung());

		// Khung chương trình của ngành, mỗi học phần kèm trạng thái đăng ký
		model.addAttribute("keHoach", keHoachDangKyService.keHoach(sv, hk, bc));

		// Lịch của mọi lớp xuất hiện trên trang: lớp đã đăng ký và lớp đang mở
		List<Integer> maLop = new ArrayList<>();
		for (DangKyHocPhan dk : daDangKy)
			maLop.add(dk.getLopHocPhan().getMaLopHocPhan());
		for (LopHocPhan lop : dangKyHocPhanService.dsLopDangMo(hk))
			maLop.add(lop.getMaLopHocPhan());
		model.addAttribute("lichTheoLop", lichHocService.lichTheoLop(maLop));

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

	/**
	 * Gửi phiếu xác nhận những học phần đã đăng ký về hộp thư sinh viên.
	 *
	 * Tách riêng khỏi nút Đăng ký vì sinh viên chọn lớp nhiều lần rồi đổi
	 * tới đổi lui; gửi mỗi lần bấm một thư thì hộp thư đầy thư vụn mà chẳng
	 * thư nào là bản chốt. Sinh viên tự bấm khi thấy đã xong.
	 *
	 * Phiếu chỉ sao lại dữ liệu đang có, bấm mấy lần cũng không thay đổi gì
	 * trong cơ sở dữ liệu, nên không cần chặn bấm nhiều lần.
	 */
	@PostMapping("/dangky/xacnhan")
	public String guiPhieuXacNhan(@ModelAttribute("sinhVien") SinhVien sv,
	                              RedirectAttributes ra) {
		HocKy hk = dangKyHocPhanService.hocKyHienTai().orElse(null);
		PhieuDangKyService.KetQua kq = phieuDangKyService.guiPhieu(sv, hk);

		ra.addFlashAttribute(kq.thanhCong() ? "thongBao" : "loi", kq.thongBao());
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