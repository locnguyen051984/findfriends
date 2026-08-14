document.addEventListener('DOMContentLoaded', function () {
    checkBrowserTrust();
});

document.addEventListener('DOMContentLoaded', function () {
    loadNearbyDistances();
});

document.addEventListener('DOMContentLoaded', function () {
    checkPendingBrowserRequests();
    setInterval(checkPendingBrowserRequests, 10000);
});

function checkBrowserTrust() {
    var token = localStorage.getItem('browserToken');
    if (!token) {
        token = crypto.randomUUID();
        localStorage.setItem('browserToken', token);
    }

    fetch('/browser/check?browserToken=' + encodeURIComponent(token), {
        method: 'POST'
    })
        .then(function (response) { return response.json(); })
        .then(function (data) {
            if (data.status === 'PENDING') {
                window.location.href = '/browser/waiting?browserToken=' + encodeURIComponent(token);
            }
        })
        .catch(function (error) {
            console.warn('Không kiểm tra được trình duyệt:', error);
        });
}

function checkAndRequestLocation() {
    if (!navigator.geolocation) {
        showLocationWarning('Trình duyệt của bạn không hỗ trợ định vị vị trí.');
        return;
    }

    var btn = document.getElementById('getLocationBtn');
    btn.disabled = true;
    btn.textContent = 'Đang lấy vị trí...';

    if (navigator.permissions && navigator.permissions.query) {
        navigator.permissions.query({ name: 'geolocation' }).then(function (status) {
            handlePermissionStatus(status.state);
        });
    } else {
        requestLocation();
    }
}

function handlePermissionStatus(state) {
    if (state === 'denied') {
        showLocationWarning(
            'Bạn đã chặn quyền truy cập vị trí. Vui lòng bật lại trong cài đặt trình duyệt (biểu tượng khoá cạnh URL) để dùng tính năng này.'
        );
        resetButton();
    } else {
        requestLocation();
    }
}

function requestLocation() {
    navigator.geolocation.getCurrentPosition(
        function (position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;

            fetch('/location/record?latitude=' + lat + '&longitude=' + lon, {
                method: 'POST'
            })
                .then(function (response) { return response.json(); })
                .then(function (data) {
                    if (data.warning) {
                        showLocationWarning(data.warning);
                    }
                    loadNearbyDistances();
                    resetButton();
                })
                .catch(function (error) {
                    console.warn('Không gửi được vị trí:', error);
                    resetButton();
                });
        },
        function (error) {
            console.warn('Không lấy được vị trí:', error.message);

            if (error.code === 1) {
                showLocationWarning(
                    'Bạn đã chặn quyền truy cập vị trí. Vui lòng bật lại trong cài đặt trình duyệt để dùng tính năng này.'
                );
            }
            resetButton();
        }
    );
}

function resetButton() {
    var btn = document.getElementById('getLocationBtn');
    btn.disabled = false;
    btn.textContent = 'Lấy vị trí của tôi';
}

function showLocationWarning(message) {
    var box = document.getElementById('locationWarningBox');
    if (!box) return;

    box.innerHTML = `
        <span>${message}</span>
        <span style="cursor: pointer; font-weight: bold; font-size: 18px; margin-left: 15px;" 
              onclick="this.parentElement.style.display='none';">&times;</span>
    `;

    box.style.display = 'flex';
    box.style.justifyContent = 'space-between';
    box.style.alignItems = 'center';
}

function loadNearbyDistances() {
    fetch('/location/distances')
        .then(function (response) { return response.json(); })
        .then(function (data) {
            data.forEach(function (item) {
                var cell = document.querySelector('.location-cell[data-user-id="' + item.id + '"]');
                if (cell) {
                    cell.textContent = item.distance;
                }
            });
        })
        .catch(function (error) {
            console.warn('Không tải được danh sách vị trí:', error);
        });
}

function checkPendingBrowserRequests() {
    fetch('/browser/pending')
        .then(function (response) { return response.json(); })
        .then(function (data) {
            var box = document.getElementById('browserApprovalBox');
            if (!box) return;

            if (data.length === 0) {
                box.style.display = 'none';
                return;
            }

            var request = data[0];
            box.innerHTML = 'Có trình duyệt lạ đang cố đăng nhập vào tài khoản của bạn. ' +
                '<button onclick="respondBrowser(' + request.id + ', true)">Đồng ý</button> ' +
                '<button onclick="respondBrowser(' + request.id + ', false)">Từ chối</button>';
            box.style.display = 'block';
        })
        .catch(function (error) {
            console.warn('Không kiểm tra được yêu cầu trình duyệt:', error);
        });
}

function respondBrowser(requestId, accept) {
    var url = accept ? '/browser/approve' : '/browser/deny';
    fetch(url + '?requestId=' + requestId, { method: 'POST' })
        .then(function () { checkPendingBrowserRequests(); });
}