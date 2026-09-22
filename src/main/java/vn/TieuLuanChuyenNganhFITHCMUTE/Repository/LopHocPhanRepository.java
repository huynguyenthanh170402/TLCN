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

	/** Học kỳ này đã có lớp nào chưa */
	boolean existsByHocKy_MaHocKy(Integer maHocKy);

	/** Các lớp theo trạng thái, nạp sẵn học kỳ và học phần (dùng trong seeder) */
	@EntityGraph(attributePaths = {"hocKy", "hocPhan"})
	List<LopHocPhan> findByTrangThai(Integer trangThai);

	/** Các lớp của một học kỳ theo trạng thái, nạp sẵn học phần (dùng cho trang đăng ký) */
	@EntityGraph(attributePaths = {"hocPhan", "hocKy"})
	List<LopHocPhan> findByHocKy_MaHocKyAndTrangThai(Integer maHocKy, Integer trangThai, Sort sort);
}
