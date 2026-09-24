package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;

@Repository
public interface LopHocPhanRepository extends JpaRepository<LopHocPhan, Integer> {

	Optional<LopHocPhan> findByMaLop(String maLop);

	boolean existsByMaLop(String maLop);

	boolean existsByHocKy_MaHocKy(Integer maHocKy);

	/** Các lớp theo trạng thái, nạp sẵn học kỳ và học phần */
	@EntityGraph(attributePaths = {"hocKy", "hocPhan"})
	List<LopHocPhan> findByTrangThai(Integer trangThai);

	/** Các lớp của một học kỳ theo trạng thái (trang đăng ký của sinh viên) */
	@EntityGraph(attributePaths = {"hocPhan", "hocKy"})
	List<LopHocPhan> findByHocKy_MaHocKyAndTrangThai(Integer maHocKy, Integer trangThai, Sort sort);

	/** Mọi lớp của một học kỳ (trang quản lý của Phòng Đào tạo) */
	@EntityGraph(attributePaths = {"hocPhan", "hocKy"})
	List<LopHocPhan> findByHocKy_MaHocKy(Integer maHocKy, Sort sort);

	/** Đếm lớp chính khóa đã mở cho một học phần của một khóa, dùng khi lập kế hoạch */
	long countByHocKy_MaHocKyAndHocPhan_MaHocPhanIdAndKhoaApDung(
			Integer maHocKy, Integer maHocPhanId, String khoaApDung);
}