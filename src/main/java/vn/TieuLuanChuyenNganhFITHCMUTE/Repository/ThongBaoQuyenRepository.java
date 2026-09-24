package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBaoQuyen;

@Repository
public interface ThongBaoQuyenRepository extends JpaRepository<ThongBaoQuyen, Integer> {

	/** Nhóm quyền nhận của nhiều thông báo cùng lúc, nạp sẵn tên quyền */
	@EntityGraph(attributePaths = {"quyen", "thongBao"})
	List<ThongBaoQuyen> findByThongBao_MaThongBaoIn(Collection<Integer> dsMaThongBao);
}
