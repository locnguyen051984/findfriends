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
      case "CALL_END":
        this.cleanup();
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
      .then(function (stream) {
        self.setupLocalMedia(stream);
      })
      .then(function () {
        return self.peerConnection.createOffer();
      })
      .then(function (offer) {
        return self.peerConnection.setLocalDescription(offer);
      })
      .then(function () {
        self.sendSignal(
          "OFFER",
          JSON.stringify(self.peerConnection.localDescription),
        );
      })
      .catch(function (err) {
        self.onMediaError(err);
      });
  },

  // ---------- INCOMING ----------

  onOfferReceived: function (message) {
    this.callLogId = message.callLogId;
    this.callType = message.callType;
    this.direction = "INCOMING";
    window.pendingOffer = JSON.parse(message.payload);
    UI.showIncomingCallUI(this.callType);
  },

  acceptCall: function () {
    var self = this;
    document.getElementById("incomingCallButtons").style.display = "none";

    this.getMediaStream(this.callType)
      .then(function (stream) {
        self.setupLocalMedia(stream);
      })
      .then(function () {
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
        self.onMediaError(err);
        self.rejectCall();
      });
  },

  rejectCall: function () {
    this.sendSignal("CALL_REJECT", null);
    this.cleanup();
  },

  onAnswerReceived: function (message) {
    var self = this;
    this.callLogId = message.callLogId; // Cập nhật callLogId cho người gọi
    this.peerConnection
      .setRemoteDescription(
        new RTCSessionDescription(JSON.parse(message.payload)),
      )
      .then(function () {
        self.onRemoteDescriptionSet();
      })
      .then(function () {
        UI.showActiveCallUI(self.callType);
      });
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
  },

  setupLocalMedia: function (stream) {
    this.localStream = stream;
    UI.attachLocalStream(stream, this.callType);
    this.createPeerConnection();
    var self = this;
    stream.getTracks().forEach(function (track) {
      self.peerConnection.addTrack(track, stream);
    });
  },

  onRemoteDescriptionSet: function () {
    var self = this;
    this.remoteDescriptionSet = true;
    this.pendingIceCandidates.forEach(function (candidate) {
      self.peerConnection.addIceCandidate(candidate);
    });
    this.pendingIceCandidates = [];
  },

  onIceCandidateReceived: function (message) {
    var candidate = new RTCIceCandidate(JSON.parse(message.payload));
    if (this.peerConnection && this.remoteDescriptionSet) {
      this.peerConnection.addIceCandidate(candidate);
    } else {
      this.pendingIceCandidates.push(candidate);
    }
  },

  getMediaStream: function (type) {
    var constraints =
      type === "VIDEO"
        ? { audio: true, video: true }
        : { audio: true, video: false };
    return navigator.mediaDevices.getUserMedia(constraints);
  },

  onMediaError: function (err) {
    console.warn("Không lấy được media:", err);
    UI.hideCallModal();
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
    UI.hideCallModal();
  },
};

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
