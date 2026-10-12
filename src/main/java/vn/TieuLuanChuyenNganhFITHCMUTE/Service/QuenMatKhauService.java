package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TokenDatLaiMatKhau;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TokenDatLaiMatKhauRepository;

/**
 * Luồng quên mật khẩu:
 *
 *  1. Sinh viên nhập email  ->  guiLienKet()
 *  2. Hệ thống sinh mã ngẫu nhiên, lưu vào bảng TokenDatLaiMatKhau,
 *     rồi IN ĐƯỜNG DẪN RA CỬA SỔ CONSOLE.
 *  3. Sinh viên bấm vào đường dẫn  ->  kiemTraToken()
 *  4. Nhập mật khẩu mới  ->  datLaiMatKhau()
 *
 * ===================================================================
 *  BẢN KHÔNG GỬI EMAIL THẬT
 *
 *  Không dùng JavaMailSender nên chạy được ngay, không cần thư viện
 *  spring-boot-starter-mail. Đường dẫn đặt lại mật khẩu in ra cửa sổ
 *  Console của STS, chép dán vào trình duyệt để thử.
 *
 *  Khi nào nạp được thư viện mail thì chỉ cần sửa phương thức
 *  phatHanhLienKet() ở cuối file, phần còn lại giữ nguyên.
 * ===================================================================
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuenMatKhauService {

    private final TokenDatLaiMatKhauRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager em;

    @Value("${app.url-goc:http://localhost:8082}")
    private String urlGoc;

    /** Mã có hiệu lực trong 30 phút */
    private static final int SO_PHUT_HIEU_LUC = 30;

    /** Khoảng cách tối thiểu giữa hai lần gửi cho cùng một tài khoản */
    private static final int SO_PHUT_CHO_GUI_LAI = 2;

    public record KetQua(boolean thanhCong, String thongBao) { }

    // ==================================================================
    //  Bước 1 và 2: nhận email, phát hành mã đặt lại
    // ==================================================================

    @Transactional
    public KetQua guiLienKet(String email) {

        if (email == null || email.isBlank()) {
            return new KetQua(false, "Vui lòng nhập địa chỉ email.");
        }

        String emailSach = email.trim().toLowerCase();

        // Thông báo dùng chung cho mọi trường hợp.
        // Không tiết lộ email có tồn tại trong hệ thống hay không,
        // nếu không người lạ có thể dò ra danh sách email thật của trường.
        String thongBaoChung = "Nếu email này gắn với một tài khoản, "
                + "hệ thống đã gửi hướng dẫn đặt lại mật khẩu. "
                + "Vui lòng kiểm tra hộp thư, kể cả mục Spam.";

        Optional<SinhVien> timThay = timSinhVienTheoEmail(emailSach);
        if (timThay.isEmpty()) {
            log.info("Yêu cầu đặt lại mật khẩu cho email không tồn tại: {}", emailSach);
            return new KetQua(true, thongBaoChung);
        }

        SinhVien sv = timThay.get();
        TaiKhoan tk = sv.getTaiKhoan();
        if (tk == null) {
            log.warn("Sinh viên {} chưa có tài khoản đăng nhập", sv.getMaSoSinhVien());
            return new KetQua(true, thongBaoChung);
        }

        // Tài khoản bị khóa thì không cho đặt lại mật khẩu
        if (tk.getTrangThai() != null && tk.getTrangThai() == 0) {
            log.warn("Tài khoản {} đang bị khóa, bỏ qua yêu cầu đặt lại mật khẩu",
                    tk.getTenDangNhap());
            return new KetQua(true, thongBaoChung);
        }

        // Chặn bấm gửi liên tục
        List<TokenDatLaiMatKhau> dangCo =
                tokenRepository.timConHieuLuc(tk.getMaTaiKhoan(), LocalDateTime.now());
        if (!dangCo.isEmpty()) {
            LocalDateTime lanGanNhat = dangCo.get(0).getThoiDiemTao();
            if (lanGanNhat.isAfter(LocalDateTime.now().minusMinutes(SO_PHUT_CHO_GUI_LAI))) {
                return new KetQua(false,
                        "Bạn vừa gửi yêu cầu cách đây chưa tới " + SO_PHUT_CHO_GUI_LAI
                      + " phút. Vui lòng kiểm tra hộp thư hoặc thử lại sau.");
            }
        }

        // Địa chỉ thật sự nhận thư.
        // Ưu tiên email cá nhân vì email trường trong đồ án là dữ liệu mẫu,
        // không có hộp thư thật nên gửi tới đó sẽ không ai nhận được.
        String emailNhan = chonEmailNhan(sv);
        if (emailNhan == null) {
            log.warn("Sinh viên {} chưa khai email cá nhân, không có nơi nhận thư",
                    sv.getMaSoSinhVien());
            return new KetQua(false,
                    "Tài khoản này chưa khai email cá nhân nên hệ thống không gửi được "
                  + "hướng dẫn đặt lại mật khẩu. Vui lòng liên hệ Phòng Công tác Sinh viên.");
        }

        // Mã cũ hết hiệu lực ngay khi phát mã mới
        tokenRepository.voHieuHoaMaCu(tk.getMaTaiKhoan());

        TokenDatLaiMatKhau token = new TokenDatLaiMatKhau();
        token.setToken(UUID.randomUUID().toString().replace("-", ""));
        token.setTaiKhoan(tk);
        token.setThoiDiemTao(LocalDateTime.now());
        token.setThoiDiemHetHan(LocalDateTime.now().plusMinutes(SO_PHUT_HIEU_LUC));
        token.setDaSuDung(false);
        token.setEmailNhan(emailNhan);
        tokenRepository.save(token);

        String lienKet = urlGoc + "/datlaimatkhau?token=" + token.getToken();
        phatHanhLienKet(emailNhan, sv.getHoTen(), lienKet);

        return new KetQua(true, thongBaoChung);
    }

    /**
     * Chọn hộp thư nhận: email cá nhân trước, email trường chỉ là phương án dự phòng.
     * Trả về null khi sinh viên không có địa chỉ nào dùng được.
     */
    private String chonEmailNhan(SinhVien sv) {
        String caNhan = sv.getEmailCaNhan();
        if (caNhan != null && !caNhan.isBlank()) {
            return caNhan.trim();
        }
        String truong = sv.getEmailTruong();
        if (truong != null && !truong.isBlank()) {
            return truong.trim();
        }
        return null;
    }

    /** Tìm sinh viên theo email trường hoặc email cá nhân */
    private Optional<SinhVien> timSinhVienTheoEmail(String email) {
        List<SinhVien> ds = em.createQuery("""
                    select sv from SinhVien sv
                    where lower(sv.emailTruong) = :email
                       or lower(sv.emailCaNhan) = :email
                    """, SinhVien.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();

        return ds.isEmpty() ? Optional.empty() : Optional.of(ds.get(0));
    }

    // ==================================================================
    //  Bước 3: kiểm tra mã khi sinh viên bấm vào đường dẫn
    // ==================================================================

    public Optional<TokenDatLaiMatKhau> kiemTraToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        return tokenRepository.findByToken(token)
                .filter(TokenDatLaiMatKhau::conHieuLuc);
    }

    // ==================================================================
    //  Bước 4: đặt mật khẩu mới
    // ==================================================================

    @Transactional
    public KetQua datLaiMatKhau(String token, String matKhauMoi, String xacNhan) {

        Optional<TokenDatLaiMatKhau> tim = kiemTraToken(token);
        if (tim.isEmpty()) {
            return new KetQua(false,
                    "Đường dẫn không hợp lệ hoặc đã hết hạn. Vui lòng gửi lại yêu cầu.");
        }

        if (matKhauMoi == null || matKhauMoi.length() < 6) {
            return new KetQua(false, "Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!matKhauMoi.equals(xacNhan)) {
            return new KetQua(false, "Hai lần nhập mật khẩu không khớp nhau.");
        }

        TokenDatLaiMatKhau t = tim.get();
        TaiKhoan tk = t.getTaiKhoan();

        tk.setMatKhauBam(passwordEncoder.encode(matKhauMoi));
        em.merge(tk);

        // Đánh dấu đã dùng để đường dẫn không xài lại được
        t.setDaSuDung(true);
        tokenRepository.save(t);

        log.info("Tài khoản {} đã đặt lại mật khẩu thành công", tk.getTenDangNhap());
        return new KetQua(true, "Đặt lại mật khẩu thành công. Bạn có thể đăng nhập ngay.");
    }

    // ==================================================================
    //  Nơi duy nhất cần sửa khi chuyển sang gửi email thật
    // ==================================================================

    /**
     * Hiện tại in đường dẫn ra Console thay cho gửi email.
     *
     * Khi nạp được spring-boot-starter-mail, thay thân phương thức này bằng:
     *
     *   SimpleMailMessage thu = new SimpleMailMessage();
     *   thu.setTo(email);
     *   thu.setSubject("Đặt lại mật khẩu - Cổng sinh viên HCMUTE");
     *   thu.setText("Chào " + hoTen + ", bấm vào đây: " + lienKet);
     *   mailSender.send(thu);
     *
     * và khai báo thêm trường:  private final JavaMailSender mailSender;
     */
    private void phatHanhLienKet(String email, String hoTen, String lienKet) {
        log.info("""

                ==========================================================
                 ĐƯỜNG DẪN ĐẶT LẠI MẬT KHẨU (chế độ thử, chưa gửi email)
                ----------------------------------------------------------
                 Người nhận : {} <{}>
                 Hiệu lực   : {} phút
                 Đường dẫn  : {}
                ==========================================================
                """, hoTen, email, SO_PHUT_HIEU_LUC, lienKet);
    }
}