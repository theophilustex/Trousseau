# Trousseau Mobile Companion App

The official companion mobile app for Trousseau. Allows you to glance at today's weather-adapted outfit, swap clothing options on the fly, and confirm what you wore with 1 tap so wear & laundry tracking is completely effortless.

## Features
- **Server Connection**: Connects to your local or remote self-hosted Trousseau instance (LAN IP, reverse proxy URL, or Tailscale/WireGuard address).
- **Today's Glance**: Displays current temperature, weather conditions, and recommended outfit.
- **1-Tap Commitment ("I Wore This Today")**: Updates wear counts across your items immediately, triggering automatic laundry alerts when thresholds are reached.
- **Instant Swap**: Select an alternative outfit if your daily plans change.

---

## Generating the Android APK

You can build a standalone `.apk` to sideload onto any Android phone or tablet.

### Method 1: Using EAS Build (Cloud / Fastest)
```bash
# 1. Install EAS CLI if you haven't already:
npm install -g eas-cli

# 2. Log in to your Expo account:
eas login

# 3. Build the standalone APK:
pnpm --filter @trousseau/mobile run build:apk
# or
eas build -p android --profile preview
```
Once the build completes, EAS will provide a direct download link for your `trousseau.apk`.

### Method 2: Local Gradle Build (100% Offline / No Account Needed)
```bash
cd apps/mobile

# Prebuild the native Android project
npx expo prebuild --platform android

# Compile APK locally using Gradle
cd android
./gradlew assembleRelease
```
The generated APK file will be located at:
`apps/mobile/android/app/build/outputs/apk/release/app-release.apk`
