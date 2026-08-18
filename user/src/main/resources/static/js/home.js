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

// ---- FACE VERIFICATION LOGIC ----
let videoStream = null;

document.addEventListener('DOMContentLoaded', function () {
    const btnVerifyFace = document.getElementById('btnVerifyFace');
    const modal = document.getElementById('faceModal');
    const btnCloseModal = document.getElementById('btnCloseModal');
    const btnStartVerify = document.getElementById('btnStartVerify');
    const video = document.getElementById('videoElement');
    const verifyMessage = document.getElementById('verifyMessage');

    if (btnVerifyFace) {
        btnVerifyFace.addEventListener('click', function () {
            modal.style.display = 'flex';
            verifyMessage.textContent = '';
            verifyMessage.style.color = 'black';
            startCamera();
        });
    }

    if (btnCloseModal) {
        btnCloseModal.addEventListener('click', function () {
            modal.style.display = 'none';
            stopCamera();
        });
    }
    
    if (btnStartVerify) {
        btnStartVerify.addEventListener('click', function() {
            if (!videoStream) {
                alert("Camera chưa sẵn sàng.");
                return;
            }
            const accountId = btnVerifyFace.getAttribute('data-account-id');
            if (!accountId) {
                alert("Không tìm thấy account ID.");
                return;
            }
            
            verifyMessage.textContent = 'Đang xử lý...';
            verifyMessage.style.color = 'blue';
            
            // Capture frame from video
            const canvas = document.createElement('canvas');
            canvas.width = video.videoWidth;
            canvas.height = video.videoHeight;
            const ctx = canvas.getContext('2d');
            ctx.drawImage(video, 0, 0, canvas.width, canvas.height);
            
            canvas.toBlob(function(blob) {
                if (!blob) {
                    verifyMessage.textContent = 'Lỗi chụp ảnh từ camera.';
                    verifyMessage.style.color = 'red';
                    return;
                }
                
                const formData = new FormData();
                formData.append('account_id', accountId);
                formData.append('file', blob, 'face.jpg');
                formData.append('step', 'center'); // Simple verification requires "center" step for now
                
                // First try to verify
                fetch('http://localhost:8000/verify_account', {
                    method: 'POST',
                    body: formData
                })
                .then(res => res.json().then(data => ({status: res.status, data})))
                .then(result => {
                    if (result.status === 400 && result.data.detail === "No face registered for this account.") {
                        // Not registered, auto register!
                        verifyMessage.textContent = 'Đang đăng ký khuôn mặt mới...';
                        fetch('http://localhost:8000/register_account', {
                            method: 'POST',
                            body: formData
                        })
                        .then(res => res.json())
                        .then(regData => {
                            if (regData.message) {
                                verifyMessage.textContent = 'Đăng ký khuôn mặt thành công!';
                                verifyMessage.style.color = 'green';
                                alert('Đăng ký khuôn mặt mới thành công!');
                                btnVerifyFace.textContent = 'Đã xác minh tài khoản';
                                btnVerifyFace.style.backgroundColor = '#28a745';
                                btnVerifyFace.disabled = true;
                                modal.style.display = 'none';
                                stopCamera();
                            } else if (regData.error) {
                                verifyMessage.textContent = regData.error;
                                verifyMessage.style.color = 'red';
                                alert(regData.error);
                            } else {
                                verifyMessage.textContent = 'Lỗi đăng ký: ' + JSON.stringify(regData);
                                verifyMessage.style.color = 'red';
                                alert('Lỗi đăng ký khuôn mặt!');
                            }
                        })
                        .catch(err => {
                            verifyMessage.textContent = 'Lỗi kết nối server Python khi đăng ký.';
                            verifyMessage.style.color = 'red';
                        });
                    } else if (result.status === 200) {
                        if (result.data.match) {
                            if (result.data.direction_ok) {
                                verifyMessage.textContent = 'Xác minh thành công! (Khớp người dùng)';
                                verifyMessage.style.color = 'green';
                                alert('Xác minh thành công!');
                                btnVerifyFace.textContent = 'Đã xác minh tài khoản';
                                btnVerifyFace.style.backgroundColor = '#28a745';
                                btnVerifyFace.disabled = true;
                                modal.style.display = 'none';
                                stopCamera();
                            } else {
                                verifyMessage.textContent = 'Khuôn mặt đúng nhưng góc không đạt: ' + result.data.message;
                                verifyMessage.style.color = 'orange';
                                alert(verifyMessage.textContent);
                            }
                        } else {
                            verifyMessage.textContent = 'Xác minh thất bại: ' + result.data.message;
                            verifyMessage.style.color = 'red';
                            alert('Xác minh thất bại!');
                        }
                    } else {
                        verifyMessage.textContent = 'Lỗi API: ' + (result.data.detail || JSON.stringify(result.data));
                        verifyMessage.style.color = 'red';
                        alert(verifyMessage.textContent);
                    }
                })
                .catch(err => {
                    console.error(err);
                    verifyMessage.textContent = 'Lỗi kết nối tới server API (http://localhost:8000) hoặc lỗi CORS.';
                    verifyMessage.style.color = 'red';
                    alert(verifyMessage.textContent);
                });
                
            }, 'image/jpeg');
        });
    }

    function startCamera() {
        if (navigator.mediaDevices && navigator.mediaDevices.getUserMedia) {
            navigator.mediaDevices.getUserMedia({ video: true })
                .then(function (stream) {
                    videoStream = stream;
                    video.srcObject = stream;
                })
                .catch(function (err) {
                    console.error("Không thể mở camera: ", err);
                    alert("Không thể mở camera. Vui lòng cấp quyền truy cập webcam.");
                });
        } else {
            alert("Trình duyệt của bạn không hỗ trợ camera.");
        }
    }

    function stopCamera() {
        if (videoStream) {
            let tracks = videoStream.getTracks();
            tracks.forEach(track => track.stop());
            video.srcObject = null;
        }
    }
});