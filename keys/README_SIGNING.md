# Release Signing Information

This directory contains the release signing keystore configuration for the Al-Minshawi Quran Android application.

## Keystore Details

- **Keystore Filename:** `release-key.jks`
- **Keystore Location:** `/keys/release-key.jks` (relative to project root)
- **Key Alias:** `release-alias`
- **Gradle Location (Configuration):** `app/build.gradle.kts`
  - Gradle reads the keystore location and passwords automatically from `signing.properties` in the project root.

## Certificate Fingerprints

- **SHA1:** `B1:5D:94:C0:38:89:28:B7:C6:D8:6B:24:1F:30:A0:05:57:C9:AC:8E`
- **SHA256:** `CA:2A:66:2E:EA:E9:E5:84:3E:1D:84:08:FB:0A:DA:A0:0F:9B:75:7C:B6:E1:C5:62:98:3B:29:E1:29:AD:A9:28`

## Google Play Usage Instructions

1. **Initial Upload:**
   - When preparing the first release of the application on the Google Play Console, opt-in to **Google Play App Signing** (recommended).
   - Use the App Bundle (`.aab`) generated using this keystore to sign the application. Google Play will use this upload key to verify your identity and then re-sign the app with their secure main key.

2. **App Update Requirements:**
   - Any future updates submitted to Google Play Console must be signed with this exact keystore (`release-key.jks`) and alias (`release-alias`).
   - If signed with a different key, Google Play Console will reject the update.
