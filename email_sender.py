#!/usr/bin/env python3
import smtplib
import getpass
import os
from email.mime.multipart import MIMEMultipart
from email.mime.text import MIMEText
from email.mime.base import MIMEBase
from email import encoders

# Yahoo SMTP settings
smtp_server = "smtp.mail.yahoo.com"
port = 587
sender_email = "xu100@yahoo.com"

def get_recipient():
    """Get recipient email address from user"""
    while True:
        recipient = input("Enter recipient email address: ").strip()
        if "@" in recipient and "." in recipient.split("@")[1]:
            return recipient
        print("Invalid email address. Please try again.")

def get_attachment():
    """Get attachment file path from user"""
    attachment_path = input("Enter attachment file path (or press Enter to skip): ").strip()
    
    if not attachment_path:
        return None
    
    if not os.path.exists(attachment_path):
        print(f"File not found: {attachment_path}")
        return get_attachment()
    
    return attachment_path

def send_email_with_attachment(recipient, subject, message, attachment_path=None):
    """Send email with optional attachment"""
    password = getpass.getpass("Enter Yahoo password: ")
    
    try:
        # Create message object
        msg = MIMEMultipart()
        msg['From'] = sender_email
        msg['To'] = recipient
        msg['Subject'] = subject
        
        # Add message body
        msg.attach(MIMEText(message, 'plain'))
        
        # Add attachment if provided
        if attachment_path:
            with open(attachment_path, "rb") as attachment:
                part = MIMEBase('application', 'octet-stream')
                part.set_payload(attachment.read())
            
            encoders.encode_base64(part)
            filename = os.path.basename(attachment_path)
            part.add_header(
                'Content-Disposition',
                f'attachment; filename= {filename}'
            )
            msg.attach(part)
            print(f"Attached: {filename}")
        
        # Connect to SMTP server and send email
        server = smtplib.SMTP(smtp_server, port)
        server.starttls()
        server.login(sender_email, password)
        text = msg.as_string()
        server.sendmail(sender_email, recipient, text)
        server.quit()
        
        print("Email sent successfully!")
        return True
        
    except Exception as e:
        print(f"Failed to send email: {e}")
        return False

def main():
    print("=== Yahoo Email Sender ===")
    print(f"From: {sender_email}")
    print()
    
    # Get email details
    recipient = get_recipient()
    subject = input("Enter subject: ").strip()
    
    print("Enter message (press Enter twice to finish):")
    message_lines = []
    while True:
        line = input()
        if line == "" and len(message_lines) > 0 and message_lines[-1] == "":
            break
        message_lines.append(line)
    message = "\n".join(message_lines[:-1])  # Remove the last empty line
    
    # Get attachment
    attachment_path = get_attachment()
    
    # Send email
    print("\nSending email...")
    success = send_email_with_attachment(recipient, subject, message, attachment_path)
    
    if success:
        print("Email sent successfully!")
    else:
        print("Failed to send email.")

if __name__ == "__main__":
    main()