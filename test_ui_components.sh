#!/bin/bash

echo "=== Testing UI Components ==="
echo "1. Checking ReminderListScreen for 'Reminders' text in title..."
if grep -A 5 -B 5 "title = {" app/src/main/java/com/reminder/app/ui/screens/ReminderListScreen.kt | grep -q "Reminders"; then
    echo "   ❌ ISSUE: 'Reminders' text found in title section"
else
    echo "   ✅ OK: No 'Reminders' text in title section"
fi

echo "2. Checking ArchiveRestoreScreen for back arrow..."
if grep -n "onBack" app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt; then
    echo "   ✅ OK: onBack parameter found in ArchiveRestoreScreen.kt"
else
    echo "   ❌ ISSUE: onBack parameter not found in ArchiveRestoreScreen.kt"
fi

echo "3. Checking for back arrow icon import..."
if grep -n "Icons.Default.ArrowBack" app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt; then
    echo "   ✅ OK: ArrowBack icon import found"
else
    echo "   ❌ ISSUE: ArrowBack icon import not found"
fi

echo "4. Checking ArchiveRestoreScreen navigationIcon structure..."
if grep -A 10 -B 10 "navigationIcon" app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt | grep -q "Row"; then
    echo "   ✅ OK: navigationIcon with Row structure found"
else
    echo "   ❌ ISSUE: navigationIcon Row structure not found"
fi

echo "5. Checking for back arrow and home icon in navigationIcon..."
if grep -A 15 -B 15 "navigationIcon" app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt | grep -q "ArrowBack\|Home"; then
    echo "   ✅ OK: Both ArrowBack and Home icons found in navigationIcon"
else
    echo "   ❌ ISSUE: Missing ArrowBack or Home icons in navigationIcon"
fi

echo ""
echo "=== ArchiveRestoreScreen navigationIcon Section ==="
grep -A 15 -B 15 "navigationIcon" app/src/main/java/com/reminder/app/ui/screens/ArchiveRestoreScreen.kt

echo ""
echo "6. Checking ReminderListScreen for 'Reminders' text in title section (lines 110-120)..."
if sed -n '110,120p' app/src/main/java/com/reminder/app/ui/screens/ReminderListScreen.kt | grep -q "Reminders"; then
    echo "   ❌ ISSUE: 'Reminders' text still found in title section (lines 110-120)"
else
    echo "   ✅ OK: No 'Reminders' text in title section"
fi

echo ""
echo "=== Summary ==="
echo "If you see issues above, the changes may not have been applied correctly."
echo "Make sure to rebuild and reinstall the app to see the changes."