from config import *
from fastapi import WebSocket, WebSocketDisconnect
from typing import Dict, List

async def notify(users, title, body):
    tokens = []
    async for token in database[TOKEN_COLLECTION].find_one({"user_id": {"$in": users}}):
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