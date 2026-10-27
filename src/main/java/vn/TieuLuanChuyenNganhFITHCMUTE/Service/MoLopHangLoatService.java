package vn.TieuLuanChuyenNganhFITHCMUTE.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.ChuongTrinhDaoTao;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.GiangVien;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocKy;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.HocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LichHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.LopHocPhan;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.Nganh;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhanCongGiangDay;
import vn.TieuLuanChuyenNganhFITHCMUTE.Entity.PhongHoc;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.ChuongTrinhDaoTaoRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.GiangVienRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.HocKyRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LichHocRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.LopHocPhanRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.NganhRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhanCongGiangDayRepository;
import vn.TieuLuanChuyenNganhFITHCMUTE.Repository.PhongHocRepository;

/**
 * Mở lớp chính khóa hàng loạt theo kế hoạch đào tạo.
 *
 * Quy tắc chia lớp bám theo cách trường thật làm, dựa vào cột KhoiKienThuc
 * trong chương trình đào tạo:
 *
 *   - Khối đại cương (1): gom chung toàn trường theo khóa. Anh văn, Triết học,
 *     Toán cao cấp nằm trong chương trình của hầu hết các ngành; tách riêng
 *     từng ngành sẽ ra hàng chục lớp, mỗi lớp lèo tèo vài chục sinh viên,
 *     lãng phí phòng và giảng viên. Lớp loại này để trống cột MaNganh.
 *
 *   - Cơ sở ngành, chuyên ngành, tốt nghiệp (2, 3, 4): tách theo từng ngành,
 *     vì nội dung và giảng viên khác nhau giữa các ngành.
 */
@Service
@RequiredArgsConstructor
public class MoLopHangLoatService {

	public static final int SI_SO_MOI_LOP = 50;
	private static final int SI_SO_TOI_THIEU = 15;
	private static final int SO_TIET_MOI_BUOI = 3;
	/** Các ca học trong ngày: tiết bắt đầu */
	private static final int[] CA_HOC = {1, 4, 7, 10};
	private static final int LOP_CHINH_KHOA = 1;

	private static final int KHOI_DAI_CUONG = 1;
	private static final int HOC_PHAN_TU_CHON = 2;
	private static final int SV_DANG_HOC = 1;

	/**
	 * Tỷ lệ sinh viên thực sự đăng ký một học phần tự chọn.
	 *
	 * Môn bắt buộc thì cả khóa phải học nên mở đủ lớp. Môn tự chọn chỉ một
	 * phần đăng ký; mở đủ như môn bắt buộc sẽ thừa lớp, tới ngày chốt lại
	 * phải hủy vì không đạt sĩ số tối thiểu.
	 */
	private static final double TY_LE_DANG_KY_TU_CHON = 0.4;

	private final EntityManager em;
	private final HocKyRepository hocKyRepository;
	private final NganhRepository nganhRepository;
	private final ChuongTrinhDaoTaoRepository chuongTrinhRepository;
	private final LopHocPhanRepository lopRepository;
	private final PhongHocRepository phongHocRepository;
	private final LichHocRepository lichHocRepository;
	private final GiangVienRepository giangVienRepository;
	private final PhanCongGiangDayRepository phanCongRepository;

	// ===================== KIỂU DỮ LIỆU =====================

	/** Một dòng trong bảng kế hoạch mở lớp */
	@Getter
	public static class DongKeHoach {
		private final String khoa;
		private final int hocKyThu;
		private final HocPhan hocPhan;
		/** Để trống nghĩa là lớp mở chung cho mọi ngành */
		private final Nganh nganh;
		private final boolean tuChon;
		/** Các ngành có sinh viên phải học học phần này */
		private final Set<Integer> nganhLienQuan;

		private long soSinhVien;
		private int soLopCan;
		private int soLopDaCo;

		DongKeHoach(String khoa, int hocKyThu, HocPhan hocPhan, Nganh nganh, boolean tuChon) {
			this.khoa = khoa;
			this.hocKyThu = hocKyThu;
			this.hocPhan = hocPhan;
			this.nganh = nganh;
			this.tuChon = tuChon;
			this.nganhLienQuan = new HashSet<>();
		}

		public boolean isChung() { return nganh == null; }

		public int getSoLopThieu() { return Math.max(0, soLopCan - soLopDaCo); }

		/**
		 * Khoa quản lý ngành của lớp này, null với lớp đại cương mở chung.
		 *
		 * Gọi getMaDonVi() trên proxy lười không làm Hibernate nạp đơn vị,
		 * vì khóa chính đã nằm sẵn trong proxy.
		 */
		public Integer getMaDonViKhoa() {
			if (nganh == null || nganh.getDonVi() == null) return null;
			return nganh.getDonVi().getMaDonVi();
		}

		/** Chữ hiển thị ở cột Phạm vi */
		public String getTenPhamVi() {
			return (nganh == null)
					? "Chung toàn trường · " + nganhLienQuan.size() + " ngành"
					: nganh.getTenNganh();
		}
	}

	@Getter
	public static class KetQuaMoLop {
		private final int soLopDaTao;
		private final List<String> canhBao;

		KetQuaMoLop(int soLopDaTao, List<String> canhBao) {
			this.soLopDaTao = soLopDaTao;
			this.canhBao = canhBao;
		}
	}

	// ===================== PHẠM VI MỞ LỚP =====================

	/**
	 * Giới hạn kế hoạch vào một phần của trường.
	 *
	 * Trước đây màn hình mở lớp đổ cả trường ra một bảng vài trăm dòng, trộn
	 * lẫn môn đại cương với môn của mọi khoa. Người của Phòng Đào tạo phụ
	 * trách một khoa phải tự dò trong đống đó, và bấm "Mở các lớp còn thiếu"
	 * là mở luôn lớp của khoa khác — không ai dám bấm.
	 *
	 * Tách ra thì mỗi lần làm việc chỉ nhìn đúng phần mình phụ trách, và nút
	 * mở lớp chỉ đụng tới đúng phần đang xem.
	 *
	 * Khối đại cương đứng riêng chứ không nằm trong khoa nào, vì lớp đại
	 * cương gom chung toàn trường: nó không thuộc về một khoa cụ thể, và
	 * phải mở một lần cho tất cả thì sĩ số mới đủ.
	 */
	@Getter
	public static class PhamVi {

		public enum Loai { TOAN_TRUONG, DAI_CUONG, KHOA, NGANH }

		private final Loai loai;
		/** Mã khoa khi loại là KHOA, mã ngành khi loại là NGANH */
		private final Integer ma;

		private PhamVi(Loai loai, Integer ma) { this.loai = loai; this.ma = ma; }

		public static PhamVi toanTruong()           { return new PhamVi(Loai.TOAN_TRUONG, null); }
		public static PhamVi daiCuong()             { return new PhamVi(Loai.DAI_CUONG, null); }
		public static PhamVi khoa(Integer maDonVi)  { return new PhamVi(Loai.KHOA, maDonVi); }
		public static PhamVi nganh(Integer maNganh) { return new PhamVi(Loai.NGANH, maNganh); }

		public boolean isDaiCuong() { return loai == Loai.DAI_CUONG; }

		/** Dòng kế hoạch này có thuộc phạm vi đang xét không */
		boolean chapNhan(DongKeHoach d) {
			return switch (loai) {
				case TOAN_TRUONG -> true;
				// Chỉ lớp gom chung, đúng những lớp không thuộc khoa nào
				case DAI_CUONG -> d.isChung();
				// Lớp riêng của các ngành do khoa này quản lý. Lớp đại cương
				// bị loại ra vì đã có trang riêng, để hai nơi không cùng mở
				// một lớp rồi sinh ra lớp trùng.
				case KHOA -> !d.isChung() && ma != null && ma.equals(d.getMaDonViKhoa());
				/*
				 * Chỉ lớp mở riêng cho đúng ngành này.
				 *
				 * Không dùng nganhLienQuan, vì tập đó còn chứa cả lớp đại
				 * cương mà sinh viên ngành này phải học. Lấy theo nó thì màn
				 * hình ngành nào cũng hiện lớp đại cương, và hai ngành cùng
				 * bấm mở sẽ cùng tạo một lớp — đúng thứ việc tách khối đại
				 * cương ra trang riêng muốn tránh.
				 */
				case NGANH -> !d.isChung() && ma != null
						&& ma.equals(d.getNganh().getMaNganh());
			};
		}
	}

	/** Một ô trên màn hình chọn: khối đại cương hoặc một khoa */
	@Getter
	public static class TomTatPhamVi {
		/** Null nghĩa là khối đại cương */
		private final Integer maDonVi;
		private final String ten;
		private final List<String> dsNganh = new ArrayList<>();

		private int soHocPhan;
		private int soLopCan;
		private int soLopDaCo;

		TomTatPhamVi(Integer maDonVi, String ten) { this.maDonVi = maDonVi; this.ten = ten; }

		public boolean isDaiCuong()  { return maDonVi == null; }
		public int getSoLopThieu()   { return Math.max(0, soLopCan - soLopDaCo); }
		public boolean isXong()      { return soHocPhan > 0 && getSoLopThieu() == 0; }
		public int getSoNganh()      { return dsNganh.size(); }
	}

	/** Một ô trên màn hình chọn ngành, bên trong một khoa */
	@Getter
	public static class TomTatNganh {
		private final Integer maNganh;
		private final String ten;

		private int soHocPhan;
		private int soLopCan;
		private int soLopDaCo;

		TomTatNganh(Integer maNganh, String ten) { this.maNganh = maNganh; this.ten = ten; }

		public int getSoLopThieu() { return Math.max(0, soLopCan - soLopDaCo); }
		public boolean isXong()    { return soHocPhan > 0 && getSoLopThieu() == 0; }
	}

	/**
	 * Số liệu cho màn hình chọn ngành của một khoa.
	 *
	 * Một khoa quản lý nhiều ngành, mỗi ngành một khung chương trình riêng
	 * nên danh sách môn phải mở cũng khác nhau. Gộp cả khoa vào một bảng thì
	 * người làm phải tự dò xem dòng nào của ngành nào.
	 *
	 * Chỉ đếm lớp mở riêng theo ngành. Lớp đại cương không thuộc ngành nào,
	 * đã nằm ở trang riêng của khối đại cương.
	 */
	@Transactional(readOnly = true)
	public List<TomTatNganh> tomTatNganh(Integer maHocKy, Integer maDonVi) {
		Map<Integer, TomTatNganh> theoNganh = new LinkedHashMap<>();

		// Liệt kê sẵn mọi ngành của khoa, kể cả ngành kỳ này không có gì phải
		// mở, để người dùng phân biệt "đã xong" với "chưa có dữ liệu".
		for (Nganh n : nganhRepository.findAll(Sort.by("tenNganh"))) {
			if (n.getDonVi() == null || !n.getDonVi().getMaDonVi().equals(maDonVi)) continue;
			theoNganh.put(n.getMaNganh(), new TomTatNganh(n.getMaNganh(), n.getTenNganh()));
		}

		for (DongKeHoach d : keHoach(maHocKy, PhamVi.khoa(maDonVi))) {
			TomTatNganh t = theoNganh.get(d.getNganh().getMaNganh());
			if (t == null) continue;
			t.soHocPhan++;
			t.soLopCan  += d.getSoLopCan();
			t.soLopDaCo += d.getSoLopDaCo();
		}

		return new ArrayList<>(theoNganh.values());
	}

	/**
	 * Số liệu cho màn hình chọn khoa: mỗi khoa còn bao nhiêu lớp phải mở.
	 *
	 * Tính một lượt cho cả trường rồi gom lại, thay vì gọi kế hoạch riêng
	 * cho từng khoa — lớp đại cương phải cộng sinh viên của mọi ngành mới ra
	 * đúng sĩ số, nên chạy từng khoa một sẽ vừa sai vừa chậm.
	 */
	@Transactional(readOnly = true)
	public List<TomTatPhamVi> tomTat(Integer maHocKy) {
		List<DongKeHoach> tatCa = keHoach(maHocKy, PhamVi.toanTruong());

		TomTatPhamVi daiCuong = new TomTatPhamVi(null, "Khối đại cương");
		Map<Integer, TomTatPhamVi> theoKhoa = new LinkedHashMap<>();

		// Liệt kê sẵn mọi khoa có ngành, kể cả khoa không còn gì phải mở.
		// Thiếu bước này thì khoa đã mở xong biến mất khỏi màn hình, người
		// dùng không phân biệt được "đã xong" với "chưa có dữ liệu".
		for (Nganh n : nganhRepository.findAll(Sort.by("tenNganh"))) {
			if (n.getDonVi() == null) continue;
			TomTatPhamVi t = theoKhoa.computeIfAbsent(n.getDonVi().getMaDonVi(),
					k -> new TomTatPhamVi(k, n.getDonVi().getTenDonVi()));
			t.dsNganh.add(n.getTenNganh());
		}

		for (DongKeHoach d : tatCa) {
			TomTatPhamVi t = d.isChung() ? daiCuong : theoKhoa.get(d.getMaDonViKhoa());
			if (t == null) continue;
			t.soHocPhan++;
			t.soLopCan  += d.getSoLopCan();
			t.soLopDaCo += d.getSoLopDaCo();
		}

		List<TomTatPhamVi> kq = new ArrayList<>();
		kq.add(daiCuong);              // đại cương luôn đứng đầu
		kq.addAll(theoKhoa.values());
		return kq;
	}

	// ===================== LẬP KẾ HOẠCH =====================

	@Transactional(readOnly = true)
	public List<DongKeHoach> keHoach(Integer maHocKy, PhamVi phamVi) {
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));

		Integer maNganh = phamVi.getLoai() == PhamVi.Loai.NGANH ? phamVi.getMa() : null;
		if (maNganh != null && nganhRepository.findById(maNganh).isEmpty()) {
			throw new IllegalStateException("Không tìm thấy ngành.");
		}

		Map<Integer, Map<String, Long>> soSv = soSinhVienTheoNganhVaKhoa();
		if (soSv.isEmpty()) {
			throw new IllegalStateException("Chưa có sinh viên đang học nào để lập kế hoạch.");
		}

		/*
		 * Luôn duyệt toàn bộ các ngành, kể cả khi người dùng chỉ chọn một ngành.
		 * Lý do: lớp đại cương gom chung phải cộng đủ sinh viên của mọi ngành
		 * thì sĩ số mới đúng. Lọc theo ngành được chọn chỉ làm ở bước cuối.
		 */
		Map<String, DongKeHoach> gom = new LinkedHashMap<>();

		for (Nganh n : nganhRepository.findAll()) {
			Map<String, Long> theoKhoa = soSv.get(n.getMaNganh());
			if (theoKhoa == null || theoKhoa.isEmpty()) continue;

			List<ChuongTrinhDaoTao> khung = chuongTrinhRepository.findByNganh_MaNganhAndTrangThai(
					n.getMaNganh(), 1, Sort.by("hocKyDuKien", "hocPhan.maHocPhan"));
			if (khung.isEmpty()) continue;

			for (Map.Entry<String, Long> e : theoKhoa.entrySet()) {
				String khoa = e.getKey();
				long soLuong = e.getValue();

				int hocKyThu = hocKyThuCuaKhoa(khoa, hk);
				if (hocKyThu < 1 || hocKyThu > 8) continue;   // đã ra trường hoặc chưa nhập học

				for (ChuongTrinhDaoTao ct : khung) {
					if (ct.getHocKyDuKien() != hocKyThu) continue;

					boolean chung = ct.getKhoiKienThuc() != null
							&& ct.getKhoiKienThuc() == KHOI_DAI_CUONG;
					boolean tuChon = ct.getLoaiHocPhan() != null
							&& ct.getLoaiHocPhan() == HOC_PHAN_TU_CHON;

					String khoaGom = chung
							? "C|" + ct.getHocPhan().getMaHocPhanId() + "|" + khoa
							: "N|" + ct.getHocPhan().getMaHocPhanId() + "|" + khoa + "|" + n.getMaNganh();

					final Nganh nganhCuaLop = chung ? null : n;
					DongKeHoach d = gom.computeIfAbsent(khoaGom, k -> new DongKeHoach(
							khoa, hocKyThu, ct.getHocPhan(), nganhCuaLop, tuChon));

					d.soSinhVien += soLuong;
					d.nganhLienQuan.add(n.getMaNganh());
				}
			}
		}

		// Tính số lớp cần và số lớp đã có
		for (DongKeHoach d : gom.values()) {
			double heSo = d.isTuChon() ? TY_LE_DANG_KY_TU_CHON : 1.0;
			int can = (int) Math.ceil(d.getSoSinhVien() * heSo / SI_SO_MOI_LOP);
			d.soLopCan = (d.getSoSinhVien() > 0) ? Math.max(1, can) : 0;

			d.soLopDaCo = (int) (d.isChung()
					? lopRepository.countByHocKy_MaHocKyAndHocPhan_MaHocPhanIdAndKhoaApDungAndNganhIsNull(
							maHocKy, d.getHocPhan().getMaHocPhanId(), d.getKhoa())
					: lopRepository.countByHocKy_MaHocKyAndHocPhan_MaHocPhanIdAndKhoaApDungAndNganh_MaNganh(
							maHocKy, d.getHocPhan().getMaHocPhanId(), d.getKhoa(),
							d.getNganh().getMaNganh()));
		}

		/*
		 * Lọc ở bước cuối chứ không lọc từ đầu. Vòng lặp trên phải duyệt hết
		 * mọi ngành thì lớp đại cương mới cộng đủ sinh viên toàn trường; cắt
		 * sớm sẽ ra sĩ số thiếu rồi mở hụt lớp.
		 */
		List<DongKeHoach> kq = new ArrayList<>();
		for (DongKeHoach d : gom.values()) {
			if (phamVi.chapNhan(d)) {
				kq.add(d);
			}
		}

		kq.sort(Comparator.comparing(DongKeHoach::getKhoa).reversed()
				.thenComparing(DongKeHoach::getHocKyThu)
				.thenComparing(d -> d.getHocPhan().getMaHocPhan()));
		return kq;
	}

	/** Số sinh viên đang học, gom theo ngành rồi theo khóa, một truy vấn duy nhất */
	private Map<Integer, Map<String, Long>> soSinhVienTheoNganhVaKhoa() {
		List<Object[]> rows = em.createQuery("""
				SELECT sv.nganh.maNganh, sv.khoaTuyenSinh, COUNT(sv) FROM SinhVien sv
				WHERE sv.trangThai = :tt AND sv.khoaTuyenSinh IS NOT NULL
				GROUP BY sv.nganh.maNganh, sv.khoaTuyenSinh
				""", Object[].class)
				.setParameter("tt", SV_DANG_HOC)
				.getResultList();

		Map<Integer, Map<String, Long>> kq = new HashMap<>();
		for (Object[] r : rows) {
			kq.computeIfAbsent((Integer) r[0], k -> new LinkedHashMap<>())
			  .put((String) r[1], (Long) r[2]);
		}
		return kq;
	}

	/** Khóa 2022, học kỳ 1 năm học 2026-2027 → học kỳ thứ 9 */
	private int hocKyThuCuaKhoa(String khoa, HocKy hk) {
		try {
			int namKhoa = Integer.parseInt(khoa.trim());
			int namBatDau = Integer.parseInt(hk.getNamHoc().substring(0, 4));
			int hocKySo = hk.getHocKySo() == null ? 1 : hk.getHocKySo();
			return (namBatDau - namKhoa) * 2 + hocKySo;
		} catch (Exception e) {
			return -1;
		}
	}

	// ===================== THỰC HIỆN =====================

	/**
	 * Chỉ mở những lớp nằm trong phạm vi đang xem.
	 *
	 * Cùng một phạm vi với bảng kế hoạch mà người dùng vừa nhìn, nên bấm nút
	 * mở đúng những dòng đang hiện trên màn hình, không đụng tới khoa khác.
	 */
	@Transactional
	public KetQuaMoLop moLop(Integer maHocKy, PhamVi phamVi, boolean moDangKyNgay) {
		HocKy hk = hocKyRepository.findById(maHocKy)
				.orElseThrow(() -> new IllegalStateException("Không tìm thấy học kỳ."));
		if (hk.getTrangThai() == 2)
			throw new IllegalStateException("Học kỳ đã kết thúc, không mở lớp được.");

		List<DongKeHoach> keHoach = keHoach(maHocKy, phamVi);
		List<PhongHoc> dsPhong = phongHocRepository.findByTrangThaiOrderByMaPhong(1);
		if (dsPhong.isEmpty())
			throw new IllegalStateException("Chưa có phòng học nào đang sử dụng.");

		Map<Integer, Map<String, Long>> soSv = soSinhVienTheoNganhVaKhoa();

		Set<String> phongDaDung = new HashSet<>();
		Set<String> nhomDaBan = new HashSet<>();
		napLichDaXep(maHocKy, soSv, phongDaDung, nhomDaBan);

		// Giờ bận và số lớp đang dạy của từng giảng viên trong học kỳ này
		Set<String> gvBanGio = new HashSet<>();
		Map<Integer, Integer> soLopCuaGv = new HashMap<>();
		napLichGiangVien(maHocKy, gvBanGio, soLopCuaGv);

		List<String> canhBao = new ArrayList<>();
		int soLopDaTao = 0;

		for (DongKeHoach d : keHoach) {
			for (int nhom = d.getSoLopDaCo() + 1; nhom <= d.getSoLopCan(); nhom++) {
				String maLop = sinhMaLop(d, hk, nhom);
				if (lopRepository.existsByMaLop(maLop)) continue;

				int[] slot = timSlot(dsPhong, phongDaDung, nhomDaBan, d);
				if (slot == null) {
					canhBao.add("Hết chỗ trống cho " + d.getHocPhan().getTenHocPhan()
							+ " khóa " + d.getKhoa() + ".");
					continue;
				}
				int thu = slot[0], tietBatDau = slot[1];
				PhongHoc phong = dsPhong.get(slot[2]);

				LopHocPhan lop = new LopHocPhan();
				lop.setMaLop(maLop);
				lop.setHocPhan(d.getHocPhan());
				lop.setHocKy(hk);
				// Để trống ngành nghĩa là lớp mở chung cho mọi ngành
				lop.setNganh(d.getNganh());
				lop.setKhoaApDung(d.getKhoa());
				lop.setLoaiLop(LOP_CHINH_KHOA);
				lop.setSiSoToiDa(SI_SO_MOI_LOP);
				lop.setSiSoToiThieu(SI_SO_TOI_THIEU);
				lop.setTrangThai(moDangKyNgay ? 1 : 0);
				lop.setGhiChu((d.isChung() ? "Lớp đại cương mở chung" : "Lớp riêng của ngành")
						+ ", học kỳ thứ " + d.getHocKyThu()
						+ (d.isTuChon() ? ", học phần tự chọn" : ""));
				lop = lopRepository.save(lop);

				LichHoc lh = new LichHoc();
				lh.setLopHocPhan(lop);
				lh.setThu(thu);
				lh.setTietBatDau(tietBatDau);
				lh.setSoTiet(SO_TIET_MOI_BUOI);
				lh.setPhongHoc(phong);
				lh.setNgayBatDau(hk.getNgayBatDau());
				lh.setNgayKetThuc(hk.getNgayKetThuc());
				lichHocRepository.save(lh);

				// Phân công giảng viên chính: cùng khoa phụ trách học phần, rảnh giờ này, ít lớp nhất
				GiangVien gv = chonGiangVien(d.getHocPhan().getDonVi().getMaDonVi(),
						thu, tietBatDau, gvBanGio, soLopCuaGv);
				if (gv == null) {
					canhBao.add("Chưa có giảng viên rảnh cho lớp " + maLop + ".");
				} else {
					PhanCongGiangDay pc = new PhanCongGiangDay();
					pc.setLopHocPhan(lop);
					pc.setGiangVien(gv);
					pc.setVaiTro(1);
					pc.setTrangThai(1);
					phanCongRepository.save(pc);

					soLopCuaGv.merge(gv.getMaGiangVien(), 1, Integer::sum);
					for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++)
						gvBanGio.add(gv.getMaGiangVien() + "-" + thu + "-" + t);
				}

				// Đánh dấu chỗ đã dùng
				for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++) {
					phongDaDung.add(phong.getMaPhongHoc() + "-" + thu + "-" + t);
					for (Integer maNg : d.getNganhLienQuan())
						nhomDaBan.add(khoaNhom(d.getKhoa(), maNg, thu, t));
				}
				soLopDaTao++;
			}
		}
		return new KetQuaMoLop(soLopDaTao, canhBao);
	}

	private String sinhMaLop(DongKeHoach d, HocKy hk, int nhom) {
		String goc = d.getHocPhan().getMaHocPhan() + "_" + hk.getMaHocKyCode() + "_K" + d.getKhoa();
		String phan = d.isChung() ? "_C" : "_N" + d.getNganh().getMaNganh();
		return goc + phan + "_" + String.format("%02d", nhom);
	}

	// ===================== LỊCH ĐÃ XẾP =====================

	/**
	 * Nạp lại những gì đã xếp trong học kỳ: phòng nào bận giờ nào, và nhóm
	 * sinh viên nào đã có lớp vào giờ nào.
	 *
	 * Bản cũ chỉ nạp lịch phòng, còn lịch của khóa thì để rỗng. Hậu quả là
	 * bấm mở lớp lần thứ hai, hệ thống quên mất khóa đó đã có lớp vào thứ Hai
	 * ca 1 rồi xếp tiếp một lớp nữa vào đúng giờ ấy, sinh viên trùng lịch.
	 */
	private void napLichDaXep(Integer maHocKy, Map<Integer, Map<String, Long>> soSv,
	                          Set<String> phongDaDung, Set<String> nhomDaBan) {

		for (LichHoc lh : lichHocRepository.findByLopHocPhan_HocKy_MaHocKy(maHocKy)) {
			LopHocPhan lop = lh.getLopHocPhan();

			for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++) {
				if (lh.getPhongHoc() != null) {
					phongDaDung.add(lh.getPhongHoc().getMaPhongHoc() + "-" + lh.getThu() + "-" + t);
				}

				String khoa = lop.getKhoaApDung();
				if (khoa == null) continue;

				if (lop.getNganh() != null) {
					nhomDaBan.add(khoaNhom(khoa, lop.getNganh().getMaNganh(), lh.getThu(), t));
				} else {
					// Lớp mở chung chiếm giờ của mọi ngành có sinh viên khóa đó
					for (Map.Entry<Integer, Map<String, Long>> e : soSv.entrySet()) {
						if (e.getValue().containsKey(khoa)) {
							nhomDaBan.add(khoaNhom(khoa, e.getKey(), lh.getThu(), t));
						}
					}
				}
			}
		}
	}

	private void napLichGiangVien(Integer maHocKy, Set<String> gvBanGio,
	                              Map<Integer, Integer> soLopCuaGv) {
		for (PhanCongGiangDay pc : phanCongRepository
				.findByLopHocPhan_HocKy_MaHocKyAndTrangThai(maHocKy, 1)) {
			Integer maGv = pc.getGiangVien().getMaGiangVien();
			soLopCuaGv.merge(maGv, 1, Integer::sum);
			for (LichHoc lh : lichHocRepository.findByLopHocPhan_MaLopHocPhanIn(
					List.of(pc.getLopHocPhan().getMaLopHocPhan()))) {
				for (int t = lh.getTietBatDau(); t <= lh.getTietKetThuc(); t++)
					gvBanGio.add(maGv + "-" + lh.getThu() + "-" + t);
			}
		}
	}

	private String khoaNhom(String khoa, Integer maNganh, int thu, int tiet) {
		return khoa + "|" + maNganh + "|" + thu + "|" + tiet;
	}

	// ===================== CHỌN GIẢNG VIÊN =====================

	/**
	 * Chọn giảng viên cho một lớp: ưu tiên người của khoa phụ trách học phần,
	 * không trùng giờ dạy, đang dạy ít lớp nhất. Hết người trong khoa thì lấy khoa khác.
	 */
	private GiangVien chonGiangVien(Integer maDonVi, int thu, int tietBatDau,
	                                Set<String> gvBanGio, Map<Integer, Integer> soLopCuaGv) {
		List<GiangVien> ungVien =
				giangVienRepository.findByDonVi_MaDonViAndTrangThaiOrderByMaSoGiangVien(maDonVi, 1);
		GiangVien chon = timGiangVienRanh(ungVien, thu, tietBatDau, gvBanGio, soLopCuaGv);
		if (chon != null) return chon;

		// Khoa hết người rảnh: mượn giảng viên khoa khác
		return timGiangVienRanh(giangVienRepository.findAll(), thu, tietBatDau, gvBanGio, soLopCuaGv);
	}

	private GiangVien timGiangVienRanh(List<GiangVien> ungVien, int thu, int tietBatDau,
	                                   Set<String> gvBanGio, Map<Integer, Integer> soLopCuaGv) {
		GiangVien chon = null;
		int itNhat = Integer.MAX_VALUE;

		for (GiangVien gv : ungVien) {
			if (gv.getTrangThai() != null && gv.getTrangThai() != 1) continue;

			boolean ban = false;
			for (int t = tietBatDau; t < tietBatDau + SO_TIET_MOI_BUOI; t++)
				if (gvBanGio.contains(gv.getMaGiangVien() + "-" + thu + "-" + t)) { ban = true; break; }
			if (ban) continue;

			int soLop = soLopCuaGv.getOrDefault(gv.getMaGiangVien(), 0);
			if (soLop < itNhat) { itNhat = soLop; chon = gv; }
		}
		return chon;
	}

	// ===================== TÌM CHỖ TRỐNG =====================

	/**
	 * Tìm chỗ trống đầu tiên: thứ 2 đến thứ 7, bốn ca mỗi ngày, phòng còn rảnh
	 * và mọi nhóm sinh viên liên quan chưa có lớp vào giờ ấy.
	 *
	 * Nhóm ở đây là cặp khóa và ngành, không phải chỉ khóa. Hai ngành khác
	 * nhau của cùng một khóa là hai tập sinh viên khác nhau nên hoàn toàn có
	 * thể học cùng giờ; chặn theo khóa sẽ làm mất rất nhiều chỗ trống.
	 *
	 * @return {thứ, tiết bắt đầu, chỉ số phòng} hoặc null nếu hết chỗ
	 */
	private int[] timSlot(List<PhongHoc> dsPhong, Set<String> phongDaDung,
	                      Set<String> nhomDaBan, DongKeHoach d) {
		for (int thu = 2; thu <= 7; thu++) {
			for (int tiet : CA_HOC) {

				boolean nhomBan = false;
				for (int t = tiet; t < tiet + SO_TIET_MOI_BUOI && !nhomBan; t++) {
					for (Integer maNganh : d.getNganhLienQuan()) {
						if (nhomDaBan.contains(khoaNhom(d.getKhoa(), maNganh, thu, t))) {
							nhomBan = true;
							break;
						}
					}
				}
				if (nhomBan) continue;

				for (int i = 0; i < dsPhong.size(); i++) {
					PhongHoc p = dsPhong.get(i);
					if (p.getSucChua() < SI_SO_MOI_LOP) continue;

					boolean ban = false;
					for (int t = tiet; t < tiet + SO_TIET_MOI_BUOI; t++)
						if (phongDaDung.contains(p.getMaPhongHoc() + "-" + thu + "-" + t)) { ban = true; break; }
					if (!ban) return new int[]{thu, tiet, i};
				}
			}
		}
		return null;
	}
}
