document.addEventListener('DOMContentLoaded', function () {
    var browserToken = document.getElementById('waitBrowserToken').value;

    var checkInterval = setInterval(function () {
        fetch('/browser/status?browserToken=' + encodeURIComponent(browserToken))
            .then(function (response) { return response.json(); })
            .then(function (data) {
                var statusText = document.getElementById('statusText');

                if (data.status === 'TRUSTED') {
                    clearInterval(checkInterval);
                    statusText.innerHTML = '<b>Đã được chấp nhận! Đang chuyển hướng...</b>';
                    setTimeout(function () { window.location.href = '/home'; }, 1500);
                } else if (data.status === 'DENIED') {
                    clearInterval(checkInterval);
                    statusText.innerHTML = '<b style="color:red;">Yêu cầu đã bị từ chối.</b>';
                }
            })
            .catch(function (error) {
                console.warn('Lỗi kiểm tra trạng thái:', error);
            });
    }, 3000);
});