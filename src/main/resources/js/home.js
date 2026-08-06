document.addEventListener('DOMContentLoaded', function () {
    loadNearbyDistances();

    if (!navigator.geolocation) {
        return;
    }

    navigator.geolocation.getCurrentPosition(
        function (position) {
            var lat = position.coords.latitude;
            var lon = position.coords.longitude;

            fetch('/location/record?latitude=' + lat + '&longitude=' + lon, {
                method: 'POST'
            })
                .then(function (response) {
                    return response.json();
                })
                .then(function (data) {
                    if (data.warning) {
                        var box = document.getElementById('locationWarningBox');
                        
                        box.innerHTML = `
                            <span>${data.warning}</span>
                            <span style="cursor: pointer; font-weight: bold; font-size: 18px; margin-left: 15px;" 
                                  onclick="this.parentElement.style.display='none';">&times;</span>
                        `;
                        
                        box.style.display = 'flex';
                        box.style.justifyContent = 'space-between';
                        box.style.alignItems = 'center';
                    }
                })
                .catch(function (error) {
                    console.warn('Không gửi được vị trí:', error);
                });
        },
        function (error) {
            console.warn('Không lấy được vị trí:', error.message);
        }
    );
});

function loadNearbyDistances() {
    fetch('/location/distances')
        .then(function (response) {
            return response.json();
        })
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