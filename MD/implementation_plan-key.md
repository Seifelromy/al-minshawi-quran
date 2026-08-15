# Create Permanent Release Signing Structure

This implementation plan details the steps to establish a permanent and secure release signing configuration for the Android application, without hardcoding credentials or changing the application code.

## Proposed Changes

### 1. Keystore Infrastructure

#### [NEW] [release-key.jks](file:///e:/app/al-minshawi-quran/keys/release-key.jks)
- Create a directory `keys` in the project root.
- Generate a release keystore using the standard `keytool` command with RSA 4096 and 10000 days validity:
  ```powershell
  keytool -genkeypair -v -keystore keys/release-key.jks -keyalg RSA -keysize 4096 -validity 10000 -alias release-alias -storepass [PASSWORD] -keypass [PASSWORD] -dname "CN=Al-Minshawi Quran, O=Al-Minshawi, C=EG"
  ```
- *Note*: The password will be generated securely and kept private, and it will be stored in `signing.properties`.

#### [NEW] [README_SIGNING.md](file:///e:/app/al-minshawi-quran/keys/README_SIGNING.md)
- A document containing:
  - Keystore filename: `release-key.jks`
  - Alias name: `release-alias`
  - Location used by Gradle: `keys/release-key.jks`
  - Detailed instructions for backup.
  - Detailed instructions for restoring the signing environment on another PC.

### 2. Configuration & Secrets Management

#### [NEW] [signing.properties](file:///e:/app/al-minshawi-quran/signing.properties)
- Create `signing.properties` in the project root containing:
  ```properties
  STORE_FILE=keys/release-key.jks
  STORE_PASSWORD=[SECURE_STORE_PASSWORD]
  KEY_ALIAS=release-alias
  KEY_PASSWORD=[SECURE_KEY_PASSWORD]
  ```

#### [MODIFY] [.gitignore](file:///e:/app/al-minshawi-quran/.gitignore)
- Append `signing.properties` to `.gitignore` to prevent committing secure keys and passwords to the repository:
  ```diff
  + # Release signing configuration
  + signing.properties
  ```

#### [MODIFY] [build.gradle.kts](file:///e:/app/al-minshawi-quran/app/build.gradle.kts)
- Configure Gradle to load signing credentials from `signing.properties` automatically.
- Fallback gracefully (e.g. log a warning or default to dummy/environment properties) if the file doesn't exist so that builds do not break for other environments that do not have the release signing keys.

```kotlin
  signingConfigs {
    create("release") {
      val signingPropsFile = rootProject.file("signing.properties")
      if (signingPropsFile.exists()) {
        val properties = java.util.Properties().apply {
          signingPropsFile.inputStream().use { load(it) }
        }
        val storeFilePath = properties.getProperty("STORE_FILE")
        storeFile = if (storeFilePath != null) rootProject.file(storeFilePath) else null
        storePassword = properties.getProperty("STORE_PASSWORD")
        keyAlias = properties.getProperty("KEY_ALIAS")
        keyPassword = properties.getProperty("KEY_PASSWORD")
      } else {
        // Fallback to environment variables or dummy settings
        val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
        storeFile = file(keystorePath)
        storePassword = System.getenv("STORE_PASSWORD")
        keyAlias = "upload"
        keyPassword = System.getenv("KEY_PASSWORD")
      }
    }
    ...
  }
```

## Verification Plan

### Automated Tests
- Run release build assembly:
  ```powershell
  .\gradlew.bat assembleRelease
  ```
- Run release bundle generation:
  ```powershell
  .\gradlew.bat bundleRelease
  ```

### Manual Verification
- Verify that the keystore has been successfully generated using RSA 4096 and 10000 days validity using keytool:
  ```powershell
  keytool -list -v -keystore keys/release-key.jks -storepass [PASSWORD]
  ```
