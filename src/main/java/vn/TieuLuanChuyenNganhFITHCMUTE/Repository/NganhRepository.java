package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;

@Repository
public interface NganhRepository extends JpaRepository<Nganh, Integer> {

	Optional<Nganh> findByTenNganh(String tenNganh);
}