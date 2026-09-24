package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;

@Repository
public interface TaiKhoanRepository extends JpaRepository<TaiKhoan, Integer> {

	Optional<TaiKhoan> findByTenDangNhap(String tenDangNhap);

	/** Kiểm tra trùng tên đăng nhập khi tạo tài khoản từ file Excel */
	boolean existsByTenDangNhap(String tenDangNhap);
}
