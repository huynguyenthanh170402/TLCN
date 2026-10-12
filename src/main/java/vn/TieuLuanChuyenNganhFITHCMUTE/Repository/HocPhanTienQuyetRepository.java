package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhanTienQuyet;

@Repository
public interface HocPhanTienQuyetRepository extends JpaRepository<HocPhanTienQuyet, Integer> {

	/** Điều kiện của nhiều học phần cùng lúc, nạp sẵn môn điều kiện để lấy tên */
	@EntityGraph(attributePaths = {"hocPhanDieuKien"})
	List<HocPhanTienQuyet> findByHocPhan_MaHocPhanIdInAndTrangThai(
			Collection<Integer> dsMaHocPhan, Integer trangThai);

	/** Điều kiện của một học phần */
	@EntityGraph(attributePaths = {"hocPhanDieuKien"})
	List<HocPhanTienQuyet> findByHocPhan_MaHocPhanIdAndTrangThai(Integer maHocPhanId, Integer trangThai);

	boolean existsByHocPhan_MaHocPhanIdAndHocPhanDieuKien_MaHocPhanId(
			Integer maHocPhanId, Integer maHocPhanDieuKien);
}