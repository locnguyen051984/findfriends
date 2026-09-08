// ================== CONFIG ==================
var ICE_SERVERS = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
};

// ================== CALL MANAGER ==================
var CallManager = {
  // -- webrtc --
  peerConnection: null,
  localStream: null,

  // -- call state --
  callLogId: null,
  callType: null, // 'VOICE' | 'VIDEO'
  direction: null, // 'OUTGOING' | 'INCOMING'

  // -- media control --
  micEnabled: true,
  camEnabled: true,

  // -- ICE buffering --
  pendingIceCandidates: [],
  remoteDescriptionSet: false,

  // -- timeout --
  callTimeoutTimer: null,

  // ---------- SOCKET ----------
  // Connection is shared and managed by socket-global.js (GlobalSocket).
  // This page just calls sendSignal() and receives via GlobalSocket.onCallSignal -> onSignalReceived().

  sendSignal: function (type, payload) {
    GlobalSocket.stompClient.publish({
      destination: "/app/call.signal",
      body: JSON.stringify({
        type: type,
        fromUserId: currentUserId,
        toUserId: otherUserId,
        callType: this.callType,
        callLogId: this.callLogId,
        payload: payload,
      }),
    });
  },

  onSignalReceived: function (message) {
    if (Number(message.fromUserId) === Number(currentUserId)) {
      if (message.type === "OFFER") {
        this.callLogId = message.callLogId;
      }
      return;
    }

    switch (message.type) {
      case "OFFER":
        this.onOfferReceived(message);
        break;
      case "ANSWER":
        this.onAnswerReceived(message);
        break;
      case "ICE_CANDIDATE":
        this.onIceCandidateReceived(message);
        break;
      case "CALL_REJECT":
        this.onCallRejected();
        break;
      case "CALL_BUSY":
        this.cleanup();
        UI.showCallModal("Người dùng đang bận");
        setTimeout(function() { UI.hideCallModal(); }, 2000);
        break;
      case "CALL_END":
        this.cleanup();
        break;
      case "CALL_FAILED":
        this.cleanup();
        UI.showCallModal("Cuộc gọi thất bại do rớt mạng");
        setTimeout(function() { UI.hideCallModal(); }, 2000);
        break;
      case "CALL_TIMEOUT":
        this.cleanup();
        UI.showCallModal("Cuộc gọi nhỡ");
        setTimeout(function() { UI.hideCallModal(); }, 2000);
        break;
    }
  },

  // ---------- OUTGOING ----------

  startCall: function (type) {
    var self = this;
    this.callType = type;
    this.direction = "OUTGOING";
    UI.showCallModal("Đang gọi " + otherUserName + "...");
    UI.showCallingUI();

    this.getMediaStream(type)
      .catch(function(err) {
        self.onMediaError(err, false);
        return null;
      })
      .then(function (stream) {
        self.setupLocalMedia(stream);
        var offerOptions = {
          offerToReceiveAudio: true,
          offerToReceiveVideo: self.callType === "VIDEO"
        };
        return self.peerConnection.createOffer(offerOptions);
      })
      .then(function (offer) {
        return self.peerConnection.setLocalDescription(offer);
      })
      .then(function () {
        self.sendSignal(
          "OFFER",
          JSON.stringify(self.peerConnection.localDescription),
        );
        self.callTimeoutTimer = setTimeout(function () {
          self.sendSignal("CALL_TIMEOUT", null);
          self.cleanup();
          UI.showCallModal("Không ai nhấc máy");
          setTimeout(function() { UI.hideCallModal(); }, 2000);
        }, 30000);
      })
      .catch(function (err) {
        console.error("WebRTC Error (startCall):", err);
        self.cleanup();
      });
  },

  // ---------- INCOMING ----------

  onOfferReceived: function (message) {
    if (this.direction !== null || this.peerConnection !== null) {
      GlobalSocket.stompClient.publish({
        destination: "/app/call.signal",
        body: JSON.stringify({
          type: "CALL_BUSY",
          fromUserId: currentUserId,
          toUserId: message.fromUserId,
          callType: message.callType,
          callLogId: message.callLogId
        })
      });
      return;
    }

    this.callLogId = message.callLogId;
    this.callType = message.callType;
    this.direction = "INCOMING";
    window.pendingOffer = JSON.parse(message.payload);
    UI.showIncomingCallUI(this.callType);
    
    var self = this;
    this.callTimeoutTimer = setTimeout(function () {
      self.sendSignal("CALL_TIMEOUT", null);
      self.cleanup();
      UI.showCallModal("Cuộc gọi nhỡ");
      setTimeout(function() { UI.hideCallModal(); }, 2000);
    }, 31000);
  },

  acceptCall: function () {
    var self = this;
    if (this.callTimeoutTimer) {
      clearTimeout(this.callTimeoutTimer);
      this.callTimeoutTimer = null;
    }
    document.getElementById("incomingCallButtons").style.display = "none";

    this.getMediaStream(this.callType)
      .catch(function (err) {
        self.onMediaError(err, true);
        return null;
      })
      .then(function (stream) {
        self.setupLocalMedia(stream);
        return self.peerConnection.setRemoteDescription(
          new RTCSessionDescription(window.pendingOffer),
        );
      })
      .then(function () {
        self.onRemoteDescriptionSet();
      })
      .then(function () {
        return self.peerConnection.createAnswer();
      })
      .then(function (answer) {
        return self.peerConnection.setLocalDescription(answer);
      })
      .then(function () {
        self.sendSignal(
          "ANSWER",
          JSON.stringify(self.peerConnection.localDescription),
        );
        UI.showActiveCallUI(self.callType);
      })
      .catch(function (err) {
        console.error("Lỗi WebRTC trong quá trình acceptCall:", err);
        self.cleanup();
      });
  },

  rejectCall: function () {
    this.sendSignal("CALL_REJECT", null);
    this.cleanup();
  },

  onAnswerReceived: function (message) {
    if (this.callTimeoutTimer) {
      clearTimeout(this.callTimeoutTimer);
      this.callTimeoutTimer = null;
    }
    if (!this.peerConnection) return;

    var self = this;
    this.callLogId = message.callLogId;
    try {
      var payload = JSON.parse(message.payload);
      this.peerConnection
        .setRemoteDescription(new RTCSessionDescription(payload))
        .then(function () {
          self.onRemoteDescriptionSet();
        })
        .then(function () {
          UI.showActiveCallUI(self.callType);
        })
        .catch(console.error);
    } catch (e) {
      console.error("Invalid ANSWER payload", e);
    }
  },

  onCallRejected: function () {
    this.cleanup();
  },

  endCall: function () {
    this.sendSignal("CALL_END", null);
    this.cleanup();
  },

  // ---------- WEBRTC CORE ----------

  createPeerConnection: function () {
    var self = this;
    this.peerConnection = new RTCPeerConnection(ICE_SERVERS);

    this.peerConnection.onicecandidate = function (event) {
      if (event.candidate) {
        self.sendSignal("ICE_CANDIDATE", JSON.stringify(event.candidate));
      }
    };

    this.peerConnection.ontrack = function (event) {
      document.getElementById("remoteVideo").srcObject = event.streams[0];
    };

    this.peerConnection.oniceconnectionstatechange = function () {
      console.log("ICE Connection State:", self.peerConnection.iceConnectionState);
      if (self.peerConnection.iceConnectionState === "disconnected") {
        self.iceDisconnectTimer = setTimeout(function() {
          if (self.peerConnection && self.peerConnection.iceConnectionState === "disconnected") {
            self.sendSignal("CALL_FAILED", null);
            self.cleanup();
            UI.showCallModal("Mất kết nối mạng");
            setTimeout(function() { UI.hideCallModal(); }, 2000);
          }
        }, 7000); // Đợi 7s xem có tự reconnect không
      } else if (self.peerConnection.iceConnectionState === "connected" || self.peerConnection.iceConnectionState === "completed") {
        if (self.iceDisconnectTimer) {
          clearTimeout(self.iceDisconnectTimer);
          self.iceDisconnectTimer = null;
        }
      } else if (self.peerConnection.iceConnectionState === "failed") {
        self.sendSignal("CALL_FAILED", null);
        self.cleanup();
        UI.showCallModal("Mất kết nối mạng");
        setTimeout(function() { UI.hideCallModal(); }, 2000);
      }
    };
  },

  setupLocalMedia: function (stream) {
    this.localStream = stream;
    if (stream) {
      UI.attachLocalStream(stream, this.callType);
    }
    this.createPeerConnection();
    var self = this;
    if (stream) {
      stream.getTracks().forEach(function (track) {
        self.peerConnection.addTrack(track, stream);
      });
    }
  },

  onRemoteDescriptionSet: function () {
    var self = this;
    this.remoteDescriptionSet = true;
    this.pendingIceCandidates.forEach(function (candidate) {
      if (self.peerConnection) {
        self.peerConnection.addIceCandidate(candidate).catch(console.error);
      }
    });
    this.pendingIceCandidates = [];
  },

  onIceCandidateReceived: function (message) {
    if (!message.payload) return;
    try {
      var candidate = new RTCIceCandidate(JSON.parse(message.payload));
      if (this.peerConnection && this.remoteDescriptionSet) {
        this.peerConnection.addIceCandidate(candidate).catch(console.error);
      } else {
        this.pendingIceCandidates.push(candidate);
      }
    } catch (e) {
      console.error("Invalid ICE Candidate payload", e);
    }
  },

  getMediaStream: function (type) {
    var constraints =
      type === "VIDEO"
        ? { audio: true, video: true }
        : { audio: true, video: false };
    return navigator.mediaDevices.getUserMedia(constraints);
  },

  onMediaError: function (err, isIncoming) {
    console.warn("Không lấy được media:", err);
    alert(isIncoming
      ? "Không thể truy cập Camera/Micro. Bạn sẽ tham gia cuộc gọi với chế độ chỉ xem/nghe."
      : "Vui lòng cấp quyền Camera/Micro trên trình duyệt để bắt đầu cuộc gọi!");

    if (!isIncoming) {
      this.cleanup();
    } else {
      document.getElementById("toggleMicBtn").disabled = true;
      document.getElementById("toggleCamBtn").disabled = true;
    }
  },

  // ---------- MIC / CAM CONTROL ----------

  toggleMic: function () {
    if (!this.localStream) return;
    this.micEnabled = !this.micEnabled;
    this.localStream.getAudioTracks().forEach(
      function (track) {
        track.enabled = this.micEnabled;
      }.bind(this),
    );
    document.getElementById("toggleMicBtn").textContent = this.micEnabled
      ? "🎤 Tắt mic"
      : "🎤 Bật mic";
  },

  toggleCam: function () {
    if (!this.localStream) return;
    this.camEnabled = !this.camEnabled;
    this.localStream.getVideoTracks().forEach(
      function (track) {
        track.enabled = this.camEnabled;
      }.bind(this),
    );
    document.getElementById("toggleCamBtn").textContent = this.camEnabled
      ? "📷 Tắt cam"
      : "📷 Bật cam";
  },

  // ---------- CLEANUP ----------

  cleanup: function () {
    if (this.callTimeoutTimer) {
      clearTimeout(this.callTimeoutTimer);
      this.callTimeoutTimer = null;
    }
    if (this.iceDisconnectTimer) {
      clearTimeout(this.iceDisconnectTimer);
      this.iceDisconnectTimer = null;
    }
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
    }
    if (this.localStream) {
      this.localStream.getTracks().forEach(function (track) {
        track.stop();
      });
      this.localStream = null;
    }

    this.callLogId = null;
    this.callType = null;
    this.direction = null;
    this.pendingIceCandidates = [];
    this.remoteDescriptionSet = false;
    this.micEnabled = true;
    this.camEnabled = true;

    document.getElementById("toggleMicBtn").textContent = "🎤 Tắt mic";
    document.getElementById("toggleCamBtn").textContent = "📷 Tắt cam";
    document.getElementById("toggleMicBtn").disabled = false;
    document.getElementById("toggleCamBtn").disabled = false;
    UI.hideCallModal();
  },
};

window.addEventListener("beforeunload", function() {
  if (CallManager.direction !== null) {
    CallManager.sendSignal("CALL_END", null);
  }
});

// ================== UI HELPERS ==================
var UI = {
  showCallModal: function (statusText) {
    document.getElementById("callModal").style.display = "flex";
    document.getElementById("callStatusText").textContent = statusText;
  },

  hideCallModal: function () {
    document.getElementById("callModal").style.display = "none";
    document.getElementById("videoContainer").style.display = "none";
    document.getElementById("incomingCallButtons").style.display = "none";
    document.getElementById("activeCallButtons").style.display = "none";
    document.getElementById("callingButtons").style.display = "none";
  },

  showVideoContainer: function () {
    document.getElementById("videoContainer").style.display = "block";
  },
  showCallingUI: function () {
    document.getElementById("callingButtons").style.display = "block";
  },

  attachLocalStream: function (stream, type) {
    if (type === "VIDEO") {
      this.showVideoContainer();
      document.getElementById("localVideo").srcObject = stream;
    }
  },

  showIncomingCallUI: function (callType) {
    var label = callType === "VIDEO" ? "video" : "thoại";
    this.showCallModal(otherUserName + " đang gọi " + label + "...");
    document.getElementById("incomingCallButtons").style.display = "block";
  },

  showActiveCallUI: function (callType) {
    document.getElementById("callStatusText").textContent =
      "Đang trong cuộc gọi với " + otherUserName;
    document.getElementById("activeCallButtons").style.display = "block";
    document.getElementById("toggleCamBtn").style.display =
      callType === "VIDEO" ? "inline-block" : "none";
    document.getElementById("callingButtons").style.display = "none";
  },
};
