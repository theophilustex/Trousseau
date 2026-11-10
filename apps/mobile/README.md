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

### Method 1: Automatic GitHub Actions (1-Click Download, No Local Setup Required)
1. Push your code to your repository on GitHub.
2. Go to the **Actions** tab in your GitHub repository (`https://github.com/<owner>/Trousseau/actions`).
3. Select the **Build Android APK** workflow and click **Run workflow** (or it triggers automatically on push).
4. When finished (typically 3–5 minutes), scroll to the **Artifacts** section at the bottom of the run page.
5. Download **`trousseau-mobile-apk`**, unzip the downloaded file, and install `app-debug.apk` directly on your Android phone!

### Method 2: Using EAS Build (Cloud)
```bash
# 1. Install EAS CLI:
npm install -g eas-cli

# 2. Log in to your Expo account:
eas login

# 3. Build standalone APK:
pnpm --filter @trousseau/mobile run build:apk
# or
eas build -p android --profile preview
```
Once the build completes, EAS provides a direct download link for your `.apk`.

### Method 3: Local Gradle Build
```bash
cd apps/mobile

# Prebuild the native Android project
npx expo prebuild --platform android

# Compile APK locally using Gradle
cd android
./gradlew assembleDebug
```
The generated APK file will be located at:
`apps/mobile/android/app/build/outputs/apk/debug/app-debug.apk`
