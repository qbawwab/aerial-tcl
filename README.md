# AERIAL / TCL — custom USB video screensaver

A lightweight **Android TV / Google TV** project aimed at the **TCL 50T7B**. It is an independent custom app, **not a fork of OrangeJedi/Aerial or Aerial Views**. No telemetry, downloads, streaming, logins, ads, or network permission. The APK contains **no copyrighted Apple videos**; you provide your own on a USB stick.

## Features (v0.1)

- Actual Android `DreamService` for automatic idle activation once registered by the TV.
- Select a **USB folder**, or select individual video files via Android's system file picker.
- Permanently retains read-only access through Android's Storage Access Framework; no broad storage permissions.
- Automatically scans the chosen USB folder at screensaver start (including nested folders).
- Shuffle, loop, optional clock and file-name overlay.
- Full-screen silent video using Android's built-in decoder.
- Remote: **Right arrow** skips a video; **Back / OK** exits from the actual screensaver. Preview uses **Back** to return.
- Supports common video extensions (.mp4, .m4v, .mkv, .mov, .webm, .ts), **provided the TV can decode the codec**.

This is a **starter implementation**, not hardware-tested on the TCL 50T7B. There may be a short black gap between video files. Dolby Vision/HDR playback depends on the TV's Android decoder and original video profile.

## Build a real APK via GitHub Actions (no Android Studio required)

1. Create a new GitHub repository and **upload the contents of this project**, including the hidden `.github/workflows/build-apk.yml` file (not just this ZIP itself). Public repo is simplest; keep video files out of the repository.
2. In GitHub, open **Actions → Build Aerial TCL APK → Run workflow**.
3. After the action **succeeds**, open the run and download the artifact named **AerialTCL-debug-apk**.
4. Unzip it to obtain `app-debug.apk`, an installable debug APK signed automatically by the build environment.

Alternatively, open this folder in Android Studio with JDK 17 and Android SDK 35, and use **Build → Build APK(s)** (AGP 8.8.2 / Gradle 8.10.2). The GitHub workflow installs its own compatible Gradle version.

## Install on your TCL 50T7B (Google TV)

1. Copy `app-debug.apk` to your Windows PC. [Download Android SDK Platform Tools](https://developer.android.com/tools/releases/platform-tools) and open a terminal containing `adb`.
2. Enable TV Developer options (`Settings → System → About → Android TV OS build`, press OK 7 times) and ADB / USB debugging. Note the TV's IP address and keep PC and TV on the same local network.
3. Run these commands (replace `TV_IP`; accept TV's ADB authorization prompt):

```powershell
adb connect TV_IP
adb install -r app-debug.apk
```

4. Launch **Aerial TCL** on the TV and use **CHOOSE USB VIDEO FOLDER**. Select the folder on your USB stick where the aerial videos are stored, and confirm folder access. Select **PREVIEW MY SCREENSAVER** to test.
5. Register it as the system screensaver:

```powershell
adb shell settings put secure screensaver_components com.aerialtcl.usbdream/.UsbDreamService
adb shell settings put secure screensaver_enabled 1
adb shell settings put system screen_off_timeout 600000
adb shell settings get secure screensaver_components
```

6. **TCL-specific background permission:** If the TV has the **Safety Guard** app, open `Permission Shield → Auto Launch Permission`; set **Auto manager → Closed**, then find **Aerial TCL** and set it to **Opened**. Without this, TCL firmware may prevent automatic launching. On some versions an ADB AppOps change may be needed; see the [Aerial Views TCL guide](https://github.com/theothernt/AerialViews#allow-auto-launch-on-tcl-tvs). Our app package is `com.aerialtcl.usbdream`, not the Aerial Views package.
7. Return to the TV home screen and leave it idle for approximately ten minutes; if the default ambient screen still wins, check Google TV's power/ambient settings and developer/debugging state. Firmware behavior varies.

You may also run the included `install-tcl.ps1` **if** `adb` is on PATH and `app-debug.apk` is next to the script. It runs installation and screensaver registration.

### Troubleshooting USB pickers

The Android system file-picker must be present and expose your USB stick. Some Google TV firmwares do not expose USB volumes in `ACTION_OPEN_DOCUMENT_TREE`. If the USB folder doesn't show up, try **OR CHOOSE INDIVIDUAL VIDEO FILES**. If neither picker works, we must adapt the app for your TV's USB provider rather than claiming that all Google TVs allow this approach. Keep the USB mounted when the screensaver runs. If you move it to a different port, re-select the folder if permissions stop working.

### Revert the TV screensaver

```powershell
adb shell settings put secure screensaver_components com.google.android.apps.tv.dreamx/.service.Backdrop
```

### Video files

Start with standard 4K/1080p **MP4 H.264 or HEVC** files. Some Apple aerial clips are Dolby Vision and might not decode with this basic player despite the TV's HDR support. If you experience black playback, test with a normal H.264 MP4 first.

## Code map

- `MainActivity.java`: TV-friendly UI, Android folder/file picker, preferences
- `VideoLibrary.java`: safely enumerates selected USB folder (max 600 files) and single-file selections
- `PlaybackView.java`: silent sequential video playback, optional clock/name overlays
- `UsbDreamService.java`: Android framework screensaver service
- `PreviewActivity.java`: manual test player
- `.github/workflows/build-apk.yml`: build artifact in GitHub Actions

## Caveats

- Not compiled or device-tested in the environment where this project was created (Android SDK not available). Build and test with Android SDK/CI before use.
- Manual source selection needed once via the TV's system picker. No silent auto-discovery of all USB content (Android scoped-storage restrictions).
- Selecting new individual videos replaces the previous manually-selected list; the folder selection is kept separately.
- This is a **debug APK for personal testing**, not a Play Store release. Use a persistent signing key before distributing app updates; different ephemeral GitHub Action debug keys can make later APKs unable to install over the previous APK without uninstalling first.
