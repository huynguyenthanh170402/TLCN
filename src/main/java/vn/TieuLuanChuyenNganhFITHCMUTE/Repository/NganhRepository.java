package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;

@Repository
public interface NganhRepository extends JpaRepository<Nganh, Integer> {

	Optional<Nganh> findByTenNganh(String tenNganh);

	/** Một ngành kèm khoa quản lý, cho trang giới thiệu của khách */
	@EntityGraph(attributePaths = {"donVi"})
	Optional<Nganh> findWithDonViByMaNganh(Integer maNganh);

	/** Các ngành của một khoa, nạp sẵn khoa */
	@EntityGraph(attributePaths = {"donVi"})
	List<Nganh> findByDonVi_MaDonViOrderByTenNganh(Integer maDonVi);
}
