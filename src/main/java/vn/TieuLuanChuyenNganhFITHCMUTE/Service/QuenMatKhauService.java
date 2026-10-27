package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.TaiKhoan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.TaiKhoanRepository;

/**
 * Quên mật khẩu: cấp một mật khẩu tạm, gửi về email cá nhân của sinh viên.
 *
 * Trường thật gửi về email trường. Ở đây gửi về email cá nhân vì tiểu luận
 * không có quyền dùng hệ thống thư của trường; luồng nghiệp vụ giữ nguyên.
 *
 * Mật khẩu tạm chỉ sống 5 phút và chỉ dùng được một lần: đăng nhập xong là
 * hệ thống buộc đổi ngay, không vào được trang nào khác cho tới khi đổi.
 * Gửi thẳng mật khẩu qua thư vốn yếu hơn gửi liên kết dùng một lần, nên hai
 * ràng buộc này là phần bù: kẻ đọc trộm thư chỉ có 5 phút, và chủ tài khoản
 * biết ngay vì mật khẩu của họ đã bị đổi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class QuenMatKhauService {

    /** Mật khẩu tạm sống 5 phút */
    public static final int SO_PHUT_HIEU_LUC = 5;

    /** Chặn bấm gửi liên tục: phải chờ bấy nhiêu phút mới xin được mã mới */
    private static final int SO_PHUT_CHO_GUI_LAI = 1;

    private static final int DO_DAI_MAT_KHAU = 10;

    /**
     * Bộ ký tự bỏ đi những chữ dễ nhìn nhầm nhau khi chép tay từ thư:
     * số 0 với chữ O, số 1 với chữ l và I.
     */
    private static final String KY_TU =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";

    private static final DateTimeFormatter DINH_DANG_GIO =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy");

    private final TaiKhoanRepository taiKhoanRepository;
    private final PasswordEncoder passwordEncoder;

    /** Việc gửi thư và phần in dự phòng ra Console nằm chung ở MailService */
    private final MailService mailService;

    @PersistenceContext
    private EntityManager em;

    public record KetQua(boolean thanhCong, String thongBao) { }

    /**
     * Thông báo trả về luôn giống nhau dù email có tồn tại hay không.
     *
     * Nếu báo "email không tồn tại", kẻ xấu chỉ cần thử lần lượt là dò ra
     * danh sách email thật của trường. Đây là lỗi dò tài khoản, rất hay gặp
     * ở màn hình quên mật khẩu.
     */
    private static final String THONG_BAO_CHUNG =
            "Nếu email vừa nhập có trong hệ thống, mật khẩu tạm đã được gửi tới hộp thư đó. "
            + "Mật khẩu tạm chỉ dùng được trong " + SO_PHUT_HIEU_LUC + " phút.";

    // ===================== CẤP MẬT KHẨU TẠM =====================

    @Transactional
    public KetQua capMatKhauTam(String email) {
        String emailSach = (email == null) ? "" : email.trim().toLowerCase();
        if (emailSach.isEmpty()) {
            return new KetQua(false, "Vui lòng nhập email.");
        }

        Optional<SinhVien> oSv = timSinhVienTheoEmail(emailSach);
        if (oSv.isEmpty()) {
            log.info("Yêu cầu cấp mật khẩu tạm cho email không có trong hệ thống: {}", emailSach);
            return new KetQua(true, THONG_BAO_CHUNG);
        }

        SinhVien sv = oSv.get();
        TaiKhoan tk = sv.getTaiKhoan();

        if (tk == null) {
            log.warn("Sinh viên {} chưa có tài khoản đăng nhập", sv.getMaSoSinhVien());
            return new KetQua(true, THONG_BAO_CHUNG);
        }
        if (tk.getTrangThai() == null || tk.getTrangThai() != 1) {
            log.warn("Tài khoản {} đang bị khóa, bỏ qua yêu cầu cấp mật khẩu tạm",
                    tk.getTenDangNhap());
            return new KetQua(true, THONG_BAO_CHUNG);
        }

        // Vừa xin xong thì chờ một chút, tránh bấm liên tục làm đầy hộp thư
        if (tk.matKhauTamConHieuLuc()
                && tk.getHanMatKhauTam()
                      .minusMinutes(SO_PHUT_HIEU_LUC - SO_PHUT_CHO_GUI_LAI)
                      .isAfter(LocalDateTime.now())) {
            return new KetQua(true, THONG_BAO_CHUNG);
        }

        String emailNhan = chonEmailNhan(sv);
        if (emailNhan == null) {
            log.warn("Sinh viên {} chưa khai email nào, không có nơi nhận thư",
                    sv.getMaSoSinhVien());
            return new KetQua(true, THONG_BAO_CHUNG);
        }

        String matKhauTam = sinhMatKhau();
        LocalDateTime han = LocalDateTime.now().plusMinutes(SO_PHUT_HIEU_LUC);

        // Ghi đè luôn mật khẩu cũ. Chủ tài khoản vì thế biết ngay có người
        // vừa yêu cầu đặt lại, thay vì âm thầm tồn tại hai mật khẩu dùng được.
        tk.setMatKhauBam(passwordEncoder.encode(matKhauTam));
        tk.setMatKhauTamThoi(Boolean.TRUE);
        tk.setHanMatKhauTam(han);
        taiKhoanRepository.save(tk);

        guiThu(emailNhan, sv.getHoTen(), tk.getTenDangNhap(), matKhauTam, han);

        return new KetQua(true, THONG_BAO_CHUNG);
    }

    /** Ưu tiên email cá nhân, không có thì dùng email trường */
    private String chonEmailNhan(SinhVien sv) {
        if (sv.getEmailCaNhan() != null && !sv.getEmailCaNhan().isBlank())
            return sv.getEmailCaNhan().trim();
        if (sv.getEmailTruong() != null && !sv.getEmailTruong().isBlank())
            return sv.getEmailTruong().trim();
        return null;
    }

    /** Tra theo cả email cá nhân lẫn email trường, không phân biệt hoa thường */
    private Optional<SinhVien> timSinhVienTheoEmail(String email) {
        List<SinhVien> ds = em.createQuery("""
                SELECT sv FROM SinhVien sv
                LEFT JOIN FETCH sv.taiKhoan
                WHERE LOWER(sv.emailCaNhan) = :email OR LOWER(sv.emailTruong) = :email
                """, SinhVien.class)
                .setParameter("email", email)
                .setMaxResults(1)
                .getResultList();
        return ds.isEmpty() ? Optional.empty() : Optional.of(ds.get(0));
    }

    /** Mật khẩu ngẫu nhiên, dùng SecureRandom chứ không dùng Random thường */
    private String sinhMatKhau() {
        SecureRandom rd = new SecureRandom();
        StringBuilder sb = new StringBuilder(DO_DAI_MAT_KHAU);
        for (int i = 0; i < DO_DAI_MAT_KHAU; i++) {
            sb.append(KY_TU.charAt(rd.nextInt(KY_TU.length())));
        }
        return sb.toString();
    }

    // ===================== GỬI THƯ =====================

    private void guiThu(String email, String hoTen, String tenDangNhap,
                        String matKhauTam, LocalDateTime han) {

        String tieuDe = "Mật khẩu tạm — Cổng thông tin sinh viên HCMUTE";
        String noiDung = """
                Chào %s,

                Hệ thống vừa nhận yêu cầu đặt lại mật khẩu cho tài khoản của bạn.

                    Tên đăng nhập : %s
                    Mật khẩu tạm  : %s

                Mật khẩu tạm này hết hiệu lực lúc %s, tức là sau %d phút.
                Đăng nhập xong, hệ thống sẽ yêu cầu bạn đặt mật khẩu mới ngay
                trước khi vào được bất kỳ trang nào.

                Nếu không phải bạn yêu cầu, hãy báo Phòng Công tác Sinh viên:
                mật khẩu cũ của bạn đã bị thay thế.

                Đây là thư tự động, vui lòng không trả lời.
                """.formatted(hoTen, tenDangNhap, matKhauTam,
                              han.format(DINH_DANG_GIO), SO_PHUT_HIEU_LUC);

        mailService.gui(email, tieuDe, noiDung);
    }

    // ===================== ĐỔI MẬT KHẨU TẠM =====================

    /**
     * Đổi mật khẩu tạm sang mật khẩu chính thức.
     * Gọi từ màn hình bắt buộc đổi sau khi đăng nhập.
     */
    @Transactional
    public KetQua doiMatKhauTam(String tenDangNhap, String matKhauMoi, String xacNhan) {
        TaiKhoan tk = taiKhoanRepository.findByTenDangNhap(tenDangNhap).orElse(null);
        if (tk == null) {
            return new KetQua(false, "Không tìm thấy tài khoản.");
        }
        if (matKhauMoi == null || matKhauMoi.length() < 6) {
            return new KetQua(false, "Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        if (!matKhauMoi.equals(xacNhan)) {
            return new KetQua(false, "Hai ô mật khẩu mới không khớp nhau.");
        }
        if (passwordEncoder.matches(matKhauMoi, tk.getMatKhauBam())) {
            return new KetQua(false, "Mật khẩu mới trùng với mật khẩu tạm vừa được cấp.");
        }

        tk.setMatKhauBam(passwordEncoder.encode(matKhauMoi));
        tk.setMatKhauTamThoi(Boolean.FALSE);
        tk.setHanMatKhauTam(null);
        taiKhoanRepository.save(tk);

        log.info("Tài khoản {} đã đổi mật khẩu tạm sang mật khẩu chính thức", tenDangNhap);
        return new KetQua(true, "Đã đổi mật khẩu. Từ giờ hãy dùng mật khẩu mới để đăng nhập.");
    }
}
