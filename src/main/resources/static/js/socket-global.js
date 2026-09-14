// Shared STOMP connection for call + message realtime, used on every logged-in page.
// Requires `currentUserId` global var to be defined by the page before this script runs.
var GlobalSocket = {
  stompClient: null,
  isConnected: false,

  connect: function () {
    var self = this;
    this.stompClient = new StompJs.Client({
      webSocketFactory: function () {
        return new SockJS("/ws-call");
      },
      reconnectDelay: 5000,
      onConnect: function () {
        self.isConnected = true;
        self.stompClient.subscribe("/user/queue/call", function (message) {
          self.onCallSignal(JSON.parse(message.body));
        });
        self.stompClient.subscribe("/user/queue/message", function (message) {
          self.onMessage(JSON.parse(message.body));
        });

        // Process pending call after connection is established
        // (Handled directly in call-room popup now)
      },
      onStompError: function (frame) {
        console.warn("STOMP error:", frame);
      },
    });
    this.stompClient.activate();
  },

  onCallSignal: function (message) {
    var terminalTypes = ["CALL_TIMEOUT", "CALL_END", "CALL_REJECT", "CALL_BUSY", "CALL_FAILED", "ANSWER"];
    if (terminalTypes.includes(message.type)) {
      IncomingCallModal.hide();
    }

    var isIncomingOffer =
      message.type === "OFFER" &&
      Number(message.fromUserId) !== Number(currentUserId);

    // On message.html, only hand off to CallManager if it's the conversation currently open.
    // Any other page, or an OFFER from someone other than the open conversation, -> modal.
    var isOpenConversation =
      typeof otherUserId !== "undefined" &&
      Number(message.fromUserId) === Number(otherUserId);

    if (typeof CallManager !== "undefined" && (!isIncomingOffer || isOpenConversation)) {
      CallManager.onSignalReceived(message);
      return;
    }

    if (isIncomingOffer) {
      IncomingCallModal.show(message);
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

// ================== INCOMING CALL MODAL ==================
var IncomingCallModal = {
  el: null,

  show: function (message) {
    this.hide();

    var div = document.createElement("div");
    div.id = "incomingCallModal";
    div.style.cssText =
      "position:fixed;top:0;left:0;width:100%;height:100%;background:rgba(0,0,0,0.6);z-index:9999;" +
      "display:flex;justify-content:center;align-items:center;";

    var box = document.createElement("div");
    box.style.cssText =
      "background:#fff;padding:24px 32px;border-radius:12px;text-align:center;min-width:300px;box-shadow:0 10px 25px rgba(0,0,0,0.3);";

    var label = message.callType === "VIDEO" ? "Video 🎥" : "Thoại 📞";
    var title = document.createElement("h3");
    title.style.cssText = "margin-top:0;color:#333;font-size:20px;";
    title.textContent = "Cuộc gọi " + label;

    var text = document.createElement("p");
    text.style.cssText = "font-size:16px;color:#666;margin-bottom:24px;";
    text.textContent = "Người dùng #" + message.fromUserId + " đang gọi cho bạn...";

    var btnWrapper = document.createElement("div");
    btnWrapper.style.cssText = "display:flex;justify-content:center;gap:12px;";

    var acceptBtn = document.createElement("button");
    acceptBtn.textContent = "Chấp nhận";
    acceptBtn.style.cssText = "background:#28a745;color:#fff;border:none;padding:10px 24px;border-radius:20px;cursor:pointer;font-weight:bold;font-size:15px;";
    acceptBtn.onclick = function () {
      localStorage.setItem("pendingOffer_" + message.callLogId, JSON.stringify(message));
      var w = 800;
      var h = 600;
      var left = (screen.width / 2) - (w / 2);
      var top = (screen.height / 2) - (h / 2);
      var url = '/call-room?action=answer&type=' + message.callType + '&callerId=' + message.fromUserId + '&callLogId=' + message.callLogId;
      window.open(url, 'CallWindow_' + message.callLogId, 'width=' + w + ',height=' + h + ',top=' + top + ',left=' + left);
      IncomingCallModal.hide();
    };

    var rejectBtn = document.createElement("button");
    rejectBtn.textContent = "Từ chối";
    rejectBtn.style.cssText = "background:#dc3545;color:#fff;border:none;padding:10px 24px;border-radius:20px;cursor:pointer;font-weight:bold;font-size:15px;";
    rejectBtn.onclick = function () {
      if (GlobalSocket.stompClient && GlobalSocket.isConnected) {
        GlobalSocket.stompClient.publish({
          destination: "/app/call.signal",
          body: JSON.stringify({
            type: "CALL_REJECT",
            callType: message.callType,
            toUserId: message.fromUserId,
            callLogId: message.callLogId
          }),
        });
      }
      IncomingCallModal.hide();
    };

    btnWrapper.appendChild(acceptBtn);
    btnWrapper.appendChild(rejectBtn);

    box.appendChild(title);
    box.appendChild(text);
    box.appendChild(btnWrapper);
    div.appendChild(box);

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
