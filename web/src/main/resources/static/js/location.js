document.addEventListener('DOMContentLoaded', function () {
    loadNearbyDistances();
});

function checkAndRequestLocation() {
    if (!navigator.geolocation) {
        showLocationWarning('Trình duyệt của bạn không hỗ trợ định vị vị trí.');
        return;
    }

    var btn = document.getElementById('getLocationBtn');
    if (btn) {
        btn.disabled = true;
        btn.textContent = 'Đang lấy vị trí...';
    }

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
    if (btn) {
        btn.disabled = false;
        btn.textContent = '📍 Lấy vị trí của tôi';
    }
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
