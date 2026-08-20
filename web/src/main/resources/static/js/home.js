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
            if (modal) modal.style.display = 'flex';
            if (verifyMessage) {
                verifyMessage.textContent = '';
                verifyMessage.style.color = 'black';
            }
            startCamera();
        });
    }

    if (btnCloseModal) {
        btnCloseModal.addEventListener('click', function () {
            if (modal) modal.style.display = 'none';
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
                                if (modal) modal.style.display = 'none';
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
                                if (modal) modal.style.display = 'none';
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
                    if (video) video.srcObject = stream;
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
            if (video) video.srcObject = null;
        }
    }
});