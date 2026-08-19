import os
import re

replacements = {
    "register": "userRegister",
    "login": "userLogin",
    "verifyAccount": "userVerifyAccount",
    "resetPassword": "userResetPassword",
    "startCall": "userStartCall",
    "markStarted": "userMarkStarted",
    "endCall": "userEndCall",
    "processSignal": "userProcessSignal",
    "haversine": "userHaversine",
    "recordLoginAndCheckAnomaly": "userRecordLoginAndCheckAnomaly",
    "getNearbyList": "userGetNearbyList",
    "sendMessage": "userSendMessage",
    "getConversation": "userGetConversation",
    "buildTimeline": "userBuildTimeline",
    "getPremiumPrice": "userGetPremiumPrice",
    "createPayment": "userCreatePayment",
    "markAsPaidManually": "userMarkAsPaidManually",
    "processWebhook": "userProcessWebhook",
    "getStatus": "userGetStatus",
    "getUserIfPaid": "userGetUserIfPaid",
    "getAllUsers": "userGetAllUsers",
    "getAllUsersByEmail": "userGetAllUsersByEmail"
}

def replace_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    new_content = content
    for old, new in replacements.items():
        new_content = re.sub(r'\b' + old + r'\s*\(', new + '(', new_content)
    
    if new_content != content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        print(f"Updated {filepath}")

user_dir = r"d:\findfriends\user\src\main\java\com\phaithanhcong\user"
for root, dirs, files in os.walk(user_dir):
    for file in files:
        if file.endswith(".java"):
            replace_in_file(os.path.join(root, file))

