#!/bin/bash
cd /Users/thantthuaung/AndroidStudioProjects/LearnLog

echo "Checking git status..."
git status

echo ""
echo "Staging all changes..."
git add .

echo ""
echo "Committing changes..."
git commit -m "Update: Fixed ThreeTenBP initialization and adjusted FAB position"

echo ""
echo "Fetching remote changes..."
git fetch origin

echo ""
echo "Pulling remote changes with rebase..."
git pull origin main --rebase

echo ""
echo "Pushing local changes..."
git push -u origin main

echo ""
echo "Done!"
