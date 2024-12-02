from config import *
from fastapi import WebSocket, WebSocketDisconnect, Security
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Dict, List, Optional
from pydantic import BaseModel
import jwt

async def notify(users, title, body):
    tokens = []
    async for token in database[TOKEN_COLLECTION].find_one({"username": {"$in": users}}):
        tokens.append(token["token"])
    for token in tokens:
        FCM.notify(fcm_token=token, notification_title=title, notification_body=body)

class ConnectionManager:
    def __init__(self):
        self.active_connections: Dict[str, WebSocket] = {}
    
    async def connect(self, room_code: str, websocket: WebSocket):
        await websocket.accept()
        if room_code not in self.active_connections:
            self.active_connections[room_code] = []
        self.active_connections[room_code].append(websocket)
    
    def disconnect(self, room_code: str, websocket: WebSocket):
        if room_code in self.active_connections:
            self.active_connections[room_code].remove(websocket)
            if not self.active_connections[room_code]:
                del self.active_connections[room_code]
    
    async def broadcast(self, room_code: str, message: dict):
        if room_code in self.active_connections:
            for connection in self.active_connections[room_code]:
                await connection.send_json(message)

class LoginRequest(BaseModel):
    username: str
    password: str

class LoginResponse(BaseModel):
    status: str
    message: str
    token: str

class AuthResponse(BaseModel):
    status: str
    message: str
    username: str

class RefreshResponse(BaseModel):
    status: str
    message: str
    new_token: str

class RegisterRequest(BaseModel):
    username: str
    password: str
    security_answer: str
    auth_code: Optional[str] = None

class RegisterResponse(BaseModel):
    status: str
    message: str
    token: str

class UserInfoResponse(BaseModel):
    status: str
    message: str
    nickname: str
    role: str

class Course(BaseModel):
    course_name: str
    course_code: str
    instructor: str
    students: List[str]

class CourseInfo(BaseModel):
    course_code: str
    section: str
    by: str
    time: str
    title: str
    body: str
    file_id: Optional[str] = None

class AllCoursesResponse(BaseModel):
    status: str
    message: str
    courses: List[Course]

class CourseInfoResponse(BaseModel):
    status: str
    message: str
    infos: List[CourseInfo]

class ChangeInfoRequest(BaseModel):
    username: str
    old_password: Optional[str] = None
    new_password: Optional[str] = None
    security_answer: Optional[str] = None
    nickname: Optional[str] = None

class AddDropRequest(BaseModel):
    username: str
    course_code: str
    action: str

class InfoRequest(BaseModel):
    username: str
    course_code: str
    section: str
    title: str
    body: str
    file_id: Optional[str] = None

class Room(BaseModel):
    room_code: str
    room_name: str
    owner: str
    users: List[str]

class RoomsResponse(BaseModel):
    status: str
    message: str
    rooms: List[Room]

class RoomUserResponse(BaseModel):
    status: str
    message: str
    users: List[str]

class NewChatRequest(BaseModel):
    username: str
    room_code: str
    room_name: str

class EditUserRequest(BaseModel):
    room_code: str
    username: str
    action: str

class MessageRequest(BaseModel):
    room_code: str
    username: str
    message: str
    file_id: Optional[str] = None

class Message(BaseModel):
    sender: str
    message: str
    time: str
    file_id: Optional[str] = None

class MessageResponse(BaseModel):
    status: str
    message: str
    messages: List[Message]

class UploadFileResponse(BaseModel):
    status: str
    message: str
    file_id: str

class FileNameResponse(BaseModel):
    status: str
    message: str
    file_name: str

class StandardResponse(BaseModel):
    status: str
    message: str

class FCMSubmitRequest(BaseModel):
    username: str
    token: str

security = HTTPBearer()

def create_jwt_token(username: str) -> str:
    to_be_encoded = {"sub": username, "exp": datetime.utcnow() + timedelta(days=1)}
    return jwt.encode(to_be_encoded, SECRET_KEY, algorithm=ALGORITHM)

def verify_jwt_token(credentials: HTTPAuthorizationCredentials):
    try:
        payload = jwt.decode(credentials.credentials, SECRET_KEY, algorithms=[ALGORITHM])
        return payload
    except:
        raise HTTPException(status_code=401, detail="Invalid token")