package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Quyen;

@Repository
public interface QuyenRepository extends JpaRepository<Quyen, Integer> {
}