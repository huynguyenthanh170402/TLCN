package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonXetTotNghiep;

@Repository
public interface DonXetTotNghiepRepository extends JpaRepository<DonXetTotNghiep, Integer> {

	/** Danh sách đơn của một đợt, kèm sinh viên và ngành */
	@Query("""
	       SELECT d FROM DonXetTotNghiep d
	       JOIN FETCH d.sinhVien sv
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       WHERE d.dot.maDot = :maDot
	       ORDER BY sv.maSoSinhVien
	       """)
	List<DonXetTotNghiep> dsTheoDot(@Param("maDot") Integer maDot);

	Optional<DonXetTotNghiep> findByDot_MaDotAndSinhVien_MaSinhVien(Integer maDot, Integer maSinhVien);

	/** Các đơn sinh viên đã nộp, mới nhất trước */
	@Query("""
	       SELECT d FROM DonXetTotNghiep d
	       JOIN FETCH d.dot dot
	       JOIN FETCH dot.hocKy
	       WHERE d.sinhVien.maSinhVien = :maSinhVien
	       ORDER BY d.ngayNop DESC
	       """)
	List<DonXetTotNghiep> dsTheoSinhVien(@Param("maSinhVien") Integer maSinhVien);

	long countByDot_MaDotAndTrangThai(Integer maDot, Integer trangThai);
}
