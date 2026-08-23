// Shared STOMP connection for call + message realtime, used on every logged-in page.
// Requires `currentUserId` global var to be defined by the page before this script runs.
var GlobalSocket = {
  stompClient: null,

  connect: function () {
    var self = this;
    var socket = new SockJS("/ws-call");
    this.stompClient = new StompJs.Client({
      webSocketFactory: function () {
        return socket;
      },
      onConnect: function () {
        self.stompClient.subscribe("/user/queue/call", function (message) {
          self.onCallSignal(JSON.parse(message.body));
        });
        self.stompClient.subscribe("/user/queue/message", function (message) {
          self.onMessage(JSON.parse(message.body));
        });
      },
      onStompError: function (frame) {
        console.warn("STOMP error:", frame);
      },
    });
    this.stompClient.activate();
  },

  onCallSignal: function (message) {
    var isIncomingOffer =
      message.type === "OFFER" &&
      Number(message.fromUserId) !== Number(currentUserId);

    // On message.html, only hand off to CallManager if it's the conversation currently open.
    // Any other page, or an OFFER from someone other than the open conversation, -> toast.
    var isOpenConversation =
      typeof otherUserId !== "undefined" &&
      Number(message.fromUserId) === Number(otherUserId);

    if (window.CallManager && (!isIncomingOffer || isOpenConversation)) {
      CallManager.onSignalReceived(message);
      return;
    }

    if (isIncomingOffer) {
      CallToast.show(message);
    }
  },

  onMessage: function (message) {
    // Only message.html cares about live chat content
    if (!window.ChatUI) return;
    if (message.type === "CALL") {
      ChatUI.onCallReceived(message);
    } else {
      ChatUI.onMessageReceived(message);
    }
  },
};

// ================== INCOMING CALL TOAST ==================
var CallToast = {
  el: null,

  show: function (message) {
    this.hide();

    var div = document.createElement("div");
    div.id = "callToast";
    div.style.cssText =
      "position:fixed;bottom:20px;right:20px;z-index:9999;" +
      "background:#222;color:#fff;padding:14px 18px;border-radius:8px;" +
      "box-shadow:0 4px 12px rgba(0,0,0,.3);cursor:pointer;font-size:14px;";
    var label = message.callType === "VIDEO" ? "video" : "thoại";
    div.textContent =
      "📞 Người dùng #" +
      message.fromUserId +
      " đang gọi " +
      label +
      "... (bấm để trả lời)";

    div.onclick = function () {
      sessionStorage.setItem("pendingIncomingCall", JSON.stringify(message));
      window.location.href = "/messages/" + message.fromUserId;
    };

    document.body.appendChild(div);
    this.el = div;
  },

  hide: function () {
    if (this.el) {
      this.el.remove();
      this.el = null;
    }
  },
};

document.addEventListener("DOMContentLoaded", function () {
  if (typeof currentUserId !== "undefined" && currentUserId) {
    GlobalSocket.connect();
  }
});
