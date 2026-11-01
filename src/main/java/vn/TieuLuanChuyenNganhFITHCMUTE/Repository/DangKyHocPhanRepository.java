package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DangKyHocPhan;

@Repository
public interface DangKyHocPhanRepository extends JpaRepository<DangKyHocPhan, Integer> {

	/** Các lớp sinh viên đã đăng ký trong một học kỳ */
	@EntityGraph(attributePaths = {"lopHocPhan.hocPhan", "lopHocPhan.hocKy"})
	List<DangKyHocPhan> findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThai(
			Integer maSinhVien, Integer maHocKy, Integer trangThai);

	/** Sĩ số hiện tại của một lớp, tính cả lượt đang chờ Phòng Đào tạo xác nhận */
	long countByLopHocPhan_MaLopHocPhanAndTrangThai(Integer maLopHocPhan, Integer trangThai);

	/**
	 * Số sinh viên đã được xác nhận của một lớp, tức sĩ số chắc chắn.
	 *
	 * trangThaiDuyet IS NULL cũng tính: đó là dữ liệu có trước khi thêm bước
	 * xác nhận, bỏ ra thì lớp cũ nào cũng hiện sĩ số 0 và trần 50 thành vô nghĩa.
	 */
	@Query("""
			SELECT COUNT(dk) FROM DangKyHocPhan dk
			WHERE dk.lopHocPhan.maLopHocPhan = :maLop
			  AND dk.trangThai = 1
			  AND (dk.trangThaiDuyet IS NULL OR dk.trangThaiDuyet = 1)
			""")
	long demDaXacNhan(@Param("maLop") Integer maLop);

	/**
	 * Lượt đăng ký của sinh viên trong một học kỳ theo trạng thái xác nhận.
	 *
	 * Không lọc thêm theo trangThai: lượt bị từ chối đã chuyển sang hủy, lọc
	 * trangThai = 1 nữa thì không còn dòng nào.
	 */
	@EntityGraph(attributePaths = {"lopHocPhan.hocPhan"})
	List<DangKyHocPhan> findBySinhVien_MaSinhVienAndLopHocPhan_HocKy_MaHocKyAndTrangThaiDuyet(
			Integer maSinhVien, Integer maHocKy, Integer trangThaiDuyet);

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

	/**
	 * Toàn bộ lượt đăng ký của một học kỳ, cho màn hình Phòng Đào tạo xác nhận.
	 *
	 * Lấy cả lượt đã hủy chứ không lọc sẵn theo trạng thái: màn hình có ba tab
	 * (chờ xác nhận, đã xác nhận, bị từ chối) mà lượt bị từ chối lại mang
	 * trangThai = 2 giống hệt lượt sinh viên tự hủy. Phân biệt hai thứ đó phải
	 * nhìn thêm trangThaiDuyet, nên lọc ở Java gọn hơn là ba truy vấn với điều
	 * kiện chồng chéo. Một học kỳ chỉ vài trăm dòng.
	 *
	 * JOIN FETCH để bảng hiện tên sinh viên, ngành, khoa và học phần mà không
	 * bắn thêm một truy vấn cho mỗi dòng.
	 */
	@Query("""
			SELECT dk FROM DangKyHocPhan dk
			JOIN FETCH dk.sinhVien sv
			LEFT JOIN FETCH sv.nganh ng
			LEFT JOIN FETCH ng.donVi
			JOIN FETCH dk.lopHocPhan lop
			JOIN FETCH lop.hocPhan hp
			WHERE lop.hocKy.maHocKy = :maHocKy
			ORDER BY dk.ngayDangKy
			""")
	List<DangKyHocPhan> dsCuaHocKy(@Param("maHocKy") Integer maHocKy);
}
