package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import java.time.LocalDateTime;

import jakarta.persistence.*;
import lombok.*;

/**
 * Hai vai trò:
 *  - CheDoNguoiNhan = 3: danh sách người nhận cụ thể.
 *  - Mọi chế độ: ghi nhận ai đã đọc (tạo dòng khi người dùng mở thông báo).
 */
@Entity
@Table(name = "NguoiNhanThongBao",
       uniqueConstraints = @UniqueConstraint(columnNames = {"MaThongBao", "MaTaiKhoan"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class NguoiNhanThongBao {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaNguoiNhanThongBao")
	private Integer maNguoiNhanThongBao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaThongBao", nullable = false)
	private ThongBao thongBao;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "MaTaiKhoan", nullable = false)
	private TaiKhoan taiKhoan;

	@Column(name = "NgayGui")
	private LocalDateTime ngayGui;

	@Column(name = "NgayDoc")
	private LocalDateTime ngayDoc;

	/** 0 chưa đọc, 1 đã đọc */
	@Column(name = "TrangThaiDoc", nullable = false)
	private Integer trangThaiDoc = 0;
}
