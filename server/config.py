from datetime import datetime, timedelta
from motor.motor_asyncio import AsyncIOMotorClient, AsyncIOMotorGridFSBucket
from pyfcm import FCMNotification

MONGODB = "mongodb+srv://admin:RQgFdHkXJeVhBSHv@cluster0.trh5l.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0" # Connection string
DATABASE = "Main" # Database name
USER_COLLECTION = "user" # User collection name
COURSE_COLLECTION = "course" # Course collection name
COURSEINFO_COLLECTION = "courseinfo" # Course info collection name
COURSESELECT_COLLECTION = "courseselection" # Course select collection name
CHATS_COLLECTION = "chats" # Chats collection name
MESSAGES_COLLECTION = "messages" # Messages collection name
TOKEN_COLLECTION = "token" # Token collection name
AUTH_TOKEN = "HERE IS YOUR AUTH TOKEN" # Auth token
SECRET_KEY = "A-Extremely-Secret-Key" # JWT secret key
ALGORITHM = "HS256" # JWT algorithm

FCM = FCMNotification(service_account_file="./service_account_file.json", project_id="chatapp-3217d")
client = AsyncIOMotorClient(MONGODB)
database = client[DATABASE]
fs = AsyncIOMotorGridFSBucket(database)