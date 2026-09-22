package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;

@Repository
public interface HocPhanRepository extends JpaRepository<HocPhan, Integer> {

	Optional<HocPhan> findByMaHocPhan(String maHocPhan);
}