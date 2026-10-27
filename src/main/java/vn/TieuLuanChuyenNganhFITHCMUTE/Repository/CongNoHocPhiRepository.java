package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;

@Repository
public interface CongNoHocPhiRepository extends JpaRepository<CongNoHocPhi, Integer> {

	Optional<CongNoHocPhi> findBySinhVien_MaSinhVienAndHocKy_MaHocKy(Integer maSinhVien, Integer maHocKy);

	/** Toàn bộ công nợ của sinh viên, nạp sẵn học kỳ */
	@EntityGraph(attributePaths = {"hocKy"})
	List<CongNoHocPhi> findBySinhVien_MaSinhVien(Integer maSinhVien, Sort sort);

	// ===================== Dùng cho Phòng Kế hoạch Tài chính =====================

	/** Một dòng công nợ kèm sinh viên, ngành, khoa, học kỳ — cho màn hình thu tiền */
	@Query("""
	       SELECT cn FROM CongNoHocPhi cn
	       JOIN FETCH cn.sinhVien sv
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       JOIN FETCH cn.hocKy
	       WHERE cn.maCongNoHocPhi = :maCongNo
	       """)
	Optional<CongNoHocPhi> timChiTiet(@Param("maCongNo") Integer maCongNo);

	/**
	 * Tra cứu công nợ toàn trường, có lọc học kỳ, trạng thái và từ khóa.
	 *
	 * Mỗi bộ lọc đi kèm một cờ bật/tắt thay vì so sánh tham số với NULL.
	 * Lý do: JPQL dạng ":x IS NULL OR cot = :x" khiến trình điều khiển
	 * SQL Server không đoán được kiểu dữ liệu của tham số và báo lỗi
	 * "data types are incompatible". Dùng cờ số nguyên thì kiểu luôn rõ ràng.
	 */
	@Query(value = """
	        SELECT cn FROM CongNoHocPhi cn
	        JOIN FETCH cn.sinhVien sv
	        LEFT JOIN FETCH sv.nganh n
	        LEFT JOIN FETCH n.donVi
	        JOIN FETCH cn.hocKy hk
	        WHERE (:locHocKy = 0 OR hk.maHocKy = :maHocKy)
	          AND (:locTrangThai = 0 OR cn.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        ORDER BY sv.maSoSinhVien
	        """,
	       countQuery = """
	        SELECT COUNT(cn) FROM CongNoHocPhi cn JOIN cn.sinhVien sv JOIN cn.hocKy hk
	        WHERE (:locHocKy = 0 OR hk.maHocKy = :maHocKy)
	          AND (:locTrangThai = 0 OR cn.trangThai = :trangThai)
	          AND (:locTuKhoa = 0
	               OR sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	               OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%'))
	        """)
	Page<CongNoHocPhi> traCuu(@Param("locHocKy") int locHocKy,
	                          @Param("maHocKy") Integer maHocKy,
	                          @Param("locTrangThai") int locTrangThai,
	                          @Param("trangThai") Integer trangThai,
	                          @Param("locTuKhoa") int locTuKhoa,
	                          @Param("tuKhoa") String tuKhoa,
	                          Pageable pageable);

	/**
	 * Số liệu cho các ô thống kê, gom ngay trong database thay vì kéo hết
	 * bản ghi về rồi cộng trong Java.
	 *
	 * Trả về một dòng bốn cột: phải thu, đã thu, số dòng công nợ,
	 * số dòng còn nợ. Khai báo List để Spring Data khỏi bọc nhầm mảng.
	 */
	@Query("""
	       SELECT COALESCE(SUM(cn.soTienPhaiDong - cn.soTienMienGiam), 0),
	              COALESCE(SUM(cn.soTienDaDong), 0),
	              COUNT(cn),
	              COALESCE(SUM(CASE WHEN cn.trangThai IN (0, 1) THEN 1 ELSE 0 END), 0)
	       FROM CongNoHocPhi cn
	       WHERE cn.trangThai <> 3
	         AND (:locHocKy = 0 OR cn.hocKy.maHocKy = :maHocKy)
	       """)
	List<Object[]> thongKe(@Param("locHocKy") int locHocKy,
	                       @Param("maHocKy") Integer maHocKy);
}
