package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;

@Repository
public interface DonViRepository extends JpaRepository<DonVi, Integer> {

	Optional<DonVi> findByTenDonVi(String tenDonVi);
}