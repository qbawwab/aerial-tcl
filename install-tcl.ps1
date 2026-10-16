# Save the built app-debug.apk next to this script.
# Requires Android SDK Platform Tools (adb.exe) in your PATH.
$ErrorActionPreference = 'Stop'
$ip = Read-Host 'Enter your TCL Google TV IP address (e.g. 192.168.1.120)'
$apk = Join-Path $PSScriptRoot 'app-debug.apk'
if (-not (Test-Path $apk)) { throw 'app-debug.apk not found. Build/download the APK and put it beside this script.' }
adb connect $ip
Write-Host 'Accept the ADB debugging request on your TV, then press Enter here.'
Read-Host | Out-Null
adb install -r $apk
adb shell settings put secure screensaver_components com.aerialtcl.usbdream/.UsbDreamService
adb shell settings put secure screensaver_enabled 1
adb shell settings put system screen_off_timeout 600000
adb shell settings get secure screensaver_components
Write-Host 'Done. On your TCL, allow Aerial TCL to Auto Launch (Safety Guard), then open the app to select your USB folder.'
