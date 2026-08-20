document.addEventListener('DOMContentLoaded', function () {
    checkBrowserTrust();
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

            var lang = localStorage.getItem('lang') || 'vi';
            var t = translations[lang] || translations.vi;

            var request = data[0];
            box.innerHTML = t.browserWarning + ' ' +
                '<button onclick="respondBrowser(' + request.id + ', true)">' + t.approveBtn + '</button> ' +
                '<button onclick="respondBrowser(' + request.id + ', false)">' + t.denyBtn + '</button>';
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