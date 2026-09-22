package vn.TieuLuanChuyenNganhFITHCMUTE.Entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "LoaiDonVi")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class LoaiDonVi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "MaLoaiDonVi")
	private Integer maLoaiDonVi;

	@Column(name = "TenLoaiDonVi", length = 100)
	private String tenLoaiDonVi;

	@Column(name = "MoTa", length = 500)
	private String moTa;

	@Column(name = "TrangThai")
	private Integer trangThai;
}