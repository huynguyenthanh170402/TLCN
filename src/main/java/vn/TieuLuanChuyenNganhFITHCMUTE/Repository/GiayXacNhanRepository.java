package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiayXacNhanSinhVien;

@Repository
public interface GiayXacNhanRepository extends JpaRepository<GiayXacNhanSinhVien, Integer> {

	/** Một giấy kèm sinh viên, ngành, khoa — cho trang in và trang chi tiết */
	@Query("""
	       SELECT g FROM GiayXacNhanSinhVien g
	       JOIN FETCH g.sinhVien sv
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       WHERE g.maGiay = :maGiay
	       """)
	Optional<GiayXacNhanSinhVien> timChiTiet(@Param("maGiay") Integer maGiay);

	/** Đếm số giấy đã cấp trong ngày, dùng để đánh số thứ tự */
	long countByNgayCap(LocalDate ngayCap);

	/**
	 * Sổ cấp giấy, lọc theo từ khóa và trạng thái.
	 *
	 * Mỗi bộ lọc đi kèm một cờ bật tắt thay vì so sánh tham số với NULL.
	 * JPQL dạng ":x IS NULL OR cot = :x" khiến trình điều khiển SQL Server
	 * không đoán được kiểu tham số và báo "data types are incompatible".
	 */
	@Query(value = """
	        SELECT g FROM GiayXacNhanSinhVien g
	        JOIN FETCH g.sinhVien sv
	        LEFT JOIN FETCH sv.nganh n
	        LEFT JOIN FETCH n.donVi
	        WHERE (:locTrangThai = 0 OR g.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR g.soGiay LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        ORDER BY g.maGiay DESC
	        """,
	       countQuery = """
	        SELECT COUNT(g) FROM GiayXacNhanSinhVien g JOIN g.sinhVien sv
	        WHERE (:locTrangThai = 0 OR g.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR g.soGiay LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        """)
	Page<GiayXacNhanSinhVien> traCuu(@Param("locTrangThai") int locTrangThai,
	                                 @Param("trangThai") Integer trangThai,
	                                 @Param("locTuKhoa") int locTuKhoa,
	                                 @Param("tuKhoa") String tuKhoa,
	                                 Pageable pageable);
}
