package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.DonVi;

@Repository
public interface DonViRepository extends JpaRepository<DonVi, Integer> {

	Optional<DonVi> findByTenDonVi(String tenDonVi);

	/** Đơn vị theo loại (1 Khoa, 2 Phòng ban, 3 Trung tâm, 4 Viện), xếp theo tên */
	List<DonVi> findByLoaiDonVi_MaLoaiDonViOrderByTenDonVi(Integer maLoaiDonVi);
}
