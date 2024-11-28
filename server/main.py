from fastapi import FastAPI, Request, UploadFile, File
from fastapi.responses import JSONResponse, StreamingResponse
from fastapi.encoders import jsonable_encoder
from datetime import datetime
import pytz
from bson import ObjectId
import gridfs
from config import *

app = FastAPI()

@app.get("/")
async def root():
    return {"message": "Hello World"}

@app.post("/login")
async def login(request: Request):
    data = await request.json()
    username = data["username"]
    password = data["password"]
    user = await database[USER_COLLECTION].find_one({"username": username, "password": password})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "Invalid username or password"})
    return JSONResponse(content={"status": "success", "message": "Login successful", "user_id": str(user["_id"])})

@app.post("/register")
async def register(request: Request):
    data = await request.json()
    username = data["username"]
    password = data["password"]
    email = data["email"]
    auth_code = data.get("auth_code", None)
    if auth_code is not None: # then register as admin
        if auth_code != AUTH_TOKEN:
            return JSONResponse(content={"status": "fail", "message": "Invalid auth code"})
        user = await database[USER_COLLECTION].find_one({"username": username})
        if user is not None:
            return JSONResponse(content={"status": "fail", "message": "Username already exists"})
        user = await database[USER_COLLECTION].insert_one({"username": username, "password": password, "email": email, "role": "admin"})
        return JSONResponse(content={"status": "success", "message": "Admin registration successful", "user_id": str(user.inserted_id)})
    else: # then register as student
        user = await database[USER_COLLECTION].find_one({"username": username})
        if user is not None:
            return JSONResponse(content={"status": "fail", "message": "Username already exists"})
        user = await database[USER_COLLECTION].insert_one({"username": username, "password": password, "email": email, "role": "student"})
        return JSONResponse(content={"status": "success", "message": "Student registration successful", "user_id": str(user.inserted_id)})

@app.post("/change_password")
async def change_password(request: Request):
    data = await request.json()
    username = data["username"]
    old_password = data["old_password"]
    new_password = data["new_password"]
    user = await database[USER_COLLECTION].find_one({"username": username, "password": old_password})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "Invalid username or password"})
    await database[USER_COLLECTION].update_one({"username": username}, {"$set": {"password": new_password}})
    return JSONResponse(content={"status": "success", "message": "Password changed"})

@app.get("/get_role")
async def get_role(request: Request):
    username = request.query_params["username"]
    user = await database[USER_COLLECTION].find_one({"username": username})
    if user is None:
        return JSONResponse(content={"status": "fail", "message": "User not found"})
    return JSONResponse(content={"status": "success", "message": "Role retrieved", "role": user["role"]})

@app.get("/get_courses") # Optionally course_code as a query parameter
async def get_courses(request: Request):
    courses = []
    if "course_code" in request.query_params:
        course_code = request.query_params["course_code"]
        async for course in database[COURSE_COLLECTION].find({"course_code": course_code}):
            courses.append({"course_name": course["course_name"], "course_code": course["course_code"], "instructors": course["instructors"], "students": course["students"]})
        return JSONResponse(content={"status": "success", "message": "Courses retrieved", "courses": courses})
    async for course in database[COURSE_COLLECTION].find():
        courses.append({"course_name": course["course_name"], "course_code": course["course_code"], "instructors": course["instructors"], "students": course["students"]})
    return JSONResponse(content={"status": "success", "message": "Courses retrieved", "courses": courses})

@app.get("/get_courseinfo")
async def get_courseinfo(request: Request):
    course_code = request.query_params["course_code"]
    section = request.query_params["section"]
    infos = []
    async for info in database[COURSEINFO_COLLECTION].find({"course_code": course_code, "section": section}):
        infos.append({"name": info["name"], "description": info["data"], "id": str(info["_id"])})
    if infos is None:
        return JSONResponse(content={"status": "fail", "message": "Course info not found"})
    return JSONResponse(content={"status": "success", "message": f"{section} info retrieved", "course_info": infos})

@app.post("/add_courseinfo")
async def add_courseinfo(request: Request):
    data = await request.json()
    course_code = data["course_code"]
    section = data["section"]
    name = data["name"]
    description = data["description"]
    user = await database[USER_COLLECTION].find_one({"username": username})
    if user["role"] != "admin":
        return JSONResponse(content={"status": "fail", "message": "Only admin can add course info"})
    course = await database[COURSEINFO_COLLECTION].insert_one({"course_code": course_code, "section": section, "name": name, "data": description})
    return JSONResponse(content={"status": "success", "message": "Course info added"})


@app.post("/select_course")
async def select_course(request: Request):
    data = await request.json()
    action = data["action"]
    course_code = data["course_code"]
    username = data["username"]
    if action is None:
        return JSONResponse(content={"status": "fail", "message": "Action not specified"})
    if action == "add":
        await database[COURSEINFO_COLLECTION].update_one({"course_code": course_code}, {"$addToSet": {"users": username}})
        await database[CHATS_COLLECTION].update_one({"room_code": course_code}, {"$addToSet": {"users": username}})
        #await database[COURSESELECT_COLLECTION].insert_one({"course_code": course_code, "username": username})
        return JSONResponse(content={"status": "success", "message": "Course added"})
    elif action == "drop":
        await database[COURSEINFO_COLLECTION].update_one({"course_code": course_code}, {"$pull": {"users": username}})
        await database[CHATS_COLLECTION].update_one({"room_code": course_code}, {"$pull": {"users": username}})
        #await database[COURSESELECT_COLLECTION].delete_one({"course_code": course_code, "username": username})
        return JSONResponse(content={"status": "success", "message": "Course dropped"})
    return JSONResponse(content={"status": "fail", "message": "Invalid action"})

@app.get("/get_chats")
async def get_chats(request: Request):
    username = request.query_params["username"]
    rooms = []
    async for chat in database[CHATS_COLLECTION].find({"users": username}):
        rooms.append({"room_id": chat["room_code"], "room_name": chat["room_name"]})
    return JSONResponse(content={"status": "success", "message": "Chats retrieved", "rooms": rooms})

@app.post("/create_chat")
async def create_chat(request: Request):
    data = await request.json()
    room_code = data["room_code"]
    room_name = data["room_name"]
    username = data["username"]
    role = await database[USER_COLLECTION].find_one({"username": username})["role"]
    if role != "admin":
        return JSONResponse(content={"status": "fail", "message": "Only admin can create chat"})
    chat = await database[CHATS_COLLECTION].insert_one({"room_code": room_code, "room_name": room_name, "users": [username]})
    return JSONResponse(content={"status": "success", "message": "Chat created"})

@app.get("/get_members")
async def get_members(request: Request):
    room_code = request.query_params["room_code"]
    users = []
    async for chat in database[CHATS_COLLECTION].find({"room_code": room_code}):
        users = chat["users"]
    if len(users) == 0:
        return JSONResponse(content={"status": "fail", "message": "Chat not found"})
    return JSONResponse(content={"status": "success", "message": "Members retrieved", "users": users})

@app.post("/edit_member")
async def edit_member(request: Request):
    data = await request.json()
    action = data["action"]
    room_code = data["room_code"]
    username = data["username"]
    if action is None:
        return JSONResponse(content={"status": "fail", "message": "Action not specified"})
    if action == "add":
        await database[CHATS_COLLECTION].update_one({"room_code": room_code}, {"$addToSet": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "Member added"})
    elif action == "drop":
        await database[CHATS_COLLECTION].update_one({"room_code": room_code}, {"$pull": {"users": username}})
        return JSONResponse(content={"status": "success", "message": "Member removed"})
    return JSONResponse(content={"status": "fail", "message": "Invalid action"})

@app.get("/get_messages")
async def get_messages(request: Request):
    room_code = request.query_params["room_code"]
    messages = []
    async for message in database[MESSAGES_COLLECTION].find({"room_code": room_code}):
        messages.append({"sender": message["sender"], "message": message["message"], "time": message["time"]})
    return JSONResponse(content={"status": "success", "message": "Messages retrieved", "messages": messages})

@app.post("/send_message") # TODO: add file upload & fcm notification
async def send_message(request: Request):
    data = await request.json()
    room_code = data["room_code"]
    sender = data["sender"]
    message = data["message"]
    time = datetime.now(tz=pytz.timezone('Asia/Hong_Kong')).strftime("%Y-%m-%d %H:%M:%S")
    await database[MESSAGES_COLLECTION].insert_one({"room_code": room_code, "sender": sender, "message": message, "time": time})
    return JSONResponse(content={"status": "success", "message": "Message sent"})

@app.post("/upload_file")
async def upload_file(file: UploadFile):
    contents = await file.read()
    file_id = await fs.upload_from_stream(file.filename, contents)
    return JSONResponse(content={"status": "success", "message": "File uploaded", "file_id": str(file_id)})

@app.get("/download_file")
async def download_file(file_id: str):
    file = await fs.open_download_stream(ObjectId(file_id))
    if file is None:
        return JSONResponse(content={"status": "fail", "message": "File not found"})
    return StreamingResponse(file, media_type=file.content_type)
