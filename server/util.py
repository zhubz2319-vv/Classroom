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
    FCM.async_notify_multiple_devices(registration_ids=tokens, message_title=title, message_body=body)

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

class RegisterRequest(BaseModel):
    username: str
    password: str
    security_answer: str
    auth_code: Optional[str] = None

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