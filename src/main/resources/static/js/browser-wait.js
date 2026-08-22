document.addEventListener('DOMContentLoaded', function () {
    var browserToken = document.getElementById('waitBrowserToken').value;

    var checkInterval = setInterval(function () {
        fetch('/browser/status?browserToken=' + encodeURIComponent(browserToken))
            .then(function (response) { return response.json(); })
            .then(function (data) {
                var statusText = document.getElementById('statusText');
                var lang = localStorage.getItem('lang') || 'vi';
                var t = (translations[lang] && translations[lang]) ? translations[lang] : translations['vi'];

                if (data.status === 'TRUSTED') {
                    clearInterval(checkInterval);
                    statusText.innerHTML = '<b>' + t.trustedMsg + '</b>';
                    setTimeout(function () { window.location.href = '/home'; }, 1500);
                } else if (data.status === 'DENIED') {
                    clearInterval(checkInterval);
                    statusText.innerHTML = '<b style="color:red;">' + t.deniedMsg + '</b>';
                    setTimeout(function () {
                        window.location.href = '/logout?denied=true';
                    }, 2000);
                }
            })
            .catch(function (error) {
                console.warn('Lỗi kiểm tra trạng thái:', error);
            });
    }, 3000);
});