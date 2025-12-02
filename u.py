import os
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from googleapiclient.discovery import build
from googleapiclient.http import MediaFileUpload

# If modifying scopes, delete the file token.json
SCOPES = ['https://www.googleapis.com/auth/drive.file']

def authenticate():
    creds = None
    if os.path.exists('token.json'):
        creds = Credentials.from_authorized_user_file('token.json', SCOPES)
    if not creds or not creds.valid:
        if creds and creds.expired and creds.refresh_token:
            creds.refresh(Request())
        else:
            flow = InstalledAppFlow.from_client_secrets_file(
                'credentials.json', SCOPES)
            creds = flow.run_local_server(port=0)
        with open('token.json', 'w') as token:
            token.write(creds.to_json())
    return creds

def upload_file(file_path, drive_folder_id='1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'):
    drive_folder_id='1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'
    creds = authenticate()
    service = build('drive', 'v3', credentials=creds)
    
    file_metadata = {'name': os.path.basename(file_path)}
    if drive_folder_id:
        file_metadata['parents'] = [drive_folder_id]

    media = MediaFileUpload(file_path, resumable=True)
    file = service.files().create(
        body=file_metadata,
        media_body=media,
        fields='id'
    ).execute()
    print(f"File uploaded successfully. File ID: {file.get('id')}")

if __name__ == '__main__':
    file_to_upload = input("Enter the file path to upload: ")
    folder_id='1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'
    #folder_id = input("Enter Google Drive folder ID (or leave blank): ").strip() or None
    upload_file(file_to_upload, folder_id)
