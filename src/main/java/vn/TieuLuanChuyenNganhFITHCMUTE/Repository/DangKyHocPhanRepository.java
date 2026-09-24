package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;

@Repository
public interface DangKyHocPhanRepository extends JpaRepository<DangKyHocPhan, Integer> {

	/** Các lớp sinh viên đã đăng ký trong một học kỳ */
	@EntityGraph(attributePaths = {"lopHocPhan.hocPhan", "lopHocPhan.hocKy"})
	List<DangKyHocPhan> findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThai(
			Integer maSinhVien, Integer maHocKy, Integer trangThai);

	/** Sĩ số hiện tại của một lớp */
	long countByLopHocPhan_MaLopHocPhanAndTrangThai(Integer maLopHocPhan, Integer trangThai);

	/**
	 * Lượt đăng ký (kể cả đã hủy) của sinh viên vào một lớp.
	 * Ràng buộc UNIQUE (MaSinhVien, MaLopHocPhan): đăng ký lại sau khi hủy
	 * phải cập nhật dòng cũ, không insert dòng mới.
	 */
	Optional<DangKyHocPhan> findBySinhVien_MaSinhVienAndLopHocPhan_MaLopHocPhan(
			Integer maSinhVien, Integer maLopHocPhan);

	/** Danh sách sinh viên của một lớp (dùng cho bảng điểm giảng viên) */
	@EntityGraph(attributePaths = {"sinhVien"})
	List<DangKyHocPhan> findByLopHocPhan_MaLopHocPhanAndTrangThai(
			Integer maLopHocPhan, Integer trangThai, Sort sort);
	/** Mọi lượt đăng ký còn hiệu lực của sinh viên, dùng cho trang chương trình đào tạo */
	@EntityGraph(attributePaths = {"lopHocPhan.hocPhan"})
	List<DangKyHocPhan> findBySinhVien_MaSinhVienAndTrangThai(Integer maSinhVien, Integer trangThai);
}
