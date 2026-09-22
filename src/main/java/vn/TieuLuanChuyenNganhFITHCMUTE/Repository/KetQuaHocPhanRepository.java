package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.KetQuaHocPhan;

@Repository
public interface KetQuaHocPhanRepository extends JpaRepository<KetQuaHocPhan, Integer> {

	/*
	 * Spring Data đọc tên phương thức để tự sinh truy vấn:
	 *   findBy DangKyHocPhan_SinhVien_MaSinhVien   → kq.dangKyHocPhan.sinhVien.maSinhVien = ?
	 *      And TrangThai                           → kq.trangThai = ?
	 *      And DangKyHocPhan_LopHocPhan_HocKy_MaHocKy → ...hocKy.maHocKy = ?
	 * Dấu _ ngăn cách các cấp thuộc tính lồng nhau.
	 *
	 * @EntityGraph nạp sẵn học phần và học kỳ trong cùng một câu truy vấn,
	 * tránh lỗi LazyInitialization và tránh chạy thêm nhiều câu SELECT lẻ.
	 */
	String[] NAP_SAN = {
		"dangKyHocPhan.lopHocPhan.hocPhan",
		"dangKyHocPhan.lopHocPhan.hocKy"
	};

	/** Tất cả học kỳ, có phân trang */
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
}