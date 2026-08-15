# App Improvements and Local Export Implementation Plan

This plan details the solutions for the reported app issues (size, database slowness, installation conflict, folder deletion crash, and upload button alignment) and outlines the local export process.

---

## Analysis & Explanations

### 1. App Size (100 MB)
- **Why is it large?** The generated APK file is a **universal** package. It contains compiled binaries for all four Android architectures (`arm64-v8a`, `armeabi-v7a`, `x86`, `x86_64`) so that it can be installed on any device or emulator. 
- **Google Play Behavior (AAB):** When we build a **production AAB (Android App Bundle)** and upload it to the Google Play Store, Google automatically splits the bundle. Users downloading the app from the store will only download the binaries required for their specific device, resulting in a much smaller download size (typically around **15 to 25 MB**).
- **Scanner Remnants:** We searched the project files and dependencies for any remaining scanner/camera library code (such as `expo-camera`). No such packages are installed, meaning there is no bloat from the rolled-back document scanner. The size is normal for a universal Expo native app.

### 2. Installation Conflict ("لم يتم تثبيت التطبيق لأن حزمة التثبيت تتعارض مع حزمة حالية")
- **Cause:** Android prevents installing an app update if the signature of the new package does not match the signature of the currently installed app. If your phone has a version of the app signed with a debug key, or Expo's automatic keys, it will conflict with our build signed with your custom keystore (`E:\app\law_office_app\key`).
- **Solution:** You simply need to **uninstall** the existing app from your Android device first, and then install the new APK.

---

## User Review Required

> [!WARNING]
> Since we are modifying the local database schema to add the missing `reportId` column to the `documents` table, we will apply a safe SQLite migration on application startup. This will **not** delete any of your existing local data.

---

## Proposed Changes

### Database & Performance Fixes

#### [MODIFY] [core.ts](file:///e:/app/law_office_app/lib/db/core.ts)
- Increment `APP_SCHEMA_VERSION` to `21`.
- Add migration version `21` to run `ALTER TABLE documents ADD COLUMN reportId INTEGER REFERENCES police_reports(id) ON DELETE SET NULL ON UPDATE CASCADE;` (with safe try-catch blocks to ignore if columns already exist on some environments).
- Add check/creation for `executionId` and `executionLogId` in the same migration to prevent any schema discrepancies.

#### [MODIFY] [sessions.ts](file:///e:/app/law_office_app/lib/db/sessions.ts)
- Remove `await` from `alertEngine.runScanner()` calls. The alerts will scan in the background asynchronously without freezing database writes.

#### [MODIFY] [legal-work-db.ts](file:///e:/app/law_office_app/lib/db/legal-work-db.ts)
- Remove `await` from `alertEngine.runScanner()` calls.

#### [MODIFY] [expert-db.ts](file:///e:/app/law_office_app/lib/expert-db.ts)
- Remove `await` from `alertEngine.runScanner()` calls.

#### [MODIFY] [execution-db.ts](file:///e:/app/law_office_app/lib/execution-db.ts)
- Remove `await` from `alertEngine.runScanner()` calls.

### UI Upload Button Alignment

#### [MODIFY] [library-tab.tsx](file:///e:/app/law_office_app/components/legal-work/library-tab.tsx)
- Increase bottom property of the FAB floating upload button from `24` to `95` so that it floats above the bottom tab bar.

#### [MODIFY] [cloud-browser-tab.tsx](file:///e:/app/law_office_app/components/legal-work/cloud-browser-tab.tsx)
- Change tailwind class `bottom-6` to an inline style `bottom: 95` (or Tailwind class `bottom-24`) to match the library tab upload button alignment.

---

## Verification Plan

### Automated Tests
- Run `pnpm run check` to verify TypeScript compile.
- Run `pnpm run lint` to review lint warnings.

### Manual Verification
- Verify database migrations run successfully.
- Verify documents tab folder deletion no longer throws the `no such column: documents.reportId` error.
- Verify the floating upload button is aligned correctly above the bottom navigation bar.

---

## Local Export Plan

Once the fixes are approved and implemented:
1. Run local APK build:
   ```powershell
   npx eas build --platform android --profile preview --local --non-interactive
   ```
2. Run local AAB build:
   ```powershell
   npx eas build --platform android --profile production --local --non-interactive
   ```
