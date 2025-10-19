#!/bin/bash
cd /Users/thantthuaung/AndroidStudioProjects/LearnLog
rm -rf app/build
rm -rf build
rm -rf .gradle
./gradlew clean
./gradlew assembleDebug

