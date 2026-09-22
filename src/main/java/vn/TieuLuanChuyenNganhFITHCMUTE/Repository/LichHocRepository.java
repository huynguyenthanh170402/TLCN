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
}