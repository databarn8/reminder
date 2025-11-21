#!/bin/bash

echo "Starting git push process..."

# Add all changes
echo "Adding changes..."
git add .

# Commit with descriptive message
echo "Committing changes..."
git commit -m "Fix extra RadioButton after LONG in vibration pattern selection

- Updated AlertSettingsScreenFixed.kt to properly filter out PULSE and CUSTOM vibration patterns
- Changed from hardcoded list back to filtered VibrationPattern.values()
- Ensures only 4 patterns (SINGLE, DOUBLE, TRIPLE, LONG) are displayed
- Fixes UI issue where extra RadioButton was appearing after LONG option

Resolves issue reported by user where vibration pattern section showed 
5 RadioButtons instead of expected 4."

# Pull latest changes
echo "Pulling latest changes..."
git pull

# Push to remote
echo "Pushing to remote..."
git push --set-upstream origin main

echo "Git push process completed!"