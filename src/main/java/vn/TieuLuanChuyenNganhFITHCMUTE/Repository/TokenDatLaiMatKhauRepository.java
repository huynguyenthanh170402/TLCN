package vn.TieuLuanChuyenNganhFITHCMUTE.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TokenDatLaiMatKhau;

public interface TokenDatLaiMatKhauRepository extends JpaRepository<TokenDatLaiMatKhau, Integer> {

    Optional<TokenDatLaiMatKhau> findByToken(String token);

    /**
     * Mã gần nhất còn hiệu lực của một tài khoản.
     * Dùng để chặn bấm gửi liên tục trong thời gian ngắn.
     */
    @Query("""
           select t from TokenDatLaiMatKhau t
           where t.taiKhoan.maTaiKhoan = :maTaiKhoan
             and t.daSuDung = false
             and t.thoiDiemHetHan > :bayGio
           order by t.thoiDiemTao desc
           """)
    java.util.List<TokenDatLaiMatKhau> timConHieuLuc(@Param("maTaiKhoan") Integer maTaiKhoan,
                                                     @Param("bayGio") LocalDateTime bayGio);

    /** Vô hiệu hóa mọi mã cũ của tài khoản trước khi phát mã mới */
    @Modifying
    @Query("""
           update TokenDatLaiMatKhau t set t.daSuDung = true
           where t.taiKhoan.maTaiKhoan = :maTaiKhoan and t.daSuDung = false
           """)
    int voHieuHoaMaCu(@Param("maTaiKhoan") Integer maTaiKhoan);

    /** Dọn các mã đã hết hạn, gọi định kỳ cho gọn bảng */
    @Modifying
    @Query("delete from TokenDatLaiMatKhau t where t.thoiDiemHetHan < :moc")
    int xoaMaHetHan(@Param("moc") LocalDateTime moc);
}