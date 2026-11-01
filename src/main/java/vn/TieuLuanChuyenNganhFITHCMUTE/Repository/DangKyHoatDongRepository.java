package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHoatDong;

@Repository
public interface DangKyHoatDongRepository extends JpaRepository<DangKyHoatDong, Integer> {

	/** Danh sách tham gia của một hoạt động, kèm sinh viên và ngành */
	@Query("""
	       SELECT dk FROM DangKyHoatDong dk
	       JOIN FETCH dk.sinhVien sv
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       WHERE dk.hoatDong.maHoatDong = :maHoatDong
	       ORDER BY sv.maSoSinhVien
	       """)
	List<DangKyHoatDong> dsTheoHoatDong(@Param("maHoatDong") Integer maHoatDong);

	/** Lượt đăng ký của một sinh viên trong một hoạt động, kể cả đã hủy */
	Optional<DangKyHoatDong> findByHoatDong_MaHoatDongAndSinhVien_MaSinhVien(
			Integer maHoatDong, Integer maSinhVien);

	/** Toàn bộ hoạt động một sinh viên đã tham gia trong một học kỳ */
	@Query("""
	       SELECT dk FROM DangKyHoatDong dk
	       JOIN FETCH dk.hoatDong hd
	       JOIN FETCH hd.hocKy
	       WHERE dk.sinhVien.maSinhVien = :maSinhVien
	         AND hd.hocKy.maHocKy = :maHocKy
	         AND dk.trangThai <> 4
	       ORDER BY hd.thoiGianBatDau
	       """)
	List<DangKyHoatDong> dsTheoSinhVienVaHocKy(@Param("maSinhVien") Integer maSinhVien,
	                                           @Param("maHocKy") Integer maHocKy);

	/** Mọi lượt tham gia của một sinh viên, mọi học kỳ */
	@Query("""
	       SELECT dk FROM DangKyHoatDong dk
	       JOIN FETCH dk.hoatDong hd
	       JOIN FETCH hd.hocKy
	       WHERE dk.sinhVien.maSinhVien = :maSinhVien AND dk.trangThai <> 4
	       ORDER BY hd.hocKy.maHocKy DESC, hd.thoiGianBatDau
	       """)
	List<DangKyHoatDong> dsTheoSinhVien(@Param("maSinhVien") Integer maSinhVien);
}
