package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;

@Repository
public interface KetQuaHocPhanRepository extends JpaRepository<KetQuaHocPhan, Integer> {

	/** Tất cả học kỳ, có phân trang (trang kết quả của sinh viên) */
	@EntityGraph(attributePaths = {
		"dangKyHocPhan.lopHocPhan.hocPhan",
		"dangKyHocPhan.lopHocPhan.hocKy"
	})
	Page<KetQuaHocPhan> findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(
			Integer maSinhVien, Integer trangThai, Pageable pageable);

	/** Một học kỳ, có phân trang */
	@EntityGraph(attributePaths = {
		"dangKyHocPhan.lopHocPhan.hocPhan",
		"dangKyHocPhan.lopHocPhan.hocKy"
	})
	Page<KetQuaHocPhan> findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThaiAndDangKyHocPhan_LopHocPhan_HocKy_MaHocKy(
			Integer maSinhVien, Integer trangThai, Integer maHocKy, Pageable pageable);

	/** Toàn bộ, không phân trang — dùng tính GPA */
	@EntityGraph(attributePaths = {
		"dangKyHocPhan.lopHocPhan.hocPhan",
		"dangKyHocPhan.lopHocPhan.hocKy"
	})
	List<KetQuaHocPhan> findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(
			Integer maSinhVien, Integer trangThai, Sort sort);

	/** Kết quả của nhiều lượt đăng ký cùng lúc (bảng điểm của một lớp) */
	List<KetQuaHocPhan> findByDangKyHocPhan_MaDangKyHocPhanIn(Collection<Integer> dsMaDangKy);

	/** Các lớp có ít nhất một kết quả ở trạng thái cho trước (trang duyệt điểm) */
	@Query("""
	       SELECT DISTINCT lop FROM KetQuaHocPhan kq
	       JOIN kq.dangKyHocPhan dk
	       JOIN dk.lopHocPhan lop
	       WHERE kq.trangThai = :trangThai
	       """)
	List<LopHocPhan> timLopTheoTrangThaiDiem(@Param("trangThai") Integer trangThai);
}
