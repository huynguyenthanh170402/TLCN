package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;

@Repository
public interface PhanCongGiangDayRepository extends JpaRepository<PhanCongGiangDay, Integer> {

	/** Các lớp một giảng viên được phân công, nạp sẵn học phần và học kỳ */
	@EntityGraph(attributePaths = {"lopHocPhan.hocPhan", "lopHocPhan.hocKy"})
	List<PhanCongGiangDay> findByGiangVien_MaGiangVienAndTrangThai(Integer maGiangVien, Integer trangThai);

	/** Giảng viên này có phải giảng viên chính (đang hiệu lực) của lớp không */
	boolean existsByLopHocPhan_MaLopHocPhanAndGiangVien_MaGiangVienAndVaiTroAndTrangThai(
			Integer maLopHocPhan, Integer maGiangVien, Integer vaiTro, Integer trangThai);

	/** Giảng viên theo vai trò của một lớp (trang duyệt điểm cần biết giảng viên chính) */
	@EntityGraph(attributePaths = {"giangVien"})
	List<PhanCongGiangDay> findByLopHocPhan_MaLopHocPhanAndVaiTroAndTrangThai(
			Integer maLopHocPhan, Integer vaiTro, Integer trangThai);

	/** Toàn bộ phân công của một học kỳ, dùng khi phân công hàng loạt */
	@EntityGraph(attributePaths = {"giangVien", "lopHocPhan"})
	List<PhanCongGiangDay> findByLopHocPhan_HocKy_MaHocKyAndTrangThai(Integer maHocKy, Integer trangThai);
}