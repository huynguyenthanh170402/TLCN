package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.SinhVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.SinhVienRepository;

import lombok.RequiredArgsConstructor;

/**
 * Xử lý tải lên ảnh đại diện của sinh viên.
 *
 * Nguyên tắc:
 *  - File ảnh lưu trên ổ đĩa, database chỉ lưu TÊN FILE.
 *  - Không bao giờ dùng lại tên file do người dùng gửi lên.
 *  - Kiểm tra nội dung file thật sự là ảnh, không chỉ tin vào phần mở rộng.
 */
@Service
@RequiredArgsConstructor
public class AnhDaiDienService {

    private final SinhVienRepository sinhVienRepository;

    /** Thư mục gốc chứa file tải lên, khai báo trong application.properties */
    @Value("${app.upload.thu-muc:uploads}")
    private String thuMucGoc;

    /** Dung lượng tối đa cho một ảnh đại diện: 2 MB */
    private static final long DUNG_LUONG_TOI_DA = 2L * 1024 * 1024;

    /** Các định dạng ảnh được chấp nhận */
    private static final Set<String> LOAI_CHO_PHEP = Set.of("image/jpeg", "image/png", "image/webp");

    /** Kết quả trả về cho controller */
    public record KetQua(boolean thanhCong, String thongBao) { }

    /**
     * Lưu ảnh đại diện mới cho sinh viên và xóa ảnh cũ nếu có.
     */
    @Transactional
    public KetQua luuAnh(SinhVien sinhVien, MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return new KetQua(false, "Bạn chưa chọn ảnh.");
        }

        if (file.getSize() > DUNG_LUONG_TOI_DA) {
            return new KetQua(false, "Ảnh vượt quá 2 MB. Vui lòng chọn ảnh nhỏ hơn.");
        }

        String loaiFile = file.getContentType();
        if (loaiFile == null || !LOAI_CHO_PHEP.contains(loaiFile.toLowerCase())) {
            return new KetQua(false, "Chỉ chấp nhận ảnh định dạng JPG, PNG hoặc WEBP.");
        }

        // Đọc thử nội dung để chắc chắn đây là ảnh thật,
        // tránh trường hợp đổi tên file lạ thành .jpg rồi tải lên.
        try (InputStream in = file.getInputStream()) {
            BufferedImage anh = ImageIO.read(in);
            if (anh == null) {
                return new KetQua(false, "File tải lên không phải là ảnh hợp lệ.");
            }
        } catch (IOException e) {
            return new KetQua(false, "Không đọc được file tải lên.");
        }

        String duoiFile = switch (loaiFile.toLowerCase()) {
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default           -> ".jpg";
        };

        // Tên file hoàn toàn do hệ thống sinh ra, không lấy từ người dùng.
        String tenFileMoi = "sv" + sinhVien.getMaSoSinhVien() + "-"
                + UUID.randomUUID().toString().substring(0, 8) + duoiFile;

        try {
            Path thuMuc = Paths.get(thuMucGoc, "anhdaidien").toAbsolutePath().normalize();
            Files.createDirectories(thuMuc);

            Path dich = thuMuc.resolve(tenFileMoi);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dich, StandardCopyOption.REPLACE_EXISTING);
            }

            String anhCu = sinhVien.getAnhDaiDien();

            sinhVien.setAnhDaiDien(tenFileMoi);
            sinhVienRepository.save(sinhVien);

            xoaAnhCu(thuMuc, anhCu);

            return new KetQua(true, "Đã cập nhật ảnh đại diện.");

        } catch (IOException e) {
            return new KetQua(false, "Lưu ảnh thất bại: " + e.getMessage());
        }
    }

    /**
     * Xóa ảnh đại diện hiện tại, quay về ảnh mặc định.
     */
    @Transactional
    public KetQua xoaAnh(SinhVien sinhVien) {
        String anhCu = sinhVien.getAnhDaiDien();
        if (anhCu == null || anhCu.isBlank()) {
            return new KetQua(false, "Bạn chưa có ảnh đại diện.");
        }

        sinhVien.setAnhDaiDien(null);
        sinhVienRepository.save(sinhVien);

        Path thuMuc = Paths.get(thuMucGoc, "anhdaidien").toAbsolutePath().normalize();
        xoaAnhCu(thuMuc, anhCu);

        return new KetQua(true, "Đã xóa ảnh đại diện.");
    }

    /** Xóa file ảnh cũ, bỏ qua nếu file không còn tồn tại. */
    private void xoaAnhCu(Path thuMuc, String tenFile) {
        if (tenFile == null || tenFile.isBlank()) {
            return;
        }
        try {
            Path duongDan = thuMuc.resolve(tenFile).normalize();
            // Chặn trường hợp tên file chứa ../ để thoát ra ngoài thư mục
            if (duongDan.startsWith(thuMuc)) {
                Files.deleteIfExists(duongDan);
            }
        } catch (IOException ignored) {
            // Xóa không được thì bỏ qua, không làm hỏng luồng cập nhật
        }
    }
}