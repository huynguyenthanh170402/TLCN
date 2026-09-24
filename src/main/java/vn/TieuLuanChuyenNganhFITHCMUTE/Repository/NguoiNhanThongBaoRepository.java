package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.NguoiNhanThongBao;

@Repository
public interface NguoiNhanThongBaoRepository extends JpaRepository<NguoiNhanThongBao, Integer> {

	Optional<NguoiNhanThongBao> findByThongBao_MaThongBaoAndTaiKhoan_MaTaiKhoan(
			Integer maThongBao, Integer maTaiKhoan);

	/** Mã các thông báo tài khoản này đã đọc */
	@Query("""
	       SELECT n.thongBao.maThongBao FROM NguoiNhanThongBao n
	       WHERE n.taiKhoan.maTaiKhoan = :maTaiKhoan AND n.trangThaiDoc = 1
	       """)
	List<Integer> timMaDaDoc(@Param("maTaiKhoan") Integer maTaiKhoan);

	/** Số người đã đọc một thông báo */
	long countByThongBao_MaThongBaoAndTrangThaiDoc(Integer maThongBao, Integer trangThaiDoc);
}
