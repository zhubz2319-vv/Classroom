from config import *

def notify(users, title, body):
    tokens = []
    async for token in database[TOKEN_COLLECTION].find_one({"user_id": {"$in": users}}):
        tokens.append(token["token"])
    for token in tokens:
        FCM.notify(fcm_token=token, notification_title=title, notification_body=body)