# Android Reminder App - Current Requirements

## ✅ Priority Interface Improvement - COMPLETED
- ✅ Replace priority chips with a simple slider bar
- ✅ Allow sliding from 1 to 10 and 10 to 1
- ✅ More intuitive and compact interface
- ✅ Color-coded slider (Green=Low, Blue=Medium, Orange=High, Red=Urgent)

## ✅ When Field Enhancement - COMPLETED
- ✅ Add "when" field to reminder display
- ✅ Extract time/date info from reminder content if mentioned
- ✅ If no time info found, leave blank and prompt user to enter
- ✅ Display prominently in reminder list and detail views
- ✅ Warning when "when" field is not specified

## ✅ Database Schema Changes - COMPLETED
- ✅ Add "when" field to Reminder entity
- ✅ Keep existing priority field (1-10)
- ✅ Maintain backward compatibility with destructive migration
- ✅ Database version updated to v8

## ✅ UI Changes Required - COMPLETED
- ✅ Replace FilterChip row with Slider for priority selection
- ✅ Add "when" field display in ReminderListScreen
- ✅ Add "when" input field in InputScreen
- ✅ Parse time/date from content when creating reminders
- ✅ Enhanced time parsing (supports "3pm", "3:00", "3 o'clock")

## 📱 Current Version Info
- **Version**: v8.0 (Slider + When Field)
- **APK**: `app-debug-slider-when-v8.0.apk`
- **Git Commit**: `d2f78f6`
- **Features**: Priority slider, When field, Enhanced parsing

## 🎯 Next Features (When Ready)
- Time picker (visual time selection)
- Notification settings (custom sounds/vibration)
- Category management
- Calendar view

## ⚠️ CRITICAL PROCESS REQUIREMENTS
- **MAX 3 RETRIES LIMIT**: Never retry the same method/approach more than 3 times
- **RESTART PROCESS**: Every time process restarts, read CURRENT_REQUIREMENTS.md FIRST before taking any action
- **LOOP PREVENTION**: If stuck after 3 retries, STOP and ask for different approach
- **DOCUMENTATION**: All requirements and constraints are documented in this file
- **FIRST ACTION**: Always read this file first when starting/restarting work