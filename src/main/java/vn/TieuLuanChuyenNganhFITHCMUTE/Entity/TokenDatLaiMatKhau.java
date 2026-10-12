package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.Getter;
import lombok.Setter;

/**
 * Mã đặt lại mật khẩu gửi qua email.
 *
 * Mỗi lần sinh viên bấm "Quên mật khẩu" sẽ tạo một bản ghi mới.
 * Mã chỉ dùng được MỘT lần và hết hạn sau 30 phút.
 */
@Entity
@Table(name = "TokenDatLaiMatKhau")
@Getter
@Setter
public class TokenDatLaiMatKhau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MaToken")
    private Integer maToken;

    /** Chuỗi ngẫu nhiên gửi kèm trong đường dẫn email */
    @Column(name = "Token", length = 100, nullable = false, unique = true)
    private String token;

    @ManyToOne
    @JoinColumn(name = "MaTaiKhoan")
    private TaiKhoan taiKhoan;

    @Column(name = "ThoiDiemTao", nullable = false)
    private LocalDateTime thoiDiemTao;

    @Column(name = "ThoiDiemHetHan", nullable = false)
    private LocalDateTime thoiDiemHetHan;

    /** Đã dùng để đổi mật khẩu rồi thì không dùng lại được */
    @Column(name = "DaSuDung", nullable = false)
    private boolean daSuDung = false;

    /** Email đã gửi tới, lưu để tra cứu khi cần */
    @Column(name = "EmailNhan", length = 200)
    private String emailNhan;

    /** Mã còn dùng được hay không */
    public boolean conHieuLuc() {
        return !daSuDung && LocalDateTime.now().isBefore(thoiDiemHetHan);
    }
}