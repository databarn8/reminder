import os
from google.oauth2.credentials import Credentials
from google_auth_oauthlib.flow import InstalledAppFlow
from google.auth.transport.requests import Request
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

def find_local_folders(base_path=".", partial_name="", max_depth=5):
    """Find local directories matching the criteria"""
    matching_folders = []
    
    # Expand user home directory
    base_path = os.path.expanduser(base_path)
    
    # If base_path doesn't exist, try searching from current directory
    if not os.path.exists(base_path):
        # Search from current directory and home directory
        search_paths = ['.', os.path.expanduser('~')]
        for search_path in search_paths:
            for root, dirs, files in os.walk(search_path):
                for dir_name in dirs:
                    if base_path.lower() in dir_name.lower():
                        full_path = os.path.join(root, dir_name)
                        matching_folders.append(full_path)
                # Don't go too deep when searching for base folder
                if root.count(os.sep) - search_path.count(os.sep) > 3:
                    break
        
        if matching_folders:
            return matching_folders
        else:
            return []
    
    # If base_path exists, search within it recursively
    if os.path.isdir(base_path):
        for root, dirs, files in os.walk(base_path):
            # Calculate current depth
            depth = root.count(os.sep) - base_path.count(os.sep)
            if depth > max_depth:
                continue
                
            for dir_name in dirs:
                if not partial_name or partial_name.lower() in dir_name.lower():
                    full_path = os.path.join(root, dir_name)
                    matching_folders.append(full_path)
    
    return matching_folders

def find_google_drive_folders(service, folder_name):
    """Find matching folder in Google Drive"""
    query = f"name contains '{folder_name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
    
    results = service.files().list(
        q=query,
        pageSize=100,
        fields="files(id, name)"
    ).execute()
    
    return results.get('files', [])

def find_local_files(base_path=".", partial_name="", recursive=True):
    """Find local files matching partial name"""
    matching_files = []
    
    # Expand user home directory
    base_path = os.path.expanduser(base_path)
    
    # If base_path is a file, just return it
    if os.path.isfile(base_path):
        return [base_path]
    
    # If base_path doesn't exist, return empty
    if not os.path.exists(base_path):
        return []
    
    # Search for files in the directory
    if recursive:
        for root, dirs, files in os.walk(base_path):
            for file in files:
                if not partial_name or partial_name.lower() in file.lower():
                    full_path = os.path.join(root, file)
                    matching_files.append(full_path)
    else:
        # Only search one level deep
        for root, dirs, files in os.walk(base_path):
            for file in files:
                if not partial_name or partial_name.lower() in file.lower():
                    full_path = os.path.join(root, file)
                    matching_files.append(full_path)
            break
    
    return matching_files

def select_from_list(items, item_type="item"):
    """Generic selection function"""
    if not items:
        print(f"No {item_type}s found!")
        return None
    
    print(f"\n=== Found {len(items)} {item_type}(s) ===")
    for idx, item in enumerate(items, 1):
        display_name = item['name'] if isinstance(item, dict) else item
        print(f"{idx}. {display_name}")
    
    while True:
        try:
            choice = input(f"\nSelect {item_type} number (or 'q' to quit): ").strip()
            if choice.lower() == 'q':
                return None
            
            idx = int(choice) - 1
            if 0 <= idx < len(items):
                return items[idx]
            else:
                print("Invalid selection. Try again.")
        except ValueError:
            print("Please enter a valid number.")

def upload_file(file_path, drive_folder_id='1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'):
    creds = authenticate()
    service = build('drive', 'v3', credentials=creds)
    
    file_metadata = {'name': os.path.basename(file_path)}
    if drive_folder_id:
        file_metadata['parents'] = [drive_folder_id]

    media = MediaFileUpload(file_path, resumable=True)
    file = service.files().create(
        body=file_metadata,
        media_body=media,
        fields='id, name, webViewLink'
    ).execute()
    
    print(f"\n✓ File uploaded successfully!")
    print(f"  Name: {file.get('name')}")
    print(f"  ID: {file.get('id')}")
    print(f"  Link: {file.get('webViewLink')}")

def interactive_mode():
    """Interactive folder and file selection"""
    print("\n=== Interactive Google Drive Upload ===\n")
    
    creds = authenticate()
    service = build('drive', 'v3', credentials=creds)
    
    # Step 1: Navigate local folders to find file
    print("--- Step 1: Find Local File ---")
    base_folder = input("Enter base local folder name (or leave blank for current dir): ").strip() or "."
    
    # Find matching local folders
    local_folders = find_local_folders(base_folder, "")
    
    local_base_path = "."
    if local_folders and base_folder != ".":
        print(f"\n=== Found {len(local_folders)} matching local folder(s) ===")
        for idx, folder in enumerate(local_folders, 1):
            print(f"{idx}. {folder}")
        
        selected_local = select_from_list(local_folders, "local folder")
        if selected_local:
            local_base_path = selected_local
            print(f"\n✓ Using local folder: {local_base_path}")
    else:
        local_base_path = os.path.expanduser(base_folder) if base_folder else "."
    
    # Now search for subfolder if needed
    partial_subfolder = input("Enter partial subfolder name to filter (leave blank to skip): ").strip()
    if partial_subfolder:
        subfolders = find_local_folders(local_base_path, partial_subfolder)
        if subfolders:
            selected_subfolder = select_from_list(subfolders, "subfolder")
            if selected_subfolder:
                local_base_path = selected_subfolder
                print(f"\n✓ Using subfolder: {local_base_path}")
    
    # Search for files
    partial_file = input("Enter partial filename to search: ").strip()
    local_files = find_local_files(local_base_path, partial_file)
    selected_file = select_from_list(local_files, "file")
    
    if not selected_file:
        print("No file selected. Exiting.")
        return
    
    print(f"\n✓ Selected file: {selected_file}")
    
    # Step 2: Select Google Drive destination folder
    print("\n--- Step 2: Select Google Drive Destination Folder ---")
    drive_folder_search = input("Enter folder name to search in Google Drive (leave blank for default): ").strip()
    
    folder_id = '1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'  # default
    
    if drive_folder_search:
        drive_folders = find_google_drive_folders(service, drive_folder_search)
        selected_drive_folder = select_from_list(drive_folders, "Google Drive folder")
        
        if selected_drive_folder:
            folder_id = selected_drive_folder['id']
            folder_name = selected_drive_folder['name']
            print(f"\n✓ Selected destination: {folder_name}")
    else:
        print(f"\n✓ Using default folder")
    
    # Step 3: Upload
    print(f"\n📤 Uploading...")
    upload_file(selected_file, folder_id)

def direct_mode():
    """Direct upload with file path input"""
    file_to_upload = input("Enter the file path to upload: ")
    folder_id = '1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'
    upload_file(file_to_upload, folder_id)

if __name__ == '__main__':
    print("=== Google Drive File Uploader ===")
    print("1. Interactive mode (browse folders and files)")
    print("2. Direct mode (enter file path)")
    
    mode = input("\nSelect mode (1 or 2): ").strip()
    
    if mode == '1':
        interactive_mode()
    else:
        direct_mode()
