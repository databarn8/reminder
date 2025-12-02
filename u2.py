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

def find_base_folders(partial_name):
    """Find base folders matching partial name in the filesystem"""
    matching_folders = []
    
    # Check common direct paths first for speed
    home_dir = os.path.expanduser('~')
    current_dir = os.path.abspath('.')
    
    # Check if partial name matches the home directory itself
    if partial_name.lower() in os.path.basename(home_dir).lower():
        matching_folders.append(home_dir)
    
    # Check if partial name matches current directory
    if partial_name.lower() in os.path.basename(current_dir).lower():
        if current_dir not in matching_folders:
            matching_folders.append(current_dir)
    
    # Search from root and other common locations
    search_paths = [
        '/',  # Root directory
        home_dir,
        current_dir,
    ]
    
    for search_path in search_paths:
        if not os.path.exists(search_path):
            continue
            
        try:
            for root, dirs, files in os.walk(search_path):
                # Skip certain system directories to speed up search
                if search_path == '/':
                    dirs[:] = [d for d in dirs if d not in ['System', 'Library', 'private', 'dev', 'proc', 'sys', 'cores']]
                
                for dir_name in dirs:
                    if partial_name.lower() in dir_name.lower():
                        full_path = os.path.join(root, dir_name)
                        if full_path not in matching_folders:
                            matching_folders.append(full_path)
                
                # Don't go too deep from root
                if search_path == '/':
                    if root.count(os.sep) > 3:
                        break
                else:
                    if root.count(os.sep) - search_path.count(os.sep) > 2:
                        break
        except PermissionError:
            continue
    
    return matching_folders

def find_subfolders_with_partial_name(base_path, partial_name, max_depth=10):
    """Find all subfolders matching partial name under base_path"""
    matching_folders = []
    
    if not os.path.exists(base_path):
        return []
    
    try:
        for root, dirs, files in os.walk(base_path):
            # Calculate depth
            depth = root.count(os.sep) - base_path.count(os.sep)
            if depth > max_depth:
                continue
            
            for dir_name in dirs:
                if partial_name.lower() in dir_name.lower():
                    full_path = os.path.join(root, dir_name)
                    matching_folders.append(full_path)
    except PermissionError:
        pass
    
    return matching_folders

def find_files_with_partial_name(folder_path, partial_name):
    """Find all files matching partial name in folder_path"""
    matching_files = []
    
    if not os.path.exists(folder_path):
        return []
    
    try:
        # Search in the folder and all subdirectories
        for root, dirs, files in os.walk(folder_path):
            for file_name in files:
                if partial_name.lower() in file_name.lower():
                    full_path = os.path.join(root, file_name)
                    matching_files.append(full_path)
    except PermissionError:
        pass
    
    return matching_files

def find_google_drive_folders(service, folder_name):
    """Find matching folder in Google Drive"""
    query = f"name contains '{folder_name}' and mimeType='application/vnd.google-apps.folder' and trashed=false"
    
    results = service.files().list(
        q=query,
        pageSize=100,
        fields="files(id, name)"
    ).execute()
    
    return results.get('files', [])

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
    
    # Step 1: Find base folder
    print("--- Step 1: Navigate to Local Folder ---")
    base_folder_name = input("Enter base folder name (or leave blank for current dir): ").strip()
    
    base_folder_path = None
    
    if base_folder_name:
        base_folders = find_base_folders(base_folder_name)
        
        if not base_folders:
            print(f"No folders found containing '{base_folder_name}'")
            return
        
        if len(base_folders) == 1:
            base_folder_path = base_folders[0]
            print(f"✓ Found base folder: {base_folder_path}")
        else:
            base_folder_path = select_from_list(base_folders, "base folder")
            if not base_folder_path:
                print("No base folder selected. Exiting.")
                return
            print(f"✓ Selected base folder: {base_folder_path}")
    else:
        base_folder_path = "."
        print(f"✓ Using current directory: {os.path.abspath(base_folder_path)}")
    
    # Step 2: Find subfolders with partial name
    partial_folder_name = input("Enter partial folder name to search (or leave blank to skip): ").strip()
    
    selected_folder = base_folder_path
    
    if partial_folder_name:
        subfolders = find_subfolders_with_partial_name(base_folder_path, partial_folder_name)
        
        if not subfolders:
            print(f"No subfolders found containing '{partial_folder_name}'")
            return
        
        selected_folder = select_from_list(subfolders, "folder")
        if not selected_folder:
            print("No folder selected. Exiting.")
            return
        
        print(f"✓ Selected folder: {selected_folder}")
    
    # Step 3: Find files with partial name
    partial_file_name = input("Enter partial filename to search: ").strip()
    
    if not partial_file_name:
        print("No filename provided. Exiting.")
        return
    
    files = find_files_with_partial_name(selected_folder, partial_file_name)
    
    if not files:
        print(f"No files found containing '{partial_file_name}' in {selected_folder}")
        return
    
    selected_file = select_from_list(files, "file")
    if not selected_file:
        print("No file selected. Exiting.")
        return
    
    print(f"\n✓ Selected file: {selected_file}")
    
    # Step 4: Select Google Drive destination folder
    #print("\n--- Step 2: Select Google Drive Destination ---")
    drive_folder_search = input("Enter folder name to search in Google Drive (or leave blank for default): ").strip()
    
    folder_id = '1F8QLu24HzgpqyqIbfjmKoTVDG9YRoxvR'  # default
    
    if drive_folder_search:
        drive_folders = find_google_drive_folders(service, drive_folder_search)
        if drive_folders:
            selected_drive_folder = select_from_list(drive_folders, "Google Drive folder")
            
            if selected_drive_folder:
                folder_id = selected_drive_folder['id']
                folder_name = selected_drive_folder['name']
                print(f"\n✓ Selected destination: {folder_name}")
        else:
            print(f"No Google Drive folders found containing '{drive_folder_search}'. Using default.")
    else:
        print(f"\n✓ Using default folder")
    
    # Step 5: Upload
    print(f"\n📤 Uploading '{os.path.basename(selected_file)}'...")
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
