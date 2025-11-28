#!/usr/bin/env python3
"""
Reminder App Email Client
Based on EnhancedEmailService.kt from the reminder app
A simple Python email client to send reminder backup ZIP files via email
"""

import smtplib
import ssl
import mimetypes
import os
import sys
from email import encoders
from email.mime.base import MIMEBase
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.utils import formatdate
import tkinter as tk
from tkinter import filedialog, messagebox, simpledialog, ttk
import threading
import time

class EmailClient:
    def __init__(self, root):
        self.root = root
        self.root.title("Reminder Backup Email Client")
        self.root.geometry("600x500")
        
        # Email configuration
        self.smtp_server = "smtp.gmail.com"
        self.smtp_port = 587
        self.sender_email = ""
        self.sender_password = ""
        self.recipient_email = ""
        self.selected_file = ""
        
        # Create UI
        self.create_widgets()
        
    def create_widgets(self):
        # Main frame
        main_frame = ttk.Frame(self.root, padding="10")
        main_frame.grid(row=0, column=0, sticky=(tk.W, tk.E, tk.N, tk.S))
        
        # Title
        title_label = ttk.Label(main_frame, text="Reminder Backup Email Client", 
                               font=('Arial', 14, 'bold'))
        title_label.grid(row=0, column=0, columnspan=2, pady=(0, 20))
        
        # Sender Email
        ttk.Label(main_frame, text="Your Gmail:").grid(row=1, column=0, sticky=tk.W, pady=5)
        self.sender_entry = ttk.Entry(main_frame, width=40)
        self.sender_entry.grid(row=1, column=1, pady=5, padx=(10, 0))
        
        # Password
        ttk.Label(main_frame, text="App Password:").grid(row=2, column=0, sticky=tk.W, pady=5)
        self.password_entry = ttk.Entry(main_frame, width=40, show="*")
        self.password_entry.grid(row=2, column=1, pady=5, padx=(10, 0))
        
        # Recipient Email
        ttk.Label(main_frame, text="Recipient Email:").grid(row=3, column=0, sticky=tk.W, pady=5)
        self.recipient_entry = ttk.Entry(main_frame, width=40)
        self.recipient_entry.grid(row=3, column=1, pady=5, padx=(10, 0))
        
        # File Selection
        file_frame = ttk.Frame(main_frame)
        file_frame.grid(row=4, column=0, columnspan=2, pady=20, sticky=(tk.W, tk.E))
        
        ttk.Label(file_frame, text="Backup ZIP File:").pack(side=tk.LEFT, padx=(0, 10))
        self.file_label = ttk.Label(file_frame, text="No file selected", 
                                 foreground="blue", cursor="hand2")
        self.file_label.pack(side=tk.LEFT, padx=10)
        self.file_label.bind("<Button-1>", self.browse_file)
        
        browse_btn = ttk.Button(file_frame, text="Browse", command=self.browse_file)
        browse_btn.pack(side=tk.LEFT, padx=5)
        
        # Send Button
        self.send_btn = ttk.Button(main_frame, text="Send Email", 
                               command=self.send_email_threaded)
        self.send_btn.grid(row=5, column=0, columnspan=2, pady=30)
        
        # Status label
        self.status_label = ttk.Label(main_frame, text="Ready to send", 
                                   foreground="green")
        self.status_label.grid(row=6, column=0, columnspan=2, pady=10)
        
        # Instructions
        instructions = ttk.LabelFrame(main_frame, text="Instructions", padding="10")
        instructions.grid(row=7, column=0, columnspan=2, pady=20, sticky=(tk.W, tk.E))
        
        inst_text = """1. Enter your Gmail and App Password
2. Enter recipient email address
3. Click Browse to select ZIP file
4. Click Send Email

Note: Use Gmail App Password, not regular password.
Enable 2-factor authentication and create App Password
at: https://myaccount.google.com/apppasswords"""
        
        ttk.Label(instructions, text=inst_text, wraplength=550).pack()
        
    def browse_file(self):
        """Browse for ZIP file"""
        initial_dir = "./reminder_backups"
        if not os.path.exists(initial_dir):
            initial_dir = "./"
            
        file_path = filedialog.askopenfilename(
            title="Select Backup ZIP File",
            initialdir=initial_dir,
            filetypes=[("ZIP files", "*.zip"), ("All files", "*.*")]
        )
        
        if file_path:
            self.selected_file = file_path
            filename = os.path.basename(file_path)
            self.file_label.config(text=filename)
            
    def send_email_threaded(self):
        """Send email in a separate thread"""
        # Disable send button
        self.send_btn.config(state='disabled')
        self.status_label.config(text="Sending...", foreground="orange")
        
        # Start email sending in background thread
        thread = threading.Thread(target=self.send_email)
        thread.daemon = True
        thread.start()
        
    def send_email(self):
        """Send an email with attachment"""
        try:
            # Get form data
            sender_email = self.sender_entry.get().strip()
            sender_password = self.password_entry.get()
            recipient_email = self.recipient_entry.get().strip()
            
            # Validate inputs
            if not sender_email or not sender_password or not recipient_email:
                self.update_status("Please fill in all fields", "red")
                self.send_btn.config(state='normal')
                return
                
            if not self.selected_file or not os.path.exists(self.selected_file):
                self.update_status("Please select a valid ZIP file", "red")
                self.send_btn.config(state='normal')
                return
            
            # Create message
            msg = MIMEMultipart()
            msg['From'] = sender_email
            msg['To'] = recipient_email
            msg['Date'] = formatdate(localtime=True)
            
            # Email body - based on EnhancedEmailService.kt format
            body = f"""📅 REMINDER NOTIFICATION
            
Please find attached to reminder backup ZIP file containing:
- All your reminders in JSON format
- Backup metadata and database files
- README with restoration instructions

Generated on: {time.strftime('%Y-%m-%d %H:%M:%S')}
File size: {os.path.getsize(self.selected_file) / (1024*1024):.2f} MB

To restore reminders:
1. Save the attached ZIP file
2. Extract the reconstructed_reminders_*.json file
3. Use the Reminder App's import function
4. Select the JSON file and follow on-screen instructions

This is an automated backup from your Reminder App.
📱 Reminder App
Your personal task assistant
"""
            
            msg.attach(MIMEText(body, 'plain'))
            
            # Add ZIP attachment
            self.update_status("Attaching file...", "blue")
            self.attach_file(msg, self.selected_file)
            
            # Send email
            self.update_status("Connecting to email server...", "blue")
            
            # Create secure SMTP connection
            context = ssl.create_default_context()
            server = smtplib.SMTP(self.smtp_server, self.smtp_port)
            server.starttls(context=context)
            
            # Login and send
            server.login(sender_email, sender_password)
            self.update_status("Sending email...", "blue")
            text = msg.as_string()
            server.sendmail(sender_email, recipient_email, text)
            server.quit()
            
            # Success
            self.update_status(f"Email sent successfully to {recipient_email}", "green")
            
        except Exception as e:
            error_msg = f"Failed to send email: {str(e)}"
            self.update_status(error_msg, "red")
            print(f"Email error: {e}", file=sys.stderr)
        finally:
            # Re-enable send button
            self.send_btn.config(state='normal')
            
    def attach_file(self, msg, file_path):
        """Attach a file to the email"""
        filename = os.path.basename(file_path)
        
        # Guess MIME type
        ctype, encoding = mimetypes.guess_type(file_path)
        if ctype is None or encoding is not None:
            ctype = 'application/octet-stream'
            
        maintype, subtype = ctype.split('/', 1)
        
        # Read file and create attachment
        with open(file_path, 'rb') as fp:
            attachment = MIMEBase(maintype, subtype)
            attachment.set_payload(fp.read())
            encoders.encode_base64(attachment)
            
        attachment.add_header('Content-Disposition', f'attachment; filename="{filename}"')
        msg.attach(attachment)
        
    def update_status(self, message, color):
        """Update status label in main thread"""
        self.root.after(0, lambda: self.status_label.config(text=message, foreground=color))

def main():
    root = tk.Tk()
    app = EmailClient(root)
    
    # Handle window close
    def on_closing():
        if messagebox.askokcancel("Quit", "Are you sure you want to quit?"):
            root.destroy()
    
    root.protocol("WM_DELETE_WINDOW", on_closing)
    root.mainloop()

if __name__ == "__main__":
    main()