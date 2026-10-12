package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

/**
 * Đếm sinh viên theo từng cấp: Khoa → Ngành → Khóa.
 * Dùng GROUP BY ngay trong database, không tải danh sách sinh viên lên bộ nhớ.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ThongKeSinhVienService {

	private final EntityManager em;

	/** Một ô trong lưới: mã để bấm vào, tên hiển thị, số sinh viên */
	public record MucThongKe(String ma, String ten, long soLuong) {}

	/** Các khoa có sinh viên, kèm số lượng */
	public List<MucThongKe> theoKhoa() {
		return em.createQuery("""
				SELECT d.maDonVi, d.tenDonVi, COUNT(sv)
				FROM SinhVien sv JOIN sv.nganh n JOIN n.donVi d
				GROUP BY d.maDonVi, d.tenDonVi
				ORDER BY d.tenDonVi
				""", Object[].class)
				.getResultList().stream()
				.map(r -> new MucThongKe(String.valueOf(r[0]), (String) r[1], (Long) r[2]))
				.toList();
	}

	/** Các ngành trong một khoa */
	public List<MucThongKe> theoNganh(Integer maDonVi) {
		return em.createQuery("""
				SELECT n.maNganh, n.tenNganh, COUNT(sv)
				FROM SinhVien sv JOIN sv.nganh n
				WHERE n.donVi.maDonVi = :maDonVi
				GROUP BY n.maNganh, n.tenNganh
				ORDER BY n.tenNganh
				""", Object[].class)
				.setParameter("maDonVi", maDonVi)
				.getResultList().stream()
				.map(r -> new MucThongKe(String.valueOf(r[0]), (String) r[1], (Long) r[2]))
				.toList();
	}

	/** Các khóa tuyển sinh của một ngành, khóa mới nhất trước */
	public List<MucThongKe> theoKhoaTuyenSinh(Integer maNganh) {
		return em.createQuery("""
				SELECT sv.khoaTuyenSinh, COUNT(sv)
				FROM SinhVien sv
				WHERE sv.nganh.maNganh = :maNganh AND sv.khoaTuyenSinh IS NOT NULL
				GROUP BY sv.khoaTuyenSinh
				ORDER BY sv.khoaTuyenSinh DESC
				""", Object[].class)
				.setParameter("maNganh", maNganh)
				.getResultList().stream()
				.map(r -> new MucThongKe((String) r[0], "Khóa " + r[0], (Long) r[1]))
				.toList();
	}

	/** Tổng số sinh viên toàn trường */
	public long tongSinhVien() {
		return em.createQuery("SELECT COUNT(sv) FROM SinhVien sv", Long.class).getSingleResult();
	}
}
