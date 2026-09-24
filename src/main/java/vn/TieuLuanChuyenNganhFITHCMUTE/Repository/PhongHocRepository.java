package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;

@Repository
public interface PhongHocRepository extends JpaRepository<PhongHoc, Integer> {

	/** Phòng theo trạng thái (1 = đang sử dụng), xếp theo mã phòng */
	List<PhongHoc> findByTrangThaiOrderByMaPhong(Integer trangThai);
}
