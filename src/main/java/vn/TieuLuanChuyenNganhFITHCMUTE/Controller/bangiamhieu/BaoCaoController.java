package vn.TieuLuanChuyenNganhFITHCMUTE.Controller.bangiamhieu;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.BaoCaoService;

/**
 * Khu báo cáo của Ban Giám hiệu. Chỉ đọc, không có thao tác ghi nào.
 *
 * Bốn trang cùng dùng một lần gom số liệu. Tách trang để mỗi màn hình trả lời
 * đúng một câu hỏi, thay vì một trang dài phải cuộn mãi mới tới thứ cần xem.
 *
 * Tên biến bắt buộc là "bc": các template gọi ${bc.tongSinhVien},
 * ${bc.khoaNhan}... Đặt tên khác là template báo lỗi null.
 */
@Controller
@RequestMapping("/baocao")
@RequiredArgsConstructor
public class BaoCaoController {

	private final BaoCaoService baoCaoService;

	/** Nhận cả /baocao và /baocao/tongquan cho khỏi phải nhớ đường dẫn nào đúng */
	@GetMapping({ "", "/tongquan" })
	public String tongQuan(Model model) {
		model.addAttribute("menu", "tongquan");
		model.addAttribute("bc", baoCaoService.tongQuan());
		return "bangiamhieu/tongquan";
	}

	@GetMapping("/sinhvien")
	public String thongKeSinhVien(Model model) {
		model.addAttribute("menu", "sinhvien");
		model.addAttribute("bc", baoCaoService.tongQuan());
		return "bangiamhieu/sinhvien";
	}

	@GetMapping("/hoctap")
	public String ketQuaHocTap(Model model) {
		model.addAttribute("menu", "hoctap");
		model.addAttribute("bc", baoCaoService.tongQuan());
		return "bangiamhieu/hoctap";
	}

	@GetMapping("/taichinh")
	public String taiChinh(Model model) {
		model.addAttribute("menu", "taichinh");
		model.addAttribute("bc", baoCaoService.tongQuan());
		return "bangiamhieu/taichinh";
	}
}
