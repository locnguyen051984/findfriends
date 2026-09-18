document.addEventListener('DOMContentLoaded', function () {
    // Kiểm tra trình duyệt hiện tại
    checkBrowserTrust();
    // Kiểm tra các yêu cầu trình duyệt mới đang chờ
    checkPendingBrowserRequests();
    // Lấy danh sách trình duyệt đã tin cậy
    checkTrustedBrowsers();
    // Cứ 10 giây kiểm tra lại yêu cầu trình duyệt mới
    setInterval(checkPendingBrowserRequests, 10000);
    // Cứ 10 giây cập nhật danh sách trình duyệt đã tin cậy
    setInterval(checkTrustedBrowsers, 10000);
});

// KIỂM TRA TRÌNH DUYỆT HIỆN TẠI
function checkBrowserTrust() {
    var token = localStorage.getItem('browserToken');
    // Nếu chưa có token thì tạo token mới
    if (!token) {
        token = crypto.randomUUID();
        localStorage.setItem('browserToken', token);
    }
    fetch(
        '/browser/check?browserToken=' +
        encodeURIComponent(token),
        {
            method: 'POST'
        }
    )
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            // Trình duyệt mới -> đang chờ xác nhận
            if (data.status === 'PENDING') {
                window.location.href =
                    '/browser/waiting?browserToken=' +
                    encodeURIComponent(token);
            }
        })
        .catch(function (error) {
            console.warn(
                'Không kiểm tra được trình duyệt:',
                error
            );
        });
}
// KIỂM TRA CÁC TRÌNH DUYỆT ĐANG CHỜ XÁC NHẬN
function checkPendingBrowserRequests() {
    fetch('/browser/pending')
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            var box =
                document.getElementById(
                    'browserApprovalBox'
                );
            if (!box) {
                return;
            }
            // Không có yêu cầu
            if (data.length === 0) {
                box.style.display = 'none';
                return;
            }
            var lang =
                localStorage.getItem('lang') || 'vi';
            var t =
                translations[lang] ||
                translations.vi;
            var request = data[0];
            box.innerHTML =
                t.browserWarning +
                ' ' +
                '<button onclick="respondBrowser(' +
                request.id +
                ', true)">' +
                t.approveBtn +
                '</button> ' +
                '<button onclick="respondBrowser(' +
                request.id +
                ', false)">' +
                t.denyBtn +
                '</button>';
            box.style.display = 'block';
        })
        .catch(function (error) {
            console.warn(
                'Không kiểm tra được yêu cầu trình duyệt:',
                error
            );
        });
}

// LẤY DANH SÁCH TRÌNH DUYỆT ĐÃ TIN CẬY
function checkTrustedBrowsers() {
    fetch('/browser/trusted')
        .then(function (response) {
            if (!response.ok) {
                throw new Error(
                    'HTTP error: ' + response.status
                );
            }
            return response.json();
        })
        .then(function (data) {
            var box =
                document.getElementById(
                    'trustedBrowsersBox'
                );
            // Trang hiện tại không có khu vực hiển thị
            if (!box) {
                return;
            }
            // Không có browser nào
            if (data.length === 0) {
                box.innerHTML =
                    '<p>Chưa có trình duyệt nào được tin cậy.</p>';
                return;
            }
            var html =
                '<h3>Trình duyệt đã tin cậy</h3>';
            data.forEach(function (browser, index) {
                html +=
                    '<div class="trusted-browser">' +
                    '<p>' +
                    '<b>Trình duyệt ' +
                    (index + 1) +
                    '</b>' +
                    '<br>' +
                    'Trạng thái: ' +
                    '<span>Đã tin cậy</span>' +
                    '</p>' +
                    '</div>';
            });
            box.innerHTML = html;
        })
        .catch(function (error) {
            console.warn(
                'Không lấy được danh sách trình duyệt:',
                error
            );
        });
}

// TRUST / DENY TRÌNH DUYỆT
function respondBrowser(requestId, accept) {
    var url =
        accept
            ? '/browser/approve'
            : '/browser/deny';
    fetch(
        url +
        '?requestId=' +
        encodeURIComponent(requestId),
        {
            method: 'POST'
        }
    )
        .then(function (response) {
            return response.json();
        })
        .then(function (data) {
            // Cập nhật lại danh sách yêu cầu
            checkPendingBrowserRequests();
            // Cập nhật lại danh sách browser trusted
            checkTrustedBrowsers();
        })
        .catch(function (error) {
            console.warn(
                'Không thể xử lý trình duyệt:',
                error
            );
        });
}