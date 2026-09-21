#!/usr/bin/env python3
import os
import sys
import json
import requests
from google.oauth2 import service_account
import google.auth.transport.requests

def send_update_notification(tag_name: str, repo_name: str, release_notes: str = ""):
    service_account_path = os.environ.get("SERVICE_ACCOUNT_PATH", "app/src/main/assets/service-account.json")
    
    if not os.path.exists(service_account_path):
        print(f"❌ Error: No se encontró el archivo de credenciales en {service_account_path}")
        sys.exit(1)
        
    print(f"🔑 Cargando credenciales de Firebase desde: {service_account_path}")
    with open(service_account_path, "r", encoding="utf-8") as f:
        sa_info = json.load(f)
        
    project_id = sa_info.get("project_id")
    if not project_id:
        print("❌ Error: No se encontró 'project_id' en el archivo service-account.json")
        sys.exit(1)

    # Autenticación con OAuth2 de Google para FCM v1
    scopes = ["https://www.googleapis.com/auth/firebase.messaging"]
    credentials = service_account.Credentials.from_service_account_info(sa_info, scopes=scopes)
    auth_req = google.auth.transport.requests.Request()
    credentials.refresh(auth_req)
    access_token = credentials.token

    download_url = f"https://github.com/{repo_name}/releases/download/{tag_name}/app-release.apk"
    
    clean_tag = tag_name if tag_name.startswith("v") else f"v{tag_name}"
    title = "🚀 ¡Nueva actualización disponible!"
    body = f"Se ha publicado la versión {clean_tag}. Toca aquí para actualizar."

    payload = {
        "message": {
            "topic": "diario_app_updates",
            "notification": {
                "title": title,
                "body": body
            },
            "data": {
                "click_type": "update",
                "version": clean_tag,
                "update_url": download_url,
                "title": title,
                "body": body,
                "release_notes": release_notes[:500] if release_notes else ""
            },
            "android": {
                "priority": "HIGH",
                "notification": {
                    "channel_id": "diario_channel",
                    "sound": "default",
                    "notification_priority": "PRIORITY_HIGH"
                }
            }
        }
    }

    url = f"https://fcm.googleapis.com/v1/projects/{project_id}/messages:send"
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json; UTF-8"
    }

    print(f"📡 Enviando notificación FCM v1 para {clean_tag} al topic 'diario_app_updates'...")
    response = requests.post(url, headers=headers, json=payload)

    if response.status_code == 200:
        print("✅ ¡Notificación de actualización enviada con éxito a los dispositivos!")
        print(f"Response: {response.json()}")
    else:
        print(f"❌ Error al enviar notificación FCM ({response.status_code}): {response.text}")
        sys.exit(1)

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Uso: python3 send_update_notification.py <tag_name> <repo_name> [release_notes]")
        sys.exit(1)
        
    tag = sys.argv[1]
    repo = sys.argv[2]
    notes = sys.argv[3] if len(sys.argv) > 3 else ""
    send_update_notification(tag, repo, notes)
