package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;

@Repository
public interface HocKyRepository extends JpaRepository<HocKy, Integer> {

	Optional<HocKy> findByMaHocKyCode(String maHocKyCode);

	/** Học kỳ đang diễn ra (TrangThai = 1), lấy cái mới nhất */
	Optional<HocKy> findFirstByTrangThaiOrderByNgayBatDauDesc(Integer trangThai);
}