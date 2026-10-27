package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

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

	/**
	 * Dòng khung của một cặp ngành và học phần, kể cả dòng đã ngừng áp dụng.
	 *
	 * Bảng có ràng buộc duy nhất trên cặp này nên khi thêm lại một học phần
	 * từng bị gỡ, phải tìm ra dòng cũ để bật lại thay vì chèn dòng mới.
	 */
	Optional<ChuongTrinhDaoTao> findByNganh_MaNganhAndHocPhan_MaHocPhanId(
			Integer maNganh, Integer maHocPhanId);
}
