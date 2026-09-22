package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.NguoiNhanThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NguoiNhanThongBaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ThongBaoRepository;

/** Phía người nhận: xem danh sách, xem chi tiết, đánh dấu đã đọc */
@Service
@RequiredArgsConstructor
public class ThongBaoService {

	public static final int SO_DONG_MOI_TRANG = 10;

	private final ThongBaoRepository thongBaoRepository;
	private final NguoiNhanThongBaoRepository nguoiNhanRepository;

	public Page<ThongBao> dsThongBao(TaiKhoan tk, int trang) {
		return thongBaoRepository.timThongBaoHienThi(
				tk.getQuyen().getMaQuyen(), tk.getMaTaiKhoan(), LocalDateTime.now(),
				PageRequest.of(Math.max(trang, 0), SO_DONG_MOI_TRANG));
	}

	public Set<Integer> maDaDoc(TaiKhoan tk) {
		return new HashSet<>(nguoiNhanRepository.timMaDaDoc(tk.getMaTaiKhoan()));
	}

	public long soChuaDoc(TaiKhoan tk) {
		List<Integer> hienThi = thongBaoRepository.timMaThongBaoHienThi(
				tk.getQuyen().getMaQuyen(), tk.getMaTaiKhoan(), LocalDateTime.now());
		Set<Integer> daDoc = maDaDoc(tk);
		return hienThi.stream().filter(id -> !daDoc.contains(id)).count();
	}

	/**
	 * Mở một thông báo: kiểm tra tài khoản có được xem không,
	 * rồi ghi nhận đã đọc.
	 */
	@Transactional
	public ThongBao moThongBao(TaiKhoan tk, Integer maThongBao) {
		List<Integer> duocXem = thongBaoRepository.timMaThongBaoHienThi(
				tk.getQuyen().getMaQuyen(), tk.getMaTaiKhoan(), LocalDateTime.now());
		if (!duocXem.contains(maThongBao))
			throw new IllegalStateException("Bạn không có quyền xem thông báo này.");

		ThongBao tb = thongBaoRepository.findById(maThongBao)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy thông báo."));

		NguoiNhanThongBao nn = nguoiNhanRepository
				.findByThongBao_MaThongBaoAndTaiKhoan_MaTaiKhoan(maThongBao, tk.getMaTaiKhoan())
				.orElseGet(() -> {
					NguoiNhanThongBao moi = new NguoiNhanThongBao();
					moi.setThongBao(tb);
					moi.setTaiKhoan(tk);
					moi.setNgayGui(tb.getNgayDang());
					return moi;
				});

		if (nn.getTrangThaiDoc() == 0) {
			nn.setTrangThaiDoc(1);
			nn.setNgayDoc(LocalDateTime.now());
			nguoiNhanRepository.save(nn);
		}

		// Nạp sẵn đơn vị để template hiển thị
		if (tb.getDonVi() != null) tb.getDonVi().getTenDonVi();
		return tb;
	}
}
