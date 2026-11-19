# Personal Offline Voice Assistant (Android / Kotlin)

100% free, offline-first personal voice assistant designed for Android phones (including Redmi 10 Power / MIUI).

## Features
- **Custom Renameable Wake Word:** Powered by [Vosk](https://alphacephei.com/vosk/android) (runs offline on-device). Change your assistant's name anytime in the app.
- **Next Class (Offline):** Reads from your pre-configured weekly class schedule.
- **Daily To-Do List (Offline):** Add tasks by voice ("add task finish assignment") or in the app, and ask "what is my todo list".
- **Favorite Food & Favorite Line (Offline):** Stores and speaks your personal preferences on command.
- **Weather:** Uses [Open-Meteo](https://open-meteo.com/) (free, no API key required) when online, with an offline fallback message.

## Option A: Build APK Automatically on GitHub (No Android Studio Needed)
1. Create a free repository on GitHub and upload all files from this folder (including the hidden `.github/workflows/build.yml` file).
2. Open the **Actions** tab in your GitHub repository — the **Build Android APK** workflow will automatically download the ~40 MB Vosk model and compile `app-debug.apk`.
3. Download the `personal-assistant-apk` zip from the completed workflow run, extract `app-debug.apk`, and install it on your Redmi 10 Power.

## Option B: Build Locally in Android Studio
1. Open this folder in **Android Studio**.
2. Download `vosk-model-small-en-us-0.15` from https://alphacephei.com/vosk/models and place its contents inside `app/src/main/assets/model-en-us/`.
3. Click **Run** or **Build -> Build APK(s)**.

## Redmi / MIUI Background Tip
On your Redmi 10 Power, open **Settings -> Apps -> Manage apps -> Personal Assistant**, enable **Autostart**, and set **Battery saver** to **No restrictions** so MIUI does not stop the background microphone listener.
