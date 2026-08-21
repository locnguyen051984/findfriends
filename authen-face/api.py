import cv2
import numpy as np
from fastapi import FastAPI, File, UploadFile, HTTPException, Form
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
import psycopg2

app = FastAPI(title="Face Verification API")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=False,
    allow_methods=["*"],
    allow_headers=["*"],
)


# Init PostgreSQL DB for face storage
try:
    conn = psycopg2.connect(
        host="localhost",
        port="5432",
        database="findfriends",
        user="postgres",
        password="2005"
    )
    cursor = conn.cursor()
    cursor.execute('''
        CREATE TABLE IF NOT EXISTS face_accounts (
            account_id VARCHAR(255) PRIMARY KEY,
            feature BYTEA
        )
    ''')
    conn.commit()
except Exception as e:
    print("PostgreSQL Connection Error:", e)
    # Handle this gracefully or allow it to crash if db is required


# Load models
detector = cv2.FaceDetectorYN.create(
    "face_detection_yunet.onnx", "", (320, 320),
    score_threshold=0.7
)
recognizer = cv2.FaceRecognizerSF.create(
    "face_recognition_sface.onnx", ""
)

# In-memory storage for the registered face feature
registered_feature = None

def get_face_feature(frame, face_box):
    aligned = recognizer.alignCrop(frame, face_box)
    return recognizer.feature(aligned)

def get_yaw(face):
    """YuNet returns 5 landmarks: left eye, right eye, nose, left mouth, right mouth"""
    left_eye = face[4:6]
    right_eye = face[6:8]
    nose = face[8:10]

    eye_center_x = (left_eye[0] + right_eye[0]) / 2
    eye_dist = abs(right_eye[0] - left_eye[0])
    if eye_dist == 0:
        return 0
    offset = (nose[0] - eye_center_x) / eye_dist
    return offset

async def process_image_file(file: UploadFile):
    contents = await file.read()
    nparr = np.frombuffer(contents, np.uint8)
    frame = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
    if frame is None:
        raise HTTPException(status_code=400, detail="Invalid image file")
    return frame

@app.post("/register")
async def register_face(file: UploadFile = File(...)):
    global registered_feature
    
    frame = await process_image_file(file)
    h, w = frame.shape[:2]
    detector.setInputSize((w, h))
    
    _, faces = detector.detect(frame)
    
    if faces is None or len(faces) == 0:
        raise HTTPException(status_code=400, detail="No face detected")
    
    # Use the first detected face
    face = faces[0]
    registered_feature = get_face_feature(frame, face)
    
    return {"message": "Face registered successfully"}

@app.post("/verify")
async def verify_face(file: UploadFile = File(...), step: str = Form(...)):
    global registered_feature
    
    if registered_feature is None:
        raise HTTPException(status_code=400, detail="No face registered yet. Please call /register first.")
        
    if step not in ["center", "left", "right"]:
        raise HTTPException(status_code=400, detail="Invalid step. Must be 'center', 'left', or 'right'")
        
    frame = await process_image_file(file)
    h, w = frame.shape[:2]
    detector.setInputSize((w, h))
    
    _, faces = detector.detect(frame)
    
    if faces is None or len(faces) == 0:
        return {"match": False, "direction_ok": False, "score": 0.0, "message": "No face detected"}
        
    face = faces[0]
    feature = get_face_feature(frame, face)
    
    # Calculate similarity score
    score = recognizer.match(registered_feature, feature, cv2.FaceRecognizerSF_FR_COSINE)
    is_correct_person = score > 0.363
    
    if not is_correct_person:
        return {
            "match": False,
            "direction_ok": False,
            "score": float(score),
            "message": "Face does not match registered user"
        }
        
    # Check head pose (yaw)
    yaw = get_yaw(face)
    direction_ok = False
    
    if step == "center" and abs(yaw) < 0.15:
        direction_ok = True
    elif step == "left" and yaw < -0.25:
        direction_ok = True
    elif step == "right" and yaw > 0.25:
        direction_ok = True
        
    message = f"OK '{step}'" if direction_ok else f"Please look {step}"
    
    return {
        "match": True,
        "direction_ok": direction_ok,
        "score": float(score),
        "yaw": float(yaw),
        "message": message
    }

@app.post("/register_account")
async def register_account(account_id: str = Form(...), file: UploadFile = File(...)):
    frame = await process_image_file(file)
    h, w = frame.shape[:2]
    detector.setInputSize((w, h))
    
    _, faces = detector.detect(frame)
    
    if faces is None or len(faces) == 0:
        raise HTTPException(status_code=400, detail="No face detected")
    
    # Use the first detected face
    face = faces[0]
    feature = get_face_feature(frame, face)
    
    # Check if face already exists
    cursor.execute("SELECT account_id, feature FROM face_accounts")
    all_accounts = cursor.fetchall()
    
    for row in all_accounts:
        existing_account = row[0]
        # Skip checking against the same account if they are just updating their own face
        if existing_account == account_id:
            continue
            
        saved_feature = np.frombuffer(row[1], dtype=np.float32).reshape(1, 128)
        score = recognizer.match(saved_feature, feature, cv2.FaceRecognizerSF_FR_COSINE)
        
        if score > 0.363:
            # Face matches an existing account
            return {"error": f"Khuôn mặt này đã được đăng ký cho tài khoản khác!"}
    
    # Save to Postgres
    cursor.execute('''
        INSERT INTO face_accounts (account_id, feature) 
        VALUES (%s, %s) 
        ON CONFLICT (account_id) DO UPDATE SET feature = EXCLUDED.feature
    ''', (account_id, feature.tobytes()))
    conn.commit()
    
    return {"message": f"Face registered successfully for account {account_id}"}

@app.post("/verify_account")
async def verify_account(account_id: str = Form(...), step: str = Form(...), file: UploadFile = File(...)):
    cursor.execute("SELECT feature FROM face_accounts WHERE account_id = %s", (account_id,))
    row = cursor.fetchone()
    if not row:
        raise HTTPException(status_code=400, detail="No face registered for this account.")
        
    # feature returned by sface is (1, 128) float32
    # In psycopg2, BYTEA comes back as a memoryview
    saved_feature = np.frombuffer(row[0], dtype=np.float32).reshape(1, 128)
        
    if step not in ["center", "left", "right"]:
        raise HTTPException(status_code=400, detail="Invalid step. Must be 'center', 'left', or 'right'")
        
    frame = await process_image_file(file)
    h, w = frame.shape[:2]
    detector.setInputSize((w, h))
    
    _, faces = detector.detect(frame)
    
    if faces is None or len(faces) == 0:
        return {"match": False, "direction_ok": False, "score": 0.0, "message": "No face detected"}
        
    face = faces[0]
    feature = get_face_feature(frame, face)
    
    # Calculate similarity score
    score = recognizer.match(saved_feature, feature, cv2.FaceRecognizerSF_FR_COSINE)
    is_correct_person = score > 0.363
    
    if not is_correct_person:
        return {
            "match": False,
            "direction_ok": False,
            "score": float(score),
            "message": "Face does not match registered user"
        }
        
    # Check head pose (yaw)
    yaw = get_yaw(face)
    direction_ok = False
    
    if step == "center" and abs(yaw) < 0.15:
        direction_ok = True
    elif step == "left" and yaw < -0.25:
        direction_ok = True
    elif step == "right" and yaw > 0.25:
        direction_ok = True
        
    message = f"OK '{step}'" if direction_ok else f"Please look {step}"
    
    return {
        "match": True,
        "direction_ok": direction_ok,
        "score": float(score),
        "yaw": float(yaw),
        "message": message
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
