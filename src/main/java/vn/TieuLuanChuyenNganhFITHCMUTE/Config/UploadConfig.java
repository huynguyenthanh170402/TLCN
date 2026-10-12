package vn.TieuLuanChuyenNganhFITHCMUTE.Config;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Cho phép trình duyệt xem được file đã tải lên.
 *
 * File nằm ở thư mục uploads/ bên ngoài mã nguồn, không nằm trong static/.
 * Lý do: static/ bị đóng gói vào file .jar lúc build, ảnh tải lên sau đó
 * sẽ không được phục vụ, và mỗi lần build lại là mất sạch.
 *
 * Ánh xạ:  uploads/anhdaidien/abc.jpg  ->  http://localhost:8082/uploads/abc.jpg
 */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    @Value("${app.upload.thu-muc:uploads}")
    private String thuMucGoc;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path thuMuc = Paths.get(thuMucGoc, "anhdaidien").toAbsolutePath().normalize();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + thuMuc.toString() + "/")
                .setCachePeriod(3600);
    }
}