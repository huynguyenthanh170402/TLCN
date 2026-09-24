package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;

@Repository
public interface SinhVienRepository extends JpaRepository<SinhVien, Integer> {

	/** Sinh viên đang đăng nhập, nạp sẵn tài khoản, ngành, khoa */
	@Query("""
	       SELECT sv FROM SinhVien sv
	       JOIN FETCH sv.taiKhoan tk
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       WHERE tk.tenDangNhap = :tenDangNhap
	       """)
	Optional<SinhVien> timTheoTenDangNhap(@Param("tenDangNhap") String tenDangNhap);

	/** Một sinh viên theo mã, nạp sẵn tài khoản, ngành, khoa (trang chi tiết) */
	@Query("""
	       SELECT sv FROM SinhVien sv
	       LEFT JOIN FETCH sv.taiKhoan
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi
	       WHERE sv.maSinhVien = :maSinhVien
	       """)
	Optional<SinhVien> timTheoMa(@Param("maSinhVien") Integer maSinhVien);

	/** Kiểm tra trùng MSSV khi nhập từ Excel */
	boolean existsByMaSoSinhVien(String maSoSinhVien);

	/** Danh sách một lớp: cùng ngành, cùng khóa tuyển sinh */
	@EntityGraph(attributePaths = {"nganh", "nganh.donVi"})
	Page<SinhVien> findByNganh_MaNganhAndKhoaTuyenSinhOrderByMaSoSinhVien(
			Integer maNganh, String khoaTuyenSinh, Pageable pageable);

	/** Tìm nhanh theo MSSV hoặc họ tên, trên toàn trường */
	@Query(value = """
	        SELECT sv FROM SinhVien sv LEFT JOIN FETCH sv.nganh n LEFT JOIN FETCH n.donVi
	        WHERE sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	           OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%')
	        ORDER BY sv.maSoSinhVien
	        """,
	       countQuery = """
	        SELECT COUNT(sv) FROM SinhVien sv
	        WHERE sv.maSoSinhVien LIKE CONCAT('%', :tuKhoa, '%')
	           OR sv.hoTen LIKE CONCAT('%', :tuKhoa, '%')
	        """)
	Page<SinhVien> timNhanh(@Param("tuKhoa") String tuKhoa, Pageable pageable);
}
