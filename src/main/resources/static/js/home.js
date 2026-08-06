document.addEventListener('DOMContentLoaded', function () {
    loadNearbyDistances();

    if (!navigator.geolocation) {
        showLocationWarning('Trình duyệt của bạn không hỗ trợ định vị vị trí.');
        return;
    }

    // Check trạng thái quyền trước (nếu browser hỗ trợ Permissions API)
    if (navigator.permissions && navigator.permissions.query) {
        navigator.permissions.query({ name: 'geolocation' }).then(function (status) {
            handlePermissionStatus(status.state);

            // Lắng nghe khi user đổi quyền ngay trong lúc đang ở trang (vd: bấm icon khoá trên address bar)
            status.onchange = function () {
                handlePermissionStatus(status.state);
            };
        });
    } else {
        requestLocation();
    }
});

function handlePermissionStatus(state) {
    if (state === 'denied') {
        showLocationWarning(
            'Bạn đã chặn quyền truy cập vị trí. Vui lòng bật lại trong cài đặt trình duyệt (biểu tượng khoá cạnh URL) để dùng tính năng này.'
        );
    } else {
        // 'granted' hoặc 'prompt' -> vẫn gọi để trigger popup xin quyền nếu cần
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
                })
                .catch(function (error) {
                    console.warn('Không gửi được vị trí:', error);
                });
        },
        function (error) {
            console.warn('Không lấy được vị trí:', error.message);

            // code 1 = PERMISSION_DENIED
            if (error.code === 1) {
                showLocationWarning(
                    'Bạn đã chặn quyền truy cập vị trí. Vui lòng bật lại trong cài đặt trình duyệt để dùng tính năng này.'
                );
            }
        }
    );
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