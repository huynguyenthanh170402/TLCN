package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhieuThu;

@Repository
public interface PhieuThuRepository extends JpaRepository<PhieuThu, Integer> {

	/** Các phiếu còn hiệu lực của một công nợ, mới nhất trước */
	List<PhieuThu> findByCongNo_MaCongNoHocPhiOrderByNgayThuDesc(Integer maCongNo);

	/** Sổ phiếu thu, nạp sẵn công nợ và sinh viên để bảng không bị N+1 */
	@EntityGraph(attributePaths = { "congNo", "congNo.sinhVien", "congNo.hocKy" })
	Page<PhieuThu> findAllByOrderByNgayThuDesc(Pageable pageable);

	/** Đếm phiếu đã lập trong ngày, dùng để đánh số thứ tự trên biên lai */
	@Query("SELECT COUNT(p) FROM PhieuThu p WHERE p.ngayThu >= :dauNgay AND p.ngayThu < :cuoiNgay")
	long demTrongNgay(@Param("dauNgay") LocalDateTime dauNgay,
	                  @Param("cuoiNgay") LocalDateTime cuoiNgay);
}
