var translations = {
    vi: {
        greeting: "Xin chào",
        logout: "Đăng xuất",
        getLocationBtn: "Lấy vị trí của tôi",
        premiumLabel: "⭐ Tài khoản Premium",
        normalLabel: "Tài khoản: Thường —",
        upgradeLabel: "Nâng cấp Premium",
        verifyFaceBtn: "Xác minh tài khoản",
        colId: "ID",
        colUsername: "Username",
        colEmail: "Email",
        colAction: "Hành động",
        colLocation: "Vị trí",
        message: "Nhắn tin",
        noLocation: "Chưa lấy vị trí",
        emptyList: "Chưa có người dùng nào khác trong DB.",
        waitingTitle: "Trình duyệt lạ",
        waitingDesc: "Bạn đang đăng nhập từ 1 trình duyệt mới. Hãy mở tài khoản này trên trình duyệt quen thuộc để xác nhận.",
        waitingStatus: "Đang chờ xác nhận...",
        browserWarning: "Có trình duyệt lạ đang cố đăng nhập vào tài khoản của bạn.",
        approveBtn: "Đồng ý",
        denyBtn: "Từ chối",
        gettingLocation: "Đang lấy vị trí...",
        trustedMsg: "Đã được chấp nhận! Đang chuyển hướng...",
        deniedMsg: "Yêu cầu đã bị từ chối.",
        locationDenied: "Bạn đã chặn quyền truy cập vị trí. Vui lòng bật lại trong cài đặt trình duyệt (biểu tượng khoá cạnh URL) để dùng tính năng này.",
        locationError: "Trình duyệt của bạn không hỗ trợ định vị vị trí.",
        outOfRange: "Ngoài phạm vi 20km"
    },
    en: {
        greeting: "Hello",
        logout: "Log out",
        getLocationBtn: "Get my location",
        premiumLabel: "⭐ Premium account",
        normalLabel: "Account: Free —",
        upgradeLabel: "Upgrade to Premium",
        verifyFaceBtn: "Verify account",
        colId: "ID",
        colUsername: "Username",
        colEmail: "Email",
        colAction: "Action",
        colLocation: "Location",
        message: "Message",
        noLocation: "Location not fetched",
        emptyList: "No other users in the DB yet.",
        waitingTitle: "Unrecognized browser",
        waitingDesc: "You are logging in from a new browser. Please open this account on a trusted browser to confirm.",
        waitingStatus: "Waiting for confirmation...",
        browserWarning: "An unrecognized browser is trying to log into your account.",
        approveBtn: "Approve",
        denyBtn: "Reject",
        gettingLocation: "Getting location...",
        trustedMsg: "Approved! Redirecting...",
        deniedMsg: "Request was denied.",
        locationDenied: "Location access is blocked. Please enable it in browser settings (lock icon near the URL) to use this feature.",
        locationError: "Your browser does not support geolocation.",
        outOfRange: "Out of 20km range"
    },
    zh: {
        greeting: "你好",
        logout: "登出",
        getLocationBtn: "获取我的位置",
        premiumLabel: "⭐ 高级账户",
        normalLabel: "账户：普通 —",
        upgradeLabel: "升级为高级账户",
        verifyFaceBtn: "验证账户",
        colId: "ID",
        colUsername: "用户名",
        colEmail: "邮箱",
        colAction: "操作",
        colLocation: "位置",
        message: "发消息",
        noLocation: "尚未获取位置",
        emptyList: "数据库中还没有其他用户。",
        waitingTitle: "陌生浏览器",
        waitingDesc: "您正在使用新浏览器登录。请在信任的浏览器上打开此账户以确认。",
        waitingStatus: "等待确认中...",
        browserWarning: "有陌生浏览器正在尝试登录您的账户。",
        approveBtn: "同意",
        denyBtn: "拒绝",
        gettingLocation: "正在获取位置...",
        trustedMsg: "已审批！正在跳转...",
        deniedMsg: "请求已被拒绝。",
        locationDenied: "您已拒绝位置访问。请在浏览器设置（URL旁的锁图标）中重新启用。",
        locationError: "您的浏览器不支持地理定位。",
        outOfRange: "超出20公里范围"
    }
};

function applyLanguage(lang) {
    localStorage.setItem('lang', lang);

    document.querySelectorAll('[data-i18n]').forEach(function (el) {
        var key = el.getAttribute('data-i18n');
        if (translations[lang] && translations[lang][key]) {
            el.textContent = translations[lang][key];
        }
    });

    var switcher = document.getElementById('langSwitcher');
    if (switcher) {
        switcher.value = lang;
    }
}

document.addEventListener('DOMContentLoaded', function () {
    var savedLang = localStorage.getItem('lang') || 'vi';
    applyLanguage(savedLang);
});