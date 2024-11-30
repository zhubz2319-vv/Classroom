from fastapi import FastAPI, Request, UploadFile, File, WebSocket, WebSocketDisconnect
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.encoders import jsonable_encoder
from datetime import datetime
import pytz
from bson import ObjectId
import gridfs
from config import *
from util import *

app = FastAPI()
manager = ConnectionManager()

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.post("/login")
async def login(request: LoginRequest) -> LoginResponse:
    username = request.username
    password = request.password
    user = await database[USER_COLLECTION].find_one({"username": username, "password": password})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "Invalid username or password"})
    token = create_jwt_token(username)
    return JSONResponse(content={"status": "success", "message": "Login successful", "token": token})

@app.get("/auth")
async def auth(credentials: HTTPAuthorizationCredentials = Security(security)) -> AuthResponse:
    data = verify_jwt_token(credentials)
    return JSONResponse(content={"status": "success", "message": "Authentication successful", "username": data["sub"]})

@app.post("/refresh")
async def refresh_token(credentials: HTTPAuthorizationCredentials = Security(security)) -> RefreshResponse:
    data = verify_jwt_token(credentials)
    token = create_jwt_token(data)
    return JSONResponse(content={"status": "success", "message": "Token refreshed", "token": token})

@app.post("/register")
async def register(request: RegisterRequest) -> RegisterResponse:
    username = request.username
    password = request.password
    security_answer = request.security_answer
    auth_code = request.auth_code
    nickname = username # default nickname is username
    if auth_code is not None: # then register as admin
        if auth_code != AUTH_TOKEN:
            return JSONResponse(content={"status": "fail", "message": "Invalid auth code"})
        user = await database[USER_COLLECTION].find_one({"username": username})
        if user is not None:
            return JSONResponse(content={"status": "fail", "message": "Username already exists"})
        user = await database[USER_COLLECTION].insert_one({"username": username, "password": password, "security_answer": security_answer, "nickname": nickname, "role": "instructor"})
        token = create_jwt_token(username)
        return JSONResponse(content={"status": "success", "message": "Admin registration successful", "token": token})
    else: # then register as student
        user = await database[USER_COLLECTION].find_one({"username": username})
        if user is not None:
            return JSONResponse(content={"status": "fail", "message": "Username already exists"})
        user = await database[USER_COLLECTION].insert_one({"username": username, "password": password, "security_answer": security_answer, "nickname": nickname, "role": "student"})
        token = create_jwt_token(username)
        return JSONResponse(content={"status": "success", "message": "Student registration successful", "token": token})

@app.get("/get_info")
async def get_info(username: str = None) -> UserInfoResponse:
    if username is None:
        return JSONResponse(content={"status": "fail", "message": "Username not specified"})
    user = await database[USER_COLLECTION].find_one({"username": username})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "User not found"})
    return JSONResponse(content={"status": "success", "message": "Info retrieved", "nickname": user["nickname"], "role": user["role"]})

@app.get("/get_info2")
async def get_info(credentials: HTTPAuthorizationCredentials = Security(security)) -> UserInfoResponse:
    username = verify_jwt_token(credentials)["sub"]
    user = await database[USER_COLLECTION].find_one({"username": username})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "User not found"})
    return JSONResponse(content={"status": "success", "message": "Info retrieved", "nickname": user["nickname"], "role": user["role"]})

@app.post("/change_info")
async def change_info(request: ChangeInfoRequest) -> StandardResponse:
    username = request.username
    old_password = request.old_password
    new_password = request.new_password
    security_answer = request.security_answer
    nickname = request.nickname
    user = await database[USER_COLLECTION].find_one({"username": username})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "User not found"})
    if old_password is not None and new_password is not None:
        if old_password != user["password"]:
            return JSONResponse(content={"status": "fail", "message": "Invalid password"})
        await database[USER_COLLECTION].update_one({"username": username}, {"$set": {"password": new_password}})
        return JSONResponse(content={"status": "success", "message": "Password changed"})
    if old_password is not None and security_answer is not None:
        if old_password != user["password"]:
            return JSONResponse(content={"status": "fail", "message": "Invalid password"})
        await database[USER_COLLECTION].update_one({"username": username}, {"$set": {"security_answer": security_answer}})
        return JSONResponse(content={"status": "success", "message": "Security answer changed"})
    if new_password is not None and security_answer is not None:
        if security_answer != user["security_answer"]:
            return JSONResponse(content={"status": "fail", "message": "Invalid security answer"})
        await database[USER_COLLECTION].update_one({"username": username}, {"$set": {"password": new_password}})
        return JSONResponse(content={"status": "success", "message": "Password changed"})
    if nickname is not None:
        await database[USER_COLLECTION].update_one({"username": username}, {"$set": {"nickname": nickname}})
        return JSONResponse(content={"status": "success", "message": "Nickname changed"})
    return JSONResponse(content={"status": "fail", "message": "Invalid request"})

@app.get("/get_courses")
async def get_courses(request: Request) -> AllCoursesResponse:
    courses = []
    async for course in database[COURSE_COLLECTION].find():
        courses.append({"course_name": course["course_name"], "course_code": course["course_code"], "instructor": course["instructor"], "students": course["students"]})
    return JSONResponse(content={"status": "success", "message": "Courses retrieved", "courses": courses})

@app.get("/get_courseinfo")
async def get_courseinfo(course_code: str = None, section: str = None) -> CourseInfoResponse:
    if course_code is None or section is None:
        return JSONResponse(content={"status": "fail", "message": "Course code or section not specified"})
    infos = []
    async for info in database[COURSEINFO_COLLECTION].find({"course_code": course_code, "section": section}):
        info.pop("_id")
        infos.append(info)
    if len(infos) == 0:
        return JSONResponse(content={"status": "fail", "message": "Course info not found"})
    return JSONResponse(content={"status": "success", "message": "Course info retrieved", "infos": infos})

@app.post("/add_courseinfo")
async def add_courseinfo(request: InfoRequest) -> StandardResponse:
    username = request.username
    course_code = request.course_code
    section = request.section
    title = request.title
    body = request.body
    file_id = request.file_id
    time = datetime.now(tz=pytz.timezone('Asia/Hong_Kong')).strftime("%Y-%m-%d %H:%M:%S")
    course = await database[COURSE_COLLECTION].find_one({"course_code": course_code})
    if course["instructor"] != username:
        return JSONResponse(content={"status": "fail", "message": "Only course instructor can modify course info"})
    info = await database[COURSEINFO_COLLECTION].find_one({"course_code": course_code, "section": section, "title": title})
    if info is not None:
        await database[COURSEINFO_COLLECTION].update_one({"course_code": course_code, "section": section, "title": title}, {"$set": {"body": body, "time": time, "file_id": file_id}})
        return JSONResponse(content={"status": "success", "message": "Course info updated"})
    await database[COURSEINFO_COLLECTION].insert_one({"course_code": course_code, "section": section, "by": username, "time": time, "title": title, "body": body, "file_id": file_id})
    return JSONResponse(content={"status": "success", "message": "Course info added"})

@app.post("/select_course")
async def select_course(request: AddDropRequest) -> StandardResponse:
    course_code = request.course_code
    username = request.username
    action = request.action
    if action is None:
        return JSONResponse(content={"status": "fail", "message": "Action not specified"})
    if action == "add":
        await database[COURSE_COLLECTION].update_one({"course_code": course_code}, {"$addToSet": {"students": username}})
        await database[CHATS_COLLECTION].update_one({"room_code": course_code}, {"$addToSet": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "Course added"})
    elif action == "drop":
        await database[COURSE_COLLECTION].update_one({"course_code": course_code}, {"$pull": {"students": username}})
        await database[CHATS_COLLECTION].update_one({"room_code": course_code}, {"$pull": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "Course dropped"})
    return JSONResponse(content={"status": "fail", "message": "Invalid action"})

@app.get("/activity") # Get all infos for users' courses
async def activity(username: str = None):
    if username is None:
        return JSONResponse(content={"status": "fail", "message": "Username not specified"})
    activities = []
    async for course in database[COURSE_COLLECTION].find({"students": username}):
        course_code = course["course_code"]
        course_name = course["course_name"]
        instructor = course["instructor"]
        async for info in database[COURSEINFO_COLLECTION].find({"course_code": course_code}):
            info.pop("_id")
            info.pop("course_code")
            info.pop("by")
            info.pop("file_id")
            activities.append({"course_name": course_name, "course_code": course_code, "instructor": instructor, "info": info})
    return JSONResponse(content={"status": "success", "message": "Activities retrieved", "activities": activities})

@app.get("/get_chats")
async def get_chats(username: str = None) -> RoomsResponse:
    if username is None:
        return JSONResponse(content={"status": "fail", "message": "Username not specified"})
    rooms = []
    async for chat in database[CHATS_COLLECTION].find({"users": username}):
        rooms.append({"room_id": chat["room_code"], "room_name": chat["room_name"], "owner": chat["owner"]})
    return JSONResponse(content={"status": "success", "message": "Chats retrieved", "rooms": rooms})

@app.post("/create_chat")
async def create_chat(request: NewChatRequest) -> StandardResponse:
    username = request.username
    room_code = request.room_code
    room_name = request.room_name
    chat = await database[CHATS_COLLECTION].find_one({"room_code": room_code})
    if chat is not None:
        return JSONResponse(content={"status": "fail", "message": "Chat already exists"})
    await database[CHATS_COLLECTION].insert_one({"room_code": room_code, "room_name": room_name, "owner": username, "users": [username]})
    return JSONResponse(content={"status": "success", "message": "Chat created"})

@app.get("/get_users")
async def get_users(room_code: str = None) -> RoomUserResponse:
    if room_code is None:
        return JSONResponse(content={"status": "fail", "message": "Room code not specified"})
    users = []
    async for chat in database[CHATS_COLLECTION].find({"room_code": room_code}):
        users = chat["users"]
    if len(users) == 0:
        return JSONResponse(content={"status": "fail", "message": "Chat not found"})
    return JSONResponse(content={"status": "success", "message": "Members retrieved", "users": users})

@app.post("/edit_user")
async def edit_user(request: EditUserRequest) -> StandardResponse:
    username = request.username
    room_code = request.room_code
    action = request.action
    course = await database[COURSE_COLLECTION].find_one({"course_code": room_code})
    if course is not None:
        return JSONResponse(content={"status": "fail", "message": "Cannot edit user in course chat"})
    chat = await database[CHATS_COLLECTION].find_one({"room_code": room_code})
    if chat is None:
        return JSONResponse(content={"status": "fail", "message": "Chat not found"})
    if chat.get("owner", None) == username:
        return JSONResponse(content={"status": "fail", "message": "Owner cannot be removed"})
    if action is None:
        return JSONResponse(content={"status": "fail", "message": "Action not specified"})
    if action == "add":
        await database[CHATS_COLLECTION].update_one({"room_code": room_code}, {"$addToSet": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "User added"})
    elif action == "remove":
        await database[CHATS_COLLECTION].update_one({"room_code": room_code}, {"$pull": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "User removed"})
    return JSONResponse(content={"status": "fail", "message": "Invalid action"})

@app.get("/get_messages")
async def get_messages(room_code: str = None) -> MessageResponse:
    if room_code is None:
        return JSONResponse(content={"status": "fail", "message": "Room code not specified"})
    messages = []
    async for message in database[MESSAGES_COLLECTION].find({"room_code": room_code}):
        messages.append({"sender": message["sender"], "message": message["message"], "time": message["time"], "file_id": message.get("file_id", None)})
    return JSONResponse(content={"status": "success", "message": "Messages retrieved", "messages": messages})

@app.post("/send_message")
async def send_message(request: MessageRequest) -> StandardResponse:
    room_code = request.room_code
    sender = request.sender
    message = request.message
    file_id = request.file_id
    time = datetime.now(tz=pytz.timezone('Asia/Hong_Kong')).strftime("%Y-%m-%d %H:%M:%S")
    await database[MESSAGES_COLLECTION].insert_one({"room_code": room_code, "sender": sender, "message": message, "time": time, "file_id": file_id})
    await manager.broadcast(room_code, {"sender": sender, "message": message, "time": time, "file_id": file_id})
    return JSONResponse(content={"status": "success", "message": "Message sent"})

@app.post("/upload_file")
async def upload_file(file: UploadFile = File(...)) -> UploadFileResponse:
    contents = await file.read()
    file_id = await fs.upload_from_stream(file.filename, contents)
    return JSONResponse(content={"status": "success", "message": "File uploaded", "file_id": str(file_id)})

@app.get("/download_file")
async def download_file(file_id: str = None) -> StreamingResponse:
    if file_id is None:
        return JSONResponse(content={"status": "fail", "message": "File ID not specified"})
    file = await fs.open_download_stream(ObjectId(file_id))
    if file is None:
        return JSONResponse(content={"status": "fail", "message": "File not found"})
    return StreamingResponse(file, media_type=file.content_type)

@app.post("/submit_fcm_token")
async def submit_fcm_token(request: FCMSubmitRequest) -> StandardResponse:
    username = request.username
    token = request.token
    await database[TOKEN_COLLECTION].update_one({"username": username}, {"$set": {"token": token}}, upsert=True)
    return JSONResponse(content={"status": "success", "message": "FCM token submitted"})

@app.websocket("/ws/{room_code}")
async def websocket_endpoint(websocket: WebSocket, room_code: str):
    await manager.connect(room_code, websocket)
    try:
        while True:
            message = await websocket.receive_json()
            time = datetime.now(tz=pytz.timezone('Asia/Hong_Kong')).strftime("%Y-%m-%d %H:%M:%S")
            await database[MESSAGES_COLLECTION].insert_one({"room_code": room_code, "sender": message["sender"], "message": message["message"], "time": time, "file_id": message.get("file_id", None)})
            await manager.broadcast(room_code, {"sender": message["sender"], "message": message["message"], "time": time, "file_id": message.get("file_id", None)})
    except WebSocketDisconnect:
        manager.disconnect(room_code, websocket)