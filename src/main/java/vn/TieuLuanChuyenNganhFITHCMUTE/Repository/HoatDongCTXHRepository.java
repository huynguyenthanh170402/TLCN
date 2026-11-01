package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HoatDongCTXH;

@Repository
public interface HoatDongCTXHRepository extends JpaRepository<HoatDongCTXH, Integer> {

	/** Một hoạt động kèm học kỳ, cho trang chi tiết */
	@EntityGraph(attributePaths = {"hocKy"})
	Optional<HoatDongCTXH> findWithHocKyByMaHoatDong(Integer maHoatDong);

	/** Hoạt động của một học kỳ, nạp sẵn học kỳ */
	@EntityGraph(attributePaths = {"hocKy"})
	List<HoatDongCTXH> findByHocKy_MaHocKy(Integer maHocKy, Sort sort);

	/** Hoạt động của một học kỳ theo trạng thái */
	@EntityGraph(attributePaths = {"hocKy"})
	List<HoatDongCTXH> findByHocKy_MaHocKyAndTrangThai(Integer maHocKy, Integer trangThai, Sort sort);

	/**
	 * Số sinh viên đang giữ chỗ của một hoạt động.
	 *
	 * Đếm cả người đã đăng ký lẫn người đã được xác nhận tham gia, nhưng bỏ
	 * người đã hủy và người vắng mặt. Nếu đếm cả hai nhóm sau thì chỗ trống
	 * bị chiếm bởi người không còn tham gia, hoạt động báo đầy trong khi
	 * thực tế vẫn còn chỗ.
	 */
	@Query("""
	       SELECT COUNT(dk) FROM DangKyHoatDong dk
	       WHERE dk.hoatDong.maHoatDong = :maHoatDong AND dk.trangThai IN (1, 2)
	       """)
	long demDangGiuCho(@Param("maHoatDong") Integer maHoatDong);
}
