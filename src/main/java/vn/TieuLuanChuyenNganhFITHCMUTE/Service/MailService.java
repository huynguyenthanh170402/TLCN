package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Gửi thư của toàn hệ thống.
 *
 * Gom về một chỗ vì có nhiều nơi cần gửi: cấp mật khẩu tạm, phiếu xác nhận
 * đăng ký học phần, sau này là thông báo học phí. Mỗi nơi tự dựng
 * JavaMailSender riêng thì phần xử lý lỗi và phần in dự phòng sẽ nhân bản
 * ra nhiều bản, sửa một chỗ quên chỗ kia.
 *
 * Chưa khai báo hộp thư thì không báo lỗi ra ngoài, chỉ in nội dung ra
 * Console. Nhờ vậy máy chưa cấu hình gì vẫn chạy và vẫn thử được nghiệp vụ.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MailService {

	/** Dùng ObjectProvider để ứng dụng khởi động được cả khi không có bean gửi thư */
	private final ObjectProvider<JavaMailSender> mailSender;

	@Value("${app.mail.from:no-reply@hcmute.edu.vn}")
	private String nguoiGui;

	/**
	 * @return true nếu thư đã được chuyển cho máy chủ thư
	 */
	public boolean gui(String nguoiNhan, String tieuDe, String noiDung) {
		if (nguoiNhan == null || nguoiNhan.isBlank()) {
			log.warn("Không có địa chỉ nhận, bỏ qua thư: {}", tieuDe);
			return false;
		}

		JavaMailSender sender = mailSender.getIfAvailable();
		if (sender == null) {
			inRaConsole(nguoiNhan, tieuDe, noiDung, "chưa khai báo cấu hình thư");
			return false;
		}

		try {
			SimpleMailMessage thu = new SimpleMailMessage();
			thu.setFrom(nguoiGui);
			thu.setTo(nguoiNhan);
			thu.setSubject(tieuDe);
			thu.setText(noiDung);
			sender.send(thu);
			log.info("Đã gửi thư \"{}\" tới {}", tieuDe, nguoiNhan);
			return true;
		} catch (Exception e) {
			// Không ném lỗi ra ngoài: nghiệp vụ chính đã xong, không thể vì
			// hỏng thư mà cuộn ngược giao dịch đã ghi vào cơ sở dữ liệu.
			inRaConsole(nguoiNhan, tieuDe, noiDung, moTaLoi(e));
			return false;
		}
	}

	/**
	 * Lấy cả câu lỗi ngoài cùng lẫn câu sâu nhất.
	 *
	 * Spring bọc lỗi SMTP lại thành "Authentication failed", ba chữ đó không
	 * cho biết gì. Mã thật của Google nằm ở tầng dưới cùng, ví dụ
	 * "535-5.7.8 Username and Password not accepted".
	 */
	private String moTaLoi(Throwable e) {
		Throwable goc = e;
		while (goc.getCause() != null && goc.getCause() != goc) {
			goc = goc.getCause();
		}
		String ngoai = e.getMessage();
		String sau = goc.getMessage();
		if (sau != null && !sau.equals(ngoai)) {
			return ngoai + " — " + sau.replace("\n", " ").trim();
		}
		return String.valueOf(ngoai);
	}

	/** Không gửi được thì in ra Console để còn thử được khi làm bài */
	private void inRaConsole(String email, String tieuDe, String noiDung, String lyDo) {
		log.warn("""

				====================== THƯ KHÔNG GỬI ĐƯỢC ======================
				Lý do   : {}
				Gửi tới : {}
				Tiêu đề : {}
				{}
				================================================================
				""", lyDo, email, tieuDe, noiDung);
	}
}
