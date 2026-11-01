/*
 * Lọc và phân trang tại chỗ cho bảng dữ liệu và lưới thẻ.
 *
 * Dữ liệu các màn hình quản lý đã nằm sẵn trên trang, nên làm ngay trong
 * trình duyệt cho kết quả tức thì, không phải chờ máy chủ trả lời.
 *
 * Lọc và phân trang buộc phải nằm chung một chỗ. Tách ra thì gõ từ khóa sẽ
 * chỉ lọc trong mười dòng của trang đang xem, dòng khớp ở trang sau không
 * bao giờ hiện ra — ô tìm thành vô dụng. Ở đây lọc chạy trước trên toàn bộ
 * dữ liệu, phân trang chạy sau trên phần còn lại.
 *
 * Cách dùng trong template:
 *   <table class="table" data-trang="10">     phân trang 10 dòng mỗi trang
 *   <input data-loc="#bangX" data-trong="#khongThay">   ô tìm
 *   <tr data-bo-qua>   dòng luôn giữ nguyên, ví dụ dòng "không có dữ liệu"
 */
(function () {
    'use strict';

    var SO_DONG_MAC_DINH = 10;  // số dòng mỗi trang
    var SO_NUT_SO = 7;          // số nút trang hiện cùng lúc
    var trangHienTai = new WeakMap();

    /* Bỏ dấu trước khi so sánh: gõ "co khi" vẫn ra "Cơ khí". Thiếu bước này
       ô tìm gần như vô dụng với tiếng Việt, vì lúc tìm nhanh ít ai gõ dấu. */
    function khongDau(chuoi) {
        return (chuoi || '').toLowerCase()
                .normalize('NFD').replace(/[̀-ͯ]/g, '')
                .replace(/đ/g, 'd')
                .replace(/\s+/g, ' ').trim();
    }

    function laBang(vung) { return vung.tagName === 'TABLE'; }

    function chonMuc(vung) {
        return vung.dataset.muc || (laBang(vung) ? 'tbody > tr' : ':scope > *');
    }

    function dsMuc(vung) {
        return Array.prototype.slice.call(vung.querySelectorAll(chonMuc(vung)))
                .filter(function (m) { return !m.hasAttribute('data-bo-qua'); });
    }

    // ===================== THANH PHÂN TRANG =====================

    function thanhTrangCua(vung) {
        // Bảng thường nằm trong .table-responsive; thanh trang phải đặt
        // ngoài khung cuộn đó, không thì nó trôi theo khi cuộn ngang.
        var khung = vung.closest('.table-responsive') || vung;
        var sau = khung.nextElementSibling;
        if (sau && sau.classList.contains('thanh-trang')) return sau;

        var nav = document.createElement('nav');
        nav.className = 'thanh-trang';
        khung.parentNode.insertBefore(nav, khung.nextSibling);
        return nav;
    }

    /** Dãy số trang quanh trang hiện tại, chèn dấu … khi quá dài */
    function daySoTrang(trang, soTrang) {
        if (soTrang <= SO_NUT_SO) {
            var het = [];
            for (var i = 1; i <= soTrang; i++) het.push(i);
            return het;
        }
        var ds = [1];
        var dau = Math.max(2, trang - 2);
        var cuoi = Math.min(soTrang - 1, trang + 2);
        if (dau > 2) ds.push('…');
        for (var j = dau; j <= cuoi; j++) ds.push(j);
        if (cuoi < soTrang - 1) ds.push('…');
        ds.push(soTrang);
        return ds;
    }

    function veThanhTrang(vung, trang, soTrang, tong, soDong) {
        var nav = thanhTrangCua(vung);

        // Một trang thì không cần thanh điều hướng
        if (soTrang <= 1) { nav.hidden = true; nav.innerHTML = ''; return; }
        nav.hidden = false;

        var dau = (trang - 1) * soDong + 1;
        var cuoi = Math.min(trang * soDong, tong);

        var html = '<span class="dem-dong">Hiện ' + dau + '–' + cuoi
                 + ' trong ' + tong + ' dòng</span>'
                 + '<ul class="pagination pagination-sm">';

        html += nut('‹', trang - 1, trang === 1, false);
        daySoTrang(trang, soTrang).forEach(function (s) {
            if (s === '…') html += '<li class="page-item disabled"><span class="page-link">…</span></li>';
            else html += nut(s, s, false, s === trang);
        });
        html += nut('›', trang + 1, trang === soTrang, false);
        html += '</ul>';

        nav.innerHTML = html;

        nav.querySelectorAll('[data-di]').forEach(function (a) {
            a.addEventListener('click', function (e) {
                e.preventDefault();
                trangHienTai.set(vung, parseInt(a.dataset.di, 10));
                capNhat(vung);
            });
        });
    }

    function nut(nhan, di, tat, dangChon) {
        return '<li class="page-item' + (tat ? ' disabled' : '')
             + (dangChon ? ' active' : '') + '">'
             + (tat ? '<span class="page-link">' + nhan + '</span>'
                    : '<a class="page-link" data-di="' + di + '">' + nhan + '</a>')
             + '</li>';
    }

    // ===================== LÕI =====================

    /**
     * Tính lại những mục nào được thấy.
     *
     * Thứ tự quan trọng: lấy phần khớp bộ lọc trước, rồi mới cắt trang trên
     * phần đó. Làm ngược lại thì phân trang chạy trên cả dữ liệu đã bị lọc
     * bỏ, sinh ra những trang trống.
     */
    function capNhat(vung) {
        var muc = dsMuc(vung);
        var khop = muc.filter(function (m) { return m.dataset.khop !== '0'; });

        /* Mặc định 10 dòng mỗi trang cho mọi bảng. Bảng nào đã phân trang
           phía máy chủ thì khai data-trang="0" để tắt, không thì người dùng
           gặp hai thanh phân trang chồng nhau. */
        var khai = vung.dataset.trang;
        var soDong = (khai === undefined || khai === '') ? SO_DONG_MAC_DINH
                                                        : parseInt(khai, 10);
        var coPhanTrang = soDong > 0;
        if (!coPhanTrang) soDong = Math.max(khop.length, 1);

        var soTrang = Math.max(1, Math.ceil(khop.length / soDong));
        var trang = trangHienTai.get(vung) || 1;
        if (trang > soTrang) trang = soTrang;
        if (trang < 1) trang = 1;
        trangHienTai.set(vung, trang);

        muc.forEach(function (m) { m.hidden = true; });

        var batDau = (trang - 1) * soDong;
        khop.slice(batDau, batDau + soDong).forEach(function (m, i) {
            m.hidden = false;
            // Sọc theo thứ tự đang thấy, không theo thứ tự trong HTML
            if (laBang(vung)) m.classList.toggle('dong-chan', i % 2 === 1);
        });

        if (coPhanTrang) veThanhTrang(vung, trang, soTrang, khop.length, soDong);
        return khop.length;
    }

    // ===================== GẮN VÀO TRANG =====================

    function gan() {
        // Mọi bảng đều được sọc; chỉ bảng khai data-trang mới phân trang
        document.querySelectorAll('table.table').forEach(function (bang) {
            if (bang.tBodies.length) capNhat(bang);
        });

        // Lưới thẻ có khai data-trang
        document.querySelectorAll('[data-trang]:not(table)').forEach(capNhat);

        document.querySelectorAll('input[data-loc]').forEach(function (o) {
            var dsVung = document.querySelectorAll(o.dataset.loc);
            var oTrong = o.dataset.trong ? document.querySelector(o.dataset.trong) : null;

            function loc() {
                var tu = khongDau(o.value);
                var tongHien = 0;

                dsVung.forEach(function (vung) {
                    var hien = 0;
                    dsMuc(vung).forEach(function (m) {
                        var khop = (tu === '') || khongDau(m.textContent).indexOf(tu) >= 0;
                        m.dataset.khop = khop ? '1' : '0';
                        if (khop) hien++;

                        /* Dòng nào mở ra một dòng chi tiết thì giấu kèm, nếu
                           không phần chi tiết đang mở sẽ nằm lại một mình
                           sau khi dòng chính đã bị lọc đi. */
                        var dich = m.getAttribute('data-bs-target');
                        if (dich && dich.charAt(0) === '#') {
                            var oChiTiet = document.querySelector(dich);
                            if (oChiTiet) oChiTiet.hidden = !khop;
                        }
                    });

                    // Gõ từ khóa mới thì về trang đầu, không giữ trang cũ
                    trangHienTai.set(vung, 1);
                    tongHien += capNhat(vung);

                    /* Vùng nằm trong một khối riêng (ví dụ mỗi học kỳ một
                       thẻ): lọc hết dòng thì giấu luôn cả khối, để không còn
                       thẻ rỗng chỉ có mỗi tiêu đề. */
                    var khoi = vung.closest('[data-khoi]');
                    if (khoi) khoi.hidden = (tu !== '' && hien === 0);
                });

                if (oTrong) oTrong.hidden = !(tu !== '' && tongHien === 0);
            }

            o.addEventListener('input', loc);

            // Bấm nút Tìm hoặc nhấn Enter: lọc lại chứ không nạp lại trang
            var bieuMau = o.closest('form');
            if (bieuMau) {
                bieuMau.addEventListener('submit', function (e) { e.preventDefault(); loc(); });
            }
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', gan);
    } else {
        gan();
    }
})();
