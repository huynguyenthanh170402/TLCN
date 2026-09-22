package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;

@Repository
public interface SinhVienRepository extends JpaRepository<SinhVien, Integer> {

	/**
	 * Lấy hồ sơ theo tên đăng nhập.
	 * JOIN FETCH để nạp sẵn ngành và khoa, tránh lỗi LazyInitialization lúc render.
	 */
	@Query("""
	       SELECT sv FROM SinhVien sv
	       JOIN FETCH sv.taiKhoan tk
	       LEFT JOIN FETCH sv.nganh n
	       LEFT JOIN FETCH n.donVi d
	       WHERE tk.tenDangNhap = :tenDangNhap
	       """)
	Optional<SinhVien> timTheoTenDangNhap(@Param("tenDangNhap") String tenDangNhap);
}