package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBaoQuyen;

@Repository
public interface ThongBaoQuyenRepository extends JpaRepository<ThongBaoQuyen, Integer> {
}
