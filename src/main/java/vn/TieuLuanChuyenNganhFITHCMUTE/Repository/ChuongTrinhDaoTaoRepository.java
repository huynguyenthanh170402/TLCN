package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;

@Repository
public interface ChuongTrinhDaoTaoRepository extends JpaRepository<ChuongTrinhDaoTao, Integer> {

	/** Chương trình đào tạo của một ngành, nạp sẵn học phần */
	@EntityGraph(attributePaths = {"hocPhan"})
	List<ChuongTrinhDaoTao> findByNganh_MaNganhAndTrangThai(Integer maNganh, Integer trangThai, Sort sort);

	/** Học phần này đã nằm trong chương trình của ngành chưa */
	boolean existsByNganh_MaNganhAndHocPhan_MaHocPhanId(Integer maNganh, Integer maHocPhanId);
}