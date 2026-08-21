import cv2
import numpy as np

detector = cv2.FaceDetectorYN.create(
    "face_detection_yunet.onnx", "", (320, 320),
    score_threshold=0.7
)
recognizer = cv2.FaceRecognizerSF.create(
    "face_recognition_sface.onnx", ""
)

def get_face_feature(frame, face_box):
    aligned = recognizer.alignCrop(frame, face_box)
    return recognizer.feature(aligned)

def get_yaw(face):
    """YuNet trả 5 điểm: mắt trái, mắt phải, mũi, khoé miệng trái, khoé miệng phải"""
    left_eye = face[4:6]
    right_eye = face[6:8]
    nose = face[8:10]

    eye_center_x = (left_eye[0] + right_eye[0]) / 2
    eye_dist = abs(right_eye[0] - left_eye[0])
    if eye_dist == 0:
        return 0
    offset = (nose[0] - eye_center_x) / eye_dist
    return offset

cap = cv2.VideoCapture(0)
if not cap.isOpened():
    print("Không mở được camera")
    exit()

w, h = int(cap.get(3)), int(cap.get(4))
detector.setInputSize((w, h))

known_feature = None
mode = "dang_ky"

step_order = ["center", "left", "right"]
steps_done = {"center": False, "left": False, "right": False}
current_step_index = 0

print("Nhấn SPACE để đăng ký khuôn mặt")

while True:
    ret, frame = cap.read()
    if not ret:
        break

    _, faces = detector.detect(frame)
    instruction = "Khong thay mat"
    color = (0, 0, 255)

    key = cv2.waitKey(1) & 0xFF

    if faces is not None:
        face = faces[0]
        box = face[:4].astype(int)
        x, y, fw, fh = box
        cv2.rectangle(frame, (x, y), (x+fw, y+fh), (0, 255, 255), 2)

        if key == ord(' ') and mode == "dang_ky":
            known_feature = get_face_feature(frame, face)
            mode = "xac_thuc"
            print("Đã đăng ký! Bắt đầu xác thực - hãy nhìn thẳng, rồi quay trái, quay phải")

        if mode == "xac_thuc" and known_feature is not None:
            feature = get_face_feature(frame, face)
            score = recognizer.match(known_feature, feature, cv2.FaceRecognizerSF_FR_COSINE)
            is_correct_person = score > 0.363

            yaw = get_yaw(face)
            current_step = step_order[current_step_index] if current_step_index < len(step_order) else None

            direction_ok = False
            if current_step == "center" and abs(yaw) < 0.15:
                direction_ok = True
            elif current_step == "left" and yaw < -0.25:
                direction_ok = True
            elif current_step == "right" and yaw > 0.25:
                direction_ok = True

            if current_step:
                if not is_correct_person:
                    instruction = f"Sai nguoi (score={score:.2f})"
                    color = (0, 0, 255)
                elif direction_ok:
                    steps_done[current_step] = True
                    current_step_index += 1
                    instruction = f"OK '{current_step}'!"
                    color = (0, 255, 0)
                else:
                    huong = {"center": "Nhin thang",
                              "left": "Quay sang TRAI",
                              "right": "Quay sang PHAI"}[current_step]
                    instruction = huong
                    color = (0, 255, 255)
            else:
                instruction = "XAC THUC THANH CONG"
                color = (0, 255, 0)

    if mode == "dang_ky":
        cv2.putText(frame, "Nhan SPACE de dang ky", (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, (255, 255, 0), 2)
    else:
        cv2.putText(frame, instruction, (10, 30),
                    cv2.FONT_HERSHEY_SIMPLEX, 0.7, color, 2)
        cv2.putText(frame, f'Center:{steps_done["center"]} Left:{steps_done["left"]} Right:{steps_done["right"]}',
                    (10, 60), cv2.FONT_HERSHEY_SIMPLEX, 0.55, (255, 255, 255), 1)

    cv2.imshow('Live Verification', frame)

    if key == ord('q'):
        break

cap.release()
cv2.destroyAllWindows()