# Keystore Backup & Recovery Instructions

The Android Release Keystore (`release-key.jks`) is a critical security asset. If lost or compromised, you will not be able to publish updates to your existing app store listing without performing a recovery process.

## How to Backup the Keystore

1. **Backup Directory:**
   - Copy the `keys/` directory and the `signing.properties` file from the project root.
2. **Secure Offline Storage:**
   - Store copies in secure locations:
     - Encrypted external drives.
     - Secure corporate password managers (e.g., 1Password, Bitwarden) that support file attachments.
     - Secure, access-controlled cloud storage (e.g., Google Drive, OneDrive) using an encrypted archive (e.g., password-protected 7z/ZIP).
3. **DO NOT Commit to Git:**
   - Never commit `release-key.jks` or `signing.properties` to a public repository. They are ignored by `.gitignore`.

## How to Restore on Another PC

To set up the build environment on a new developer machine:

1. **Clone the Project:**
   - Clone the git repository to the new PC.
2. **Restore Keystore Folder:**
   - Create a folder named `keys` in the project root.
   - Place the backed-up `release-key.jks` file inside `keys/`.
3. **Restore Properties File:**
   - Create `signing.properties` in the project root.
   - Populate it with the matching configuration (ensure the exact same passwords):
     ```properties
     STORE_FILE=keys/release-key.jks
     STORE_PASSWORD=v6P9#kL2!mW8$qRt
     KEY_ALIAS=release-alias
     KEY_PASSWORD=v6P9#kL2!mW8$qRt
     ```
4. **Build:**
   - Run `./gradlew assembleRelease` or `./gradlew bundleRelease`. Gradle will automatically detect and use the restored credentials.

## What Happens if the Keystore is Lost?

- **Without Google Play App Signing:**
  - If you did not opt-in to Google Play App Signing, losing the keystore means **you can never update the app again**. You would have to register a new package name (e.g. `com.example.app.v2`) and publish it as a completely new app. All existing users and downloads would be lost.
- **With Google Play App Signing:**
  - If you opted into Google Play App Signing, the keystore you generated is your "Upload Key". If lost, you can contact Google Play Support to reset it.

## Google Play Recovery Recommendations

If you lose your upload key (`release-key.jks`):

1. **Generate a New Keystore:**
   - Run the keytool command to generate a new JKS key:
     ```powershell
     keytool -genkeypair -v -keystore keys/new-upload-key.jks -keyalg RSA -keysize 4096 -validity 10000 -alias new-upload-alias -storepass [PASSWORD] -keypass [PASSWORD] -dname "CN=Al-Minshawi Quran, O=Al-Minshawi, C=EG"
     ```
2. **Export Certificate to PEM format:**
   - Export the public certificate of the new key to a `.pem` file:
     ```powershell
     keytool -export -rfc -alias new-upload-alias -file upload_certificate.pem -keystore keys/new-upload-key.jks -storepass [PASSWORD]
     ```
3. **Contact Google Play Support:**
   - Log into the Google Play Console.
   - Go to **Release** > **Setup** > **App integrity** > **App signing**.
   - Request a key reset and upload the `upload_certificate.pem` file.
   - Wait for Google to register the new upload key (typically takes 1-2 business days).
