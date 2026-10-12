package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.KetQuaHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Service.NhapDiemService.DongDiem;

/** Phòng Đào tạo duyệt, trả lại và công bố bảng điểm */
@Service
@RequiredArgsConstructor
public class DuyetDiemService {

	private static final int VAI_TRO_CHINH = 1;
	private static final int HIEU_LUC = 1;

	private final KetQuaHocPhanRepository ketQuaRepository;
	private final LopHocPhanRepository lopRepository;
	private final PhanCongGiangDayRepository phanCongRepository;
	private final NhapDiemService nhapDiemService;

	/** Các lớp có bảng điểm ở trạng thái cho trước (1 chờ duyệt, 2 đã duyệt, 3 đã công bố) */
	public List<LopHocPhan> dsLop(int trangThaiDiem) {
		return ketQuaRepository.timLopTheoTrangThaiDiem(trangThaiDiem).stream()
				.sorted(Comparator.comparing(LopHocPhan::getMaLop))
				.toList();
	}

	public LopHocPhan lop(Integer maLop) {
		return lopRepository.findById(maLop)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy lớp học phần."));
	}

	/** Tên giảng viên chính của lớp, để hiện ở trang duyệt */
	public String giangVienChinh(Integer maLop) {
		return phanCongRepository
				.findByLopHocPhan_MaLopHocPhanAndVaiTroAndTrangThai(maLop, VAI_TRO_CHINH, HIEU_LUC)
				.stream()
				.map(pc -> pc.getGiangVien().getTenHienThi())
				.findFirst()
				.orElse("Chưa phân công");
	}

	// ===================== THAO TÁC =====================

	/** Chờ duyệt (1) → Đã duyệt (2) */
	@Transactional
	public void duyet(Integer maLop) {
		List<KetQuaHocPhan> ds = layKetQuaDongLoat(maLop, NhapDiemService.KQ_CHO_DUYET,
				"Bảng điểm không ở trạng thái chờ duyệt.");
		ds.forEach(kq -> kq.setTrangThai(NhapDiemService.KQ_DA_DUYET));
		ketQuaRepository.saveAll(ds);
	}

	/** Chờ duyệt (1) → Đang nhập (0), kèm lý do để giảng viên biết cần sửa gì */
	@Transactional
	public void traLai(Integer maLop, String lyDo) {
		if (lyDo == null || lyDo.isBlank())
			throw new IllegalStateException("Nhập lý do trả lại để giảng viên biết cần sửa gì.");

		List<KetQuaHocPhan> ds = layKetQuaDongLoat(maLop, NhapDiemService.KQ_CHO_DUYET,
				"Chỉ trả lại được bảng điểm đang chờ duyệt.");
		ds.forEach(kq -> {
			kq.setTrangThai(NhapDiemService.KQ_DANG_NHAP);
			kq.setGhiChu("Phòng Đào tạo trả lại: " + lyDo.trim());
		});
		ketQuaRepository.saveAll(ds);
	}

	/** Đã duyệt (2) → Đã công bố (3): sinh viên bắt đầu thấy điểm */
	@Transactional
	public void congBo(Integer maLop) {
		List<KetQuaHocPhan> ds = layKetQuaDongLoat(maLop, NhapDiemService.KQ_DA_DUYET,
				"Chỉ công bố được bảng điểm đã duyệt.");
		ds.forEach(kq -> kq.setTrangThai(NhapDiemService.KQ_CONG_BO));
		ketQuaRepository.saveAll(ds);
	}

	/**
	 * Lấy kết quả của cả lớp và kiểm tra tất cả đang ở cùng một trạng thái.
	 * Bảng điểm luôn được xử lý nguyên lớp, không duyệt lẻ từng sinh viên.
	 */
	private List<KetQuaHocPhan> layKetQuaDongLoat(Integer maLop, int trangThaiCanCo, String loiNeuSai) {
		List<DongDiem> bang = nhapDiemService.bangDiem(maLop);
		if (bang.isEmpty())
			throw new IllegalStateException("Lớp chưa có sinh viên.");

		boolean dongLoat = bang.stream()
				.allMatch(d -> d.getKetQua() != null && d.getKetQua().getTrangThai() == trangThaiCanCo);
		if (!dongLoat)
			throw new IllegalStateException(loiNeuSai);

		return bang.stream().map(DongDiem::getKetQua).toList();
	}
}
