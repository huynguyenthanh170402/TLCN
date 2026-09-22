package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.CongNoHocPhi;

@Repository
public interface CongNoHocPhiRepository extends JpaRepository<CongNoHocPhi, Integer> {

	Optional<CongNoHocPhi> findBySinhVien_MaSinhVienAndHocKy_MaHocKy(Integer maSinhVien, Integer maHocKy);

	/** Toàn bộ công nợ của sinh viên, nạp sẵn học kỳ */
	@EntityGraph(attributePaths = {"hocKy"})
	List<CongNoHocPhi> findBySinhVien_MaSinhVien(Integer maSinhVien, Sort sort);
}