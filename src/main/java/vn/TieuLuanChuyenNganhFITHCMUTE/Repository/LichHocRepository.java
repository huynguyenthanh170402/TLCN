package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;

@Repository
public interface LichHocRepository extends JpaRepository<LichHoc, Integer> {

	/** Lịch của nhiều lớp cùng lúc, nạp sẵn phòng và học phần */
	@EntityGraph(attributePaths = {"phongHoc", "lopHocPhan.hocPhan"})
	List<LichHoc> findByLopHocPhan_MaLopHocPhanIn(Collection<Integer> dsMaLop);

	/** Các buổi học đã xếp vào một phòng, một thứ, trong một học kỳ (kiểm tra trùng phòng) */
	@EntityGraph(attributePaths = {"lopHocPhan"})
	List<LichHoc> findByPhongHoc_MaPhongHocAndThuAndLopHocPhan_HocKy_MaHocKy(
			Integer maPhongHoc, Integer thu, Integer maHocKy);

	/** Toàn bộ lịch của một học kỳ, dùng khi xếp lớp hàng loạt */
	@EntityGraph(attributePaths = {"phongHoc"})
	List<LichHoc> findByLopHocPhan_HocKy_MaHocKy(Integer maHocKy);
}