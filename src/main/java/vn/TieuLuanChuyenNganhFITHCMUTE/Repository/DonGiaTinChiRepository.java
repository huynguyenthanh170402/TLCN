package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonGiaTinChi;

@Repository
public interface DonGiaTinChiRepository extends JpaRepository<DonGiaTinChi, Integer> {

	/** Đơn giá riêng của một khóa trong một học kỳ */
	Optional<DonGiaTinChi> findByHocKy_MaHocKyAndKhoaTuyenSinh(Integer maHocKy, String khoaTuyenSinh);

	/** Đơn giá chung của học kỳ, dòng có KhoaTuyenSinh để trống */
	@Query("SELECT dg FROM DonGiaTinChi dg WHERE dg.hocKy.maHocKy = :maHocKy AND dg.khoaTuyenSinh IS NULL")
	Optional<DonGiaTinChi> timDonGiaChung(@Param("maHocKy") Integer maHocKy);

	/** Toàn bộ bảng giá, dòng chung của mỗi học kỳ đứng trước */
	@EntityGraph(attributePaths = { "hocKy" })
	List<DonGiaTinChi> findAllByOrderByHocKy_NgayBatDauDescKhoaTuyenSinhAsc();

	List<DonGiaTinChi> findByHocKy_MaHocKyOrderByKhoaTuyenSinhAsc(Integer maHocKy);
}
