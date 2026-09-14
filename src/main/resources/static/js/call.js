// ================== CONFIG ==================
const ICE_SERVERS = {
  iceServers: [{ urls: "stun:stun.l.google.com:19302" }],
};

// ================== CALL MANAGER ==================
const CallManager = {
  peerConnection: null,
  localStream: null,
  callLogId: null,
  callType: null, // 'VOICE' | 'VIDEO'
  direction: null, // 'OUTGOING' | 'INCOMING'
  micEnabled: true,
  camEnabled: true,
  pendingIceCandidates: [],
  pendingOutgoingIceCandidates: [],
  pendingOffer: null,
  remoteDescriptionSet: false,
  callTimeoutTimer: null,
  iceDisconnectTimer: null,

  sendSignal(type, payload) {
    GlobalSocket.stompClient.publish({
      destination: "/app/call.signal",
      body: JSON.stringify({
        type, toUserId: otherUserId,
        callType: this.callType, callLogId: this.callLogId, payload,
      }),
    });
  },

  onSignalReceived(message) {
    if (Number(message.fromUserId) === Number(currentUserId)) {
      if (message.type === "OFFER") {
        this.callLogId = message.callLogId;
        if (this.pendingOutgoingIceCandidates && this.pendingOutgoingIceCandidates.length > 0) {
          this.pendingOutgoingIceCandidates.forEach(c => this.sendSignal("ICE_CANDIDATE", JSON.stringify(c)));
          this.pendingOutgoingIceCandidates = [];
        }
        return;
      }
      if (message.type === "ANSWER") {
        this.clearTimeoutTimer();
        if (!this.localStream) this.cleanup();
        return;
      }
      // NOTE: Allow CALL_BUSY and CALL_FAILED to echo back to self. 
      // This is a defensive logic to ensure that if the server broadcasts a busy/failed state 
      // to all connected tabs of the current user, this tab will correctly process the end of the call.
      // (Even if the server currently only routes to the peer, keeping this prevents future refactor bugs).
      if (!["CALL_BUSY", "CALL_FAILED"].includes(message.type)) return;
    }

    const handlers = {
      OFFER: () => this.onOfferReceived(message),
      ANSWER: () => this.onAnswerReceived(message),
      ICE_CANDIDATE: () => this.onIceCandidateReceived(message),
      CALL_REJECT: () => this.handleCallEnd("Người dùng đã từ chối cuộc gọi"),
      CALL_BUSY: () => this.handleCallEnd("Người dùng đang bận"),
      CALL_END: () => this.cleanup(),
      CALL_FAILED: () => this.handleCallEnd("Cuộc gọi thất bại do rớt mạng"),
      CALL_TIMEOUT: () => this.handleCallEnd("Cuộc gọi nhỡ"),
    };
    if (handlers[message.type]) handlers[message.type]();
  },

  handleCallEnd(msg) {
    this.cleanup();
    UI.showCallModal(msg);
    setTimeout(() => UI.hideCallModal(), 2000);
  },

  async startCall(type) {
    this.callType = type;
    this.direction = "OUTGOING";
    UI.showCallModal(`Đang gọi ${otherUserName}...`);
    UI.showCallingUI();

    try {
      const stream = await this.getMediaStream(type);
      this.setupLocalMedia(stream);
      const offer = await this.peerConnection.createOffer({ offerToReceiveAudio: true, offerToReceiveVideo: type === "VIDEO" });
      await this.peerConnection.setLocalDescription(offer);
      this.sendSignal("OFFER", JSON.stringify(this.peerConnection.localDescription));
      
      this.callTimeoutTimer = setTimeout(() => {
        this.sendSignal("CALL_TIMEOUT", null);
        this.handleCallEnd("Không ai nhấc máy");
      }, 30000);
    } catch (err) {
      console.error("WebRTC Error (startCall):", err);
      this.onMediaError(err, false);
      this.cleanup();
    }
  },

  onOfferReceived(message) {
    if (this.direction || this.peerConnection) {
      return GlobalSocket.stompClient.publish({
        destination: "/app/call.signal",
        body: JSON.stringify({
          type: "CALL_BUSY", toUserId: message.fromUserId,
          callType: message.callType, callLogId: message.callLogId
        })
      });
    }

    Object.assign(this, { callLogId: message.callLogId, callType: message.callType, direction: "INCOMING" });
    this.pendingOffer = JSON.parse(message.payload);
    UI.showIncomingCallUI(this.callType);
    
    this.callTimeoutTimer = setTimeout(() => {
      this.sendSignal("CALL_TIMEOUT", null);
      this.handleCallEnd("Cuộc gọi nhỡ");
    }, 31000);
  },

  async acceptCall() {
    this.clearTimeoutTimer();
    document.getElementById("incomingCallButtons").style.display = "none";

    try {
      const stream = await this.getMediaStream(this.callType).catch(err => {
        this.onMediaError(err, true);
        return null;
      });
      this.setupLocalMedia(stream);
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(this.pendingOffer));
      this.onRemoteDescriptionSet();
      
      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);
      this.sendSignal("ANSWER", JSON.stringify(this.peerConnection.localDescription));
      UI.showActiveCallUI(this.callType);
    } catch (err) {
      console.error("Lỗi WebRTC (acceptCall):", err);
      this.cleanup();
    }
  },

  rejectCall() {
    this.sendSignal("CALL_REJECT", null);
    this.cleanup();
  },

  async onAnswerReceived(message) {
    this.clearTimeoutTimer();
    if (!this.peerConnection) return;

    this.callLogId = message.callLogId;
    try {
      await this.peerConnection.setRemoteDescription(new RTCSessionDescription(JSON.parse(message.payload)));
      this.onRemoteDescriptionSet();
      UI.showActiveCallUI(this.callType);
    } catch (e) {
      console.error("Invalid ANSWER payload", e);
    }
  },

  endCall() {
    this.sendSignal("CALL_END", null);
    this.cleanup();
  },

  createPeerConnection() {
    this.peerConnection = new RTCPeerConnection(ICE_SERVERS);
    this.peerConnection.onicecandidate = e => {
      if (e.candidate) {
        if (!this.callLogId) {
          this.pendingOutgoingIceCandidates.push(e.candidate);
        } else {
          this.sendSignal("ICE_CANDIDATE", JSON.stringify(e.candidate));
        }
      }
    };
    this.peerConnection.ontrack = e => document.getElementById("remoteVideo").srcObject = e.streams[0];
    this.peerConnection.oniceconnectionstatechange = () => {
      const state = this.peerConnection.iceConnectionState;
      if (state === "disconnected") {
        this.iceDisconnectTimer = setTimeout(() => {
          if (this.peerConnection?.iceConnectionState === "disconnected") {
            this.sendSignal("CALL_FAILED", null);
            this.handleCallEnd("Mất kết nối mạng");
          }
        }, 7000);
      } else if (["connected", "completed"].includes(state)) {
        if (this.iceDisconnectTimer) clearTimeout(this.iceDisconnectTimer);
        this.iceDisconnectTimer = null;
      } else if (state === "failed") {
        this.sendSignal("CALL_FAILED", null);
        this.handleCallEnd("Mất kết nối mạng");
      }
    };
  },

  setupLocalMedia(stream) {
    this.localStream = stream;
    if (stream) UI.attachLocalStream(stream, this.callType);
    this.createPeerConnection();
    if (stream) stream.getTracks().forEach(track => this.peerConnection.addTrack(track, stream));
  },

  onRemoteDescriptionSet() {
    this.remoteDescriptionSet = true;
    this.pendingIceCandidates.forEach(c => this.peerConnection?.addIceCandidate(c).catch(console.error));
    this.pendingIceCandidates = [];
  },

  onIceCandidateReceived(message) {
    if (!message.payload) return;
    try {
      const candidate = new RTCIceCandidate(JSON.parse(message.payload));
      if (this.peerConnection && this.remoteDescriptionSet) this.peerConnection.addIceCandidate(candidate).catch(console.error);
      else this.pendingIceCandidates.push(candidate);
    } catch (e) {
      console.error("Invalid ICE Candidate payload", e);
    }
  },

  getMediaStream(type) {
    return navigator.mediaDevices.getUserMedia({ audio: true, video: type === "VIDEO" });
  },

  onMediaError(err, isIncoming) {
    console.warn("Media error:", err);
    alert(isIncoming ? "Không thể truy cập Camera/Micro. Chế độ chỉ xem/nghe." : "Cấp quyền Camera/Micro để gọi!");
    if (!isIncoming) this.cleanup();
    else document.getElementById("toggleMicBtn").disabled = document.getElementById("toggleCamBtn").disabled = true;
  },

  toggleMedia(isMic) {
    if (!this.localStream) return;
    const prop = isMic ? 'micEnabled' : 'camEnabled';
    this[prop] = !this[prop];
    (isMic ? this.localStream.getAudioTracks() : this.localStream.getVideoTracks()).forEach(t => t.enabled = this[prop]);
    const btn = document.getElementById(isMic ? "toggleMicBtn" : "toggleCamBtn");
    btn.textContent = `${isMic ? '🎤' : '📷'} ${this[prop] ? 'Tắt' : 'Bật'} ${isMic ? 'mic' : 'cam'}`;
  },

  toggleMic() { this.toggleMedia(true); },
  toggleCam() { this.toggleMedia(false); },

  clearTimeoutTimer() {
    if (this.callTimeoutTimer) clearTimeout(this.callTimeoutTimer);
    this.callTimeoutTimer = null;
  },

  cleanup() {
    this.clearTimeoutTimer();
    if (this.iceDisconnectTimer) clearTimeout(this.iceDisconnectTimer);
    this.iceDisconnectTimer = null;
    
    this.peerConnection?.close();
    this.peerConnection = null;
    
    this.localStream?.getTracks().forEach(t => t.stop());
    this.localStream = null;

    Object.assign(this, {
      callLogId: null, callType: null, direction: null,
      pendingIceCandidates: [], pendingOutgoingIceCandidates: [], pendingOffer: null, remoteDescriptionSet: false,
      micEnabled: true, camEnabled: true
    });

    const micBtn = document.getElementById("toggleMicBtn");
    const camBtn = document.getElementById("toggleCamBtn");
    if (micBtn) { micBtn.textContent = "🎤 Tắt mic"; micBtn.disabled = false; }
    if (camBtn) { camBtn.textContent = "📷 Tắt cam"; camBtn.disabled = false; }
    UI.hideCallModal();
  }
};

window.addEventListener("beforeunload", () => CallManager.direction && CallManager.sendSignal("CALL_END", null));

// ================== UI HELPERS ==================
const UI = {
  showCallModal(statusText) {
    document.getElementById("callModal").style.display = "flex";
    document.getElementById("callStatusText").textContent = statusText;
  },
  hideCallModal() {
    document.getElementById("callModal").style.display = "none";
    ["videoContainer", "incomingCallButtons", "activeCallButtons", "callingButtons"]
      .forEach(id => {
        const el = document.getElementById(id);
        if (el) el.style.display = "none";
      });
  },
  showCallingUI() { document.getElementById("callingButtons").style.display = "flex"; },
  attachLocalStream(stream, type) {
    if (type === "VIDEO") {
      document.getElementById("videoContainer").style.display = "block";
      document.getElementById("localVideo").srcObject = stream;
    }
  },
  showIncomingCallUI(callType) {
    this.showCallModal(`${otherUserName} đang gọi ${callType === "VIDEO" ? "video" : "thoại"}...`);
    document.getElementById("incomingCallButtons").style.display = "flex";
  },
  showActiveCallUI(callType) {
    document.getElementById("callStatusText").textContent = `Đang trong cuộc gọi với ${otherUserName}`;
    document.getElementById("activeCallButtons").style.display = "flex";
    document.getElementById("toggleCamBtn").style.display = callType === "VIDEO" ? "inline-flex" : "none";
    document.getElementById("callingButtons").style.display = "none";
  }
};
