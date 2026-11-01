package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DotXetTotNghiep;

@Repository
public interface DotXetTotNghiepRepository extends JpaRepository<DotXetTotNghiep, Integer> {

	@EntityGraph(attributePaths = {"hocKy"})
	Optional<DotXetTotNghiep> findWithHocKyByMaDot(Integer maDot);

	@EntityGraph(attributePaths = {"hocKy"})
	List<DotXetTotNghiep> findAllBy(Sort sort);

	/** Các đợt đang nhận đơn, để sinh viên thấy đợt nào nộp được */
	@EntityGraph(attributePaths = {"hocKy"})
	List<DotXetTotNghiep> findByTrangThai(Integer trangThai, Sort sort);
}
