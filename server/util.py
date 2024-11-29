from config import *

def notify(users, title, body):
    tokens = []
    async for token in database[TOKEN_COLLECTION].find_one({"user_id": {"$in": users}}):
        tokens.append(token["token"])
    FCM.async_notify_multiple_devices(registration_ids=tokens, message_title=title, message_body=body)