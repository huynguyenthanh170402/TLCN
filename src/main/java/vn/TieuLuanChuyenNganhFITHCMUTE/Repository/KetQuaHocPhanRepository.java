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

	/**
	 * Kết quả đã công bố của một sinh viên, có phân trang.
	 * maHocKy = null thì lấy tất cả học kỳ.
	 * countQuery tách riêng vì JOIN FETCH không dùng được trong câu đếm.
	 */
	@Query(value = """
	        SELECT kq FROM KetQuaHocPhan kq
	        JOIN FETCH kq.dangKyHocPhan dk
	        JOIN FETCH dk.lopHocPhan lop
	        JOIN FETCH lop.hocPhan hp
	        JOIN FETCH lop.hocKy hk
	        WHERE dk.sinhVien.maSinhVien = :maSv
	          AND kq.trangThai = 3
	          AND (:maHocKy IS NULL OR hk.maHocKy = :maHocKy)
	        ORDER BY hk.ngayBatDau DESC, hp.maHocPhan
	        """,
	       countQuery = """
	        SELECT COUNT(kq) FROM KetQuaHocPhan kq
	        JOIN kq.dangKyHocPhan dk
	        JOIN dk.lopHocPhan lop
	        WHERE dk.sinhVien.maSinhVien = :maSv
	          AND kq.trangThai = 3
	          AND (:maHocKy IS NULL OR lop.hocKy.maHocKy = :maHocKy)
	        """)
	Page<KetQuaHocPhan> timKetQuaCongBo(@Param("maSv") Integer maSv,
	                                    @Param("maHocKy") Integer maHocKy,
	                                    Pageable pageable);

	/** Toàn bộ kết quả đã công bố, dùng để tính GPA và gom theo học kỳ */
	@Query("""
	        SELECT kq FROM KetQuaHocPhan kq
	        JOIN FETCH kq.dangKyHocPhan dk
	        JOIN FETCH dk.lopHocPhan lop
	        JOIN FETCH lop.hocPhan hp
	        JOIN FETCH lop.hocKy hk
	        WHERE dk.sinhVien.maSinhVien = :maSv
	          AND kq.trangThai = 3
	        ORDER BY hk.ngayBatDau DESC
	        """)
	List<KetQuaHocPhan> timTatCaKetQuaCongBo(@Param("maSv") Integer maSv);

	/** Kết quả theo trạng thái, có phân trang (dùng ở các trang khác) */
	@EntityGraph(attributePaths = {
		"dangKyHocPhan.lopHocPhan.hocPhan",
		"dangKyHocPhan.lopHocPhan.hocKy"
	})
	Page<KetQuaHocPhan> findByDangKyHocPhan_SinhVien_MaSinhVienAndTrangThai(
			Integer maSinhVien, Integer trangThai, Pageable pageable);

	/** Kết quả theo trạng thái, không phân trang */
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