package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.MienGiamHocPhi;

@Repository
public interface MienGiamHocPhiRepository extends JpaRepository<MienGiamHocPhi, Integer> {

	/**
	 * Tổng tỉ lệ đã duyệt của một sinh viên trong một học kỳ.
	 *
	 * Cộng dồn vì một sinh viên có thể vừa thuộc diện chính sách vừa được
	 * học bổng. Phần chặn trần 100% nằm ở lớp Service, không nằm ở đây.
	 */
	@Query("""
	       SELECT COALESCE(SUM(mg.tyLePhanTram), 0) FROM MienGiamHocPhi mg
	       WHERE mg.sinhVien.maSinhVien = :maSinhVien
	         AND mg.hocKy.maHocKy = :maHocKy
	         AND mg.trangThai = 1
	       """)
	BigDecimal tongTyLeDaDuyet(@Param("maSinhVien") Integer maSinhVien,
	                           @Param("maHocKy") Integer maHocKy);

	/** Các quyết định đã duyệt của một sinh viên trong một học kỳ, để Tài chính xem căn cứ */
	@Query("""
	       SELECT mg FROM MienGiamHocPhi mg
	       WHERE mg.sinhVien.maSinhVien = :maSinhVien
	         AND mg.hocKy.maHocKy = :maHocKy
	         AND mg.trangThai = 1
	       ORDER BY mg.ngayDuyet
	       """)
	List<MienGiamHocPhi> dsDaDuyet(@Param("maSinhVien") Integer maSinhVien,
	                               @Param("maHocKy") Integer maHocKy);

	/**
	 * Tra cứu có lọc. Mỗi bộ lọc đi kèm một cờ bật/tắt thay vì so sánh
	 * tham số với NULL, để trình điều khiển SQL Server luôn đoán được
	 * kiểu dữ liệu của tham số.
	 */
	@Query(value = """
	        SELECT mg FROM MienGiamHocPhi mg
	        JOIN FETCH mg.sinhVien sv
	        LEFT JOIN FETCH sv.nganh n
	        LEFT JOIN FETCH n.donVi
	        JOIN FETCH mg.hocKy hk
	        WHERE (:locHocKy = 0 OR hk.maHocKy = :maHocKy)
	          AND (:locTrangThai = 0 OR mg.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        ORDER BY mg.trangThai ASC, mg.ngayDeNghi DESC
	        """,
	       countQuery = """
	        SELECT COUNT(mg) FROM MienGiamHocPhi mg JOIN mg.sinhVien sv JOIN mg.hocKy hk
	        WHERE (:locHocKy = 0 OR hk.maHocKy = :maHocKy)
	          AND (:locTrangThai = 0 OR mg.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        """)
	Page<MienGiamHocPhi> traCuu(@Param("locHocKy") int locHocKy,
	                            @Param("maHocKy") Integer maHocKy,
	                            @Param("locTrangThai") int locTrangThai,
	                            @Param("trangThai") Integer trangThai,
	                            @Param("locTuKhoa") int locTuKhoa,
	                            @Param("tuKhoa") String tuKhoa,
	                            Pageable pageable);

	/** Đếm hồ sơ đang chờ duyệt, để hiện con số trên trang Tổng quan */
	long countByTrangThai(Integer trangThai);
}
