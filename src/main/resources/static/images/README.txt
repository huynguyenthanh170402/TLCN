HƯỚNG DẪN SỬ DỤNG THƯ MỤC IMAGES
================================

📁 Vị trí thư mục: src/main/resources/static/images/

🖼️ CÁCH THÊM HÌNH ẢNH:

1. Copy file hình ảnh của bạn (ví dụ: background.jpg, logo.png, ...) vào thư mục này

2. Sau đó sử dụng trong HTML/CSS:
   - Trong CSS: url('/images/background.jpg')
   - Trong HTML: <img src="/images/logo.png" alt="Logo">

📝 GHI CHÚ:
- Hỗ trợ các format: JPG, PNG, GIF, WebP
- URL tương đối: /images/filename.ext
- File sẽ tự động được serve bởi Spring Boot

🎨 CÁC HỆ THỐNG HÌNH ẢNH:
- /images/background.jpg  → Background cho hero section
- /images/logo.png        → Logo trường học
- /images/banner.jpg      → Banner cho các trang khác
- /images/icon-*.png      → Icon cho các tính năng

💡 TIP:
- Nén hình ảnh để tăng tốc độ load (Tinypng.com)
- Sử dụng format WebP cho web (tương thích tốt hơn)
- Đặt tên file rõ ràng và không dấu cách

Hãy copy hình ảnh vào thư mục này!
