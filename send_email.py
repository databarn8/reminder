#!/usr/bin/env python3
import smtplib
import getpass

# Yahoo SMTP settings
smtp_server = "smtp.mail.yahoo.com"
port = 587

# Get input from user
recipient = input("To: ")
subject = input("Subject: ")
print("Message (end with Ctrl+D):")
message = ""

try:
    while True:
        line = input()
        message += line + "\n"
except EOFError:
    pass

# Get password
password = getpass.getpass("Password: ")

# Send email
try:
    server = smtplib.SMTP(smtp_server, port)
    server.starttls()
    server.login("xu100@yahoo.com", password)
    
    email_content = f"From: xu100@yahoo.com\nTo: {recipient}\nSubject: {subject}\n\n{message}"
    server.sendmail("xu100@yahoo.com", recipient, email_content)
    server.quit()
    print("Email sent!")
except Exception as e:
    print(f"Failed: {e}")