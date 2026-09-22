package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LoaiDonVi;

@Repository
public interface LoaiDonViRepository extends JpaRepository<LoaiDonVi, Integer> {
}