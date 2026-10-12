package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;

@Repository
public interface GiangVienRepository extends JpaRepository<GiangVien, Integer> {

	/** Giảng viên đang đăng nhập, nạp sẵn khoa và tài khoản */
	@EntityGraph(attributePaths = {"donVi", "taiKhoan"})
	Optional<GiangVien> findByTaiKhoan_TenDangNhap(String tenDangNhap);

	boolean existsByMaSoGiangVien(String maSoGiangVien);

	/** Giảng viên của một khoa, dùng khi phân công giảng dạy */
	List<GiangVien> findByDonVi_MaDonViAndTrangThaiOrderByMaSoGiangVien(Integer maDonVi, Integer trangThai);
}