
function getLang() {
    return localStorage.getItem('lang') || 'vi';
}

function t(key) {
    var lang = getLang();
    return (translations[lang] && translations[lang][key]) ? translations[lang][key] : key;
}

function checkAndRequestLocation() {
    if (!navigator.geolocation) {
        showLocationWarning(t('locationError'));
        return;
    }

    var btn = document.getElementById('getLocationBtn');
    if (btn) {
        btn.disabled = true;
        btn.textContent = t('gettingLocation');
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
        showLocationWarning(t('locationDenied'));
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
                showLocationWarning(t('locationDenied'));
            }
            resetButton();
        }
    );
}

function resetButton() {
    var btn = document.getElementById('getLocationBtn');
    if (btn) {
        btn.disabled = false;
        btn.textContent = t('getLocationBtn');
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
            var nearbyMap = {};
            data.forEach(function (item) {
                nearbyMap[String(item.id)] = item.distance;
            });

            document.querySelectorAll('.location-cell').forEach(function (cell) {
                var row = cell.closest('tr');
                var userId = cell.getAttribute('data-user-id');

                if (nearbyMap[userId] !== undefined) {
                    cell.textContent = nearbyMap[userId];
                    row.style.display = '';
                } else {
                    row.style.display = 'none';
                }
            });
        })
        .catch(function (error) {
            console.warn('Không tải được danh sách vị trí:', error);
        });
}
