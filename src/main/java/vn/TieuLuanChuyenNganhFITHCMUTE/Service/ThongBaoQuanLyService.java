package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Quyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBaoQuyen;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.DonViRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NguoiNhanThongBaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.QuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ThongBaoQuyenRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ThongBaoRepository;

/** Phía người gửi: soạn, đăng, gỡ thông báo và thống kê lượt đọc */
@Service
@RequiredArgsConstructor
public class ThongBaoQuanLyService {

	public static final int SO_DONG_MOI_TRANG = 10;

	private static final int LOAI_DON_VI_PHONG_BAN = 2;

	private final ThongBaoRepository thongBaoRepository;
	private final ThongBaoQuyenRepository thongBaoQuyenRepository;
	private final NguoiNhanThongBaoRepository nguoiNhanRepository;
	private final TaiKhoanRepository taiKhoanRepository;
	private final QuyenRepository quyenRepository;
	private final DonViRepository donViRepository;

	// ===================== DỮ LIỆU CHO FORM =====================

	public TaiKhoan taiKhoan(String tenDangNhap) {
		return taiKhoanRepository.findByTenDangNhap(tenDangNhap)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy tài khoản."));
	}

	public List<Quyen> dsQuyen() {
		return quyenRepository.findAll(Sort.by("maQuyen"));
	}

	/** Các phòng ban có thể đứng tên phát hành thông báo */
	public List<DonVi> dsPhongBan() {
		return donViRepository.findByLoaiDonVi_MaLoaiDonViOrderByTenDonVi(LOAI_DON_VI_PHONG_BAN);
	}

	// ===================== DANH SÁCH ĐÃ GỬI =====================

	public Page<ThongBao> dsDaGui(TaiKhoan tk, int trang) {
		return thongBaoRepository.findByNguoiDang_MaTaiKhoan(tk.getMaTaiKhoan(),
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG,
						Sort.by(Sort.Direction.DESC, "ngayTao")));
	}

	/** Mã thông báo → mô tả người nhận ("Toàn trường", "Sinh viên, Phòng ban", ...) */
	public Map<Integer, String> moTaNguoiNhan(List<ThongBao> ds) {
		Map<Integer, String> kq = new HashMap<>();
		List<Integer> theoQuyen = ds.stream()
				.filter(tb -> tb.getCheDoNguoiNhan() == 2)
				.map(ThongBao::getMaThongBao)
				.toList();

		Map<Integer, String> tenQuyen = theoQuyen.isEmpty() ? Map.of()
				: thongBaoQuyenRepository.findByThongBao_MaThongBaoIn(theoQuyen).stream()
						.collect(Collectors.groupingBy(
								tq -> tq.getThongBao().getMaThongBao(),
								Collectors.mapping(tq -> tq.getQuyen().getTenQuyen(), Collectors.joining(", "))));

		for (ThongBao tb : ds) {
			switch (tb.getCheDoNguoiNhan()) {
				case 1 -> kq.put(tb.getMaThongBao(), "Toàn trường");
				case 2 -> kq.put(tb.getMaThongBao(), tenQuyen.getOrDefault(tb.getMaThongBao(), "—"));
				case 3 -> kq.put(tb.getMaThongBao(), "Cá nhân");
				default -> kq.put(tb.getMaThongBao(), "");
			}
		}
		return kq;
	}

	/** Mã thông báo → số người đã đọc */
	public Map<Integer, Long> soNguoiDaDoc(List<ThongBao> ds) {
		Map<Integer, Long> kq = new HashMap<>();
		for (ThongBao tb : ds) {
			kq.put(tb.getMaThongBao(),
					nguoiNhanRepository.countByThongBao_MaThongBaoAndTrangThaiDoc(tb.getMaThongBao(), 1));
		}
		return kq;
	}

	// ===================== SOẠN / ĐĂNG =====================

	/**
	 * @param dangNgay true = đăng ngay, false = lưu nháp
	 */
	@Transactional
	public ThongBao tao(TaiKhoan tk, String tieuDe, String noiDung, int loai, int cheDo,
	                    List<Integer> dsMaQuyen, Integer maDonVi, LocalDate ngayHetHan, boolean dangNgay) {

		if (tieuDe == null || tieuDe.isBlank())
			throw new IllegalStateException("Tiêu đề không được để trống.");
		if (tieuDe.length() > 300)
			throw new IllegalStateException("Tiêu đề tối đa 300 ký tự.");
		if (noiDung == null || noiDung.isBlank())
			throw new IllegalStateException("Nội dung không được để trống.");
		if (loai < 1 || loai > 5)
			throw new IllegalStateException("Loại thông báo không hợp lệ.");
		if (cheDo != 1 && cheDo != 2)
			throw new IllegalStateException("Chế độ người nhận không hợp lệ.");
		if (cheDo == 2 && (dsMaQuyen == null || dsMaQuyen.isEmpty()))
			throw new IllegalStateException("Chọn ít nhất một nhóm người nhận.");
		if (ngayHetHan != null && ngayHetHan.isBefore(LocalDate.now()))
			throw new IllegalStateException("Ngày hết hạn phải từ hôm nay trở đi.");

		ThongBao tb = new ThongBao();
		tb.setNguoiDang(tk);
		tb.setTieuDe(tieuDe.trim());
		tb.setNoiDung(noiDung.trim());
		tb.setLoaiThongBao(loai);
		tb.setCheDoNguoiNhan(cheDo);
		if (maDonVi != null) {
			tb.setDonVi(donViRepository.findById(maDonVi)
					.orElseThrow(() -> new IllegalStateException("Không tìm thấy đơn vị.")));
		}
		// Hết hạn vào cuối ngày được chọn
		tb.setNgayHetHan(ngayHetHan != null ? ngayHetHan.atTime(23, 59, 59) : null);
		tb.setTrangThai(dangNgay ? 1 : 0);
		tb.setNgayDang(dangNgay ? LocalDateTime.now() : null);
		tb = thongBaoRepository.save(tb);

		if (cheDo == 2) {
			for (Integer maQuyen : dsMaQuyen.stream().distinct().toList()) {
				ThongBaoQuyen tq = new ThongBaoQuyen();
				tq.setThongBao(tb);
				tq.setQuyen(quyenRepository.findById(maQuyen)
						.orElseThrow(() -> new IllegalStateException("Nhóm quyền không hợp lệ.")));
				thongBaoQuyenRepository.save(tq);
			}
		}
		return tb;
	}

	// ===================== ĐĂNG BẢN NHÁP / GỠ =====================

	private ThongBao cuaToi(TaiKhoan tk, Integer maThongBao) {
		ThongBao tb = thongBaoRepository.findById(maThongBao)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy thông báo."));
		// Chỉ người đăng mới được thao tác trên thông báo của mình
		if (!tb.getNguoiDang().getMaTaiKhoan().equals(tk.getMaTaiKhoan()))
			throw new IllegalStateException("Bạn không có quyền thao tác trên thông báo này.");
		return tb;
	}

	@Transactional
	public void dang(TaiKhoan tk, Integer maThongBao) {
		ThongBao tb = cuaToi(tk, maThongBao);
		if (tb.getTrangThai() == 1)
			throw new IllegalStateException("Thông báo đã được đăng.");
		tb.setTrangThai(1);
		tb.setNgayDang(LocalDateTime.now());
		thongBaoRepository.save(tb);
	}

	@Transactional
	public void go(TaiKhoan tk, Integer maThongBao) {
		ThongBao tb = cuaToi(tk, maThongBao);
		if (tb.getTrangThai() != 1)
			throw new IllegalStateException("Chỉ gỡ được thông báo đang hiển thị.");
		tb.setTrangThai(2);
		thongBaoRepository.save(tb);
	}
}
