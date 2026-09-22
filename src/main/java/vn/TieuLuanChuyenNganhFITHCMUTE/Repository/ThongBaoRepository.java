package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ThongBao;

@Repository
public interface ThongBaoRepository extends JpaRepository<ThongBao, Integer> {

	/*
	 * Một tài khoản thấy thông báo khi:
	 *   đã đăng, đã tới ngày đăng, chưa hết hạn, VÀ
	 *   (gửi toàn trường
	 *    HOẶC gửi theo nhóm quyền có chứa quyền của tài khoản
	 *    HOẶC gửi cá nhân có tên tài khoản trong danh sách nhận)
	 */
	String DIEU_KIEN_HIEN_THI = """
	        tb.trangThai = 1
	        AND (tb.ngayDang IS NULL OR tb.ngayDang <= :now)
	        AND (tb.ngayHetHan IS NULL OR tb.ngayHetHan >= :now)
	        AND (
	              tb.cheDoNguoiNhan = 1
	           OR (tb.cheDoNguoiNhan = 2 AND EXISTS (
	                  SELECT 1 FROM ThongBaoQuyen q
	                  WHERE q.thongBao = tb AND q.quyen.maQuyen = :maQuyen))
	           OR (tb.cheDoNguoiNhan = 3 AND EXISTS (
	                  SELECT 1 FROM NguoiNhanThongBao n
	                  WHERE n.thongBao = tb AND n.taiKhoan.maTaiKhoan = :maTaiKhoan))
	        )
	        """;

	@Query(value = "SELECT tb FROM ThongBao tb LEFT JOIN FETCH tb.donVi WHERE "
	               + DIEU_KIEN_HIEN_THI + " ORDER BY tb.ngayDang DESC",
	       countQuery = "SELECT COUNT(tb) FROM ThongBao tb WHERE " + DIEU_KIEN_HIEN_THI)
	Page<ThongBao> timThongBaoHienThi(@Param("maQuyen") Integer maQuyen,
	                                  @Param("maTaiKhoan") Integer maTaiKhoan,
	                                  @Param("now") LocalDateTime now,
	                                  Pageable pageable);

	/** Chỉ lấy mã, dùng để đếm chưa đọc và kiểm tra quyền xem chi tiết */
	@Query("SELECT tb.maThongBao FROM ThongBao tb WHERE " + DIEU_KIEN_HIEN_THI)
	List<Integer> timMaThongBaoHienThi(@Param("maQuyen") Integer maQuyen,
	                                   @Param("maTaiKhoan") Integer maTaiKhoan,
	                                   @Param("now") LocalDateTime now);
}
