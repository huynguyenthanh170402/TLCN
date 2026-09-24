package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;

@Service
@RequiredArgsConstructor
public class LichHocService {

	/** Giờ bắt đầu của từng tiết (chỉ số 1..12) — số mẫu, sửa theo quy định của trường */
	public static final String[] GIO_BAT_DAU = {
		"", "06:45", "07:35", "08:25", "09:25", "10:15", "11:05",
		"12:35", "13:25", "14:15", "15:15", "16:05", "16:55"
	};

	private final LichHocRepository lichHocRepository;

	/** Dữ liệu để vẽ bảng thời khóa biểu theo tuần */
	@Getter
	@AllArgsConstructor
	public static class ThoiKhoaBieu {
		/** "thu-tiet" → buổi học bắt đầu tại ô đó (ví dụ "2-1") */
		private Map<String, LichHoc> oBatDau;
		/** Các ô bị buổi học phía trên chiếm (do rowspan), không vẽ td */
		private Set<String> oBiChiem;
	}

	/** Mã lớp → danh sách buổi học của lớp đó */
	public Map<Integer, List<LichHoc>> lichTheoLop(Collection<Integer> dsMaLop) {
		if (dsMaLop == null || dsMaLop.isEmpty()) return Map.of();
		return lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(dsMaLop).stream()
				.collect(Collectors.groupingBy(lh -> lh.getLopHocPhan().getMaLopHocPhan()));
	}

	/** Các buổi học của một lớp */
	public List<LichHoc> lichCuaLop(Integer maLopHocPhan) {
		return lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(List.of(maLopHocPhan));
	}

	/** Toàn bộ buổi học của các lớp đã đăng ký, xếp theo thứ rồi theo tiết */
	public List<LichHoc> lichCuaCacDangKy(List<DangKyHocPhan> dsDangKy) {
		if (dsDangKy == null || dsDangKy.isEmpty()) return List.of();
		List<Integer> dsMaLop = dsDangKy.stream()
				.map(dk -> dk.getLopHocPhan().getMaLopHocPhan())
				.toList();
		return lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(dsMaLop).stream()
				.sorted(Comparator.comparing(LichHoc::getThu).thenComparing(LichHoc::getTietBatDau))
				.toList();
	}

	/** Ném lỗi nếu lớp mới trùng giờ với bất kỳ lớp nào đã đăng ký */
	public void kiemTraTrungLich(LopHocPhan lopMoi, List<DangKyHocPhan> daDangKy) {
		List<LichHoc> lichMoi = lichHocRepository
				.findByLopHocPhan_MaLopHocPhanIn(List.of(lopMoi.getMaLopHocPhan()));
		List<LichHoc> lichCu = lichCuaCacDangKy(daDangKy);

		for (LichHoc a : lichMoi) {
			for (LichHoc b : lichCu) {
				if (a.trungGio(b)) {
					throw new IllegalStateException("Trùng lịch với lớp " + b.getLopHocPhan().getMaLop()
							+ " (" + b.getTenThu() + ", tiết " + b.getTietBatDau()
							+ "–" + b.getTietKetThuc() + ").");
				}
			}
		}
	}

	/** Chuyển danh sách buổi học thành dữ liệu cho bảng tuần */
	public ThoiKhoaBieu thoiKhoaBieu(List<LichHoc> dsLich) {
		Map<String, LichHoc> oBatDau = new HashMap<>();
		Set<String> oBiChiem = new HashSet<>();

		for (LichHoc lh : dsLich) {
			oBatDau.put(lh.getThu() + "-" + lh.getTietBatDau(), lh);
			for (int t = lh.getTietBatDau() + 1; t <= lh.getTietKetThuc(); t++) {
				oBiChiem.add(lh.getThu() + "-" + t);
			}
		}
		return new ThoiKhoaBieu(oBatDau, oBiChiem);
	}
}