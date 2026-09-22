package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;

@Repository
public interface PhongHocRepository extends JpaRepository<PhongHoc, Integer> {
}