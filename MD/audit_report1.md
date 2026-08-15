# Architecture & Production-Readiness Audit Report

**Project:** Al-Minshawi Quran Player for Android  
**Target SDK:** 36 (Android 15 / 16 Preview)  
**Min SDK:** 24  
**Date:** June 12, 2026  

---

## Executive Summary & Special Focus Analysis

This audit report identifies critical, high, and medium severity architectural issues in the Al-Minshawi Quran Player application. While the app utilizes modern Jetpack Compose for UI and Room for caching, the core playback implementation bypasses standard Android media patterns, leading to background playback failures, Application Not Responding (ANR) occurrences, and player unresponsiveness.

### 1. Why audio playback stops after the app goes to the background
Background playback halts due to a combination of three critical factors:
*   **No Client Binding (Bypassing MediaController):** The UI and ViewModel directly access `PlayerService.exoPlayer` statically. Because the Activity does not connect to the `MediaSession` via a `MediaController` (and does not bind to the service), the Android OS treats `PlayerService` as an unbound, started service. As soon as the Activity is stopped, the OS reclaims its memory and terminates the unbound service process.
*   **Missing `WAKE_LOCK` Permission:** The player sets `.setWakeMode(C.WAKE_MODE_LOCAL)` to keep the CPU awake during playback. However, the app does not declare the `android.permission.WAKE_LOCK` permission in `AndroidManifest.xml`. When the screen turns off, the CPU goes to sleep, instantly cutting off audio streaming.
*   **No Notification Channel:** The codebase does not create or register a `NotificationChannel` (which is required on Android 8.0+ / API 26+). If the notification is blocked or fails to show, the service is not promoted to the foreground and is immediately killed in the background.

### 2. Why ANR dialogs still appear
Despite asynchronous initialization efforts, ANRs are triggered by:
*   **Infinite Nested Main Loop on Crash:** The application-level uncaught exception handler in `QuranApplication.onCreate()` runs an infinite nested `Looper.loop()` loop to prevent crashes. This corrupts the main thread message pump, leaks stack frames, and will lock or freeze the UI thread when subsequent exceptions or operations queue up.
*   **Main Thread Blocking File I/O:** The repository's `deleteDownload` method deletes files synchronously on the calling thread. In the ViewModel, this is invoked within `viewModelScope` (running on `Dispatchers.Main`), meaning disk I/O runs directly on the Main thread.
*   **Excessive Main Thread Context Switching:** During downloads, progress updates are posted to the Main thread (`withContext(Dispatchers.Main)`) for every single 8KB chunk read. For a 5MB file, this translates to over 600 context-switches in a split second, saturating the main looper's message queue and freezing the UI.
*   **Main Thread Busy Waiting:** In `AudioPlaybackManager.playSurah()`, a busy loop (`while (exoPlayer == null) { delay(50) }`) runs on the Main dispatcher. While non-blocking, polling a static variable repeatedly on the Main thread is an anti-pattern.

### 3. Why the player occasionally becomes unresponsive
*   **Concurrent Playback Initialization Race Condition:** In `AudioPlaybackManager.playSurah()`, the application launches a coroutine to fetch data, validate URLs, and start ExoPlayer, but it does *not* cancel the previous active job. If a user clicks surahs rapidly, multiple parallel coroutines run concurrently. They will all call `player.setMediaItem()` and `player.play()` out of order, causing state corruption and lockups.
*   **Static Reference Desynchronization:** Storing ExoPlayer and MediaSession statically in `PlayerService.companion object` causes desynchronization. If the service is destroyed under background constraints, the references become `null`. Any subsequent user click calls a null check or waits on the busy loop for 5 seconds before returning silently, giving the illusion of a frozen player.

### 4. Whether ExoPlayer is incorrectly tied to the Activity lifecycle
*   **Code Level:** No. ExoPlayer is created in `PlayerService.onCreate()` and released in `PlayerService.onDestroy()`, keeping it technically tied to the Service lifecycle.
*   **OS Level:** Yes. Because the Activity does not bind to the `PlayerService` using a `MediaController` or `bindService()`, the system views the service as having no active connection to the foreground UI. When the Activity is stopped, the OS deprioritizes the process and reclaims the service, effectively linking the player's survivability directly to the Activity lifecycle.

### 5. Whether MediaSessionService is implemented correctly
No, the implementation is incorrect:
*   It bypasses the core Media3 architecture by exposing static references (`PlayerService.exoPlayer` and `PlayerService.mediaSession`) instead of using `MediaController` to connect.
*   The `MediaSession` is built without custom `Callbacks`, meaning external media commands (Bluetooth buttons, Android Auto, lockscreen widgets) cannot interact correctly with the playback.
*   Static context references in the service's companion object expose the app to memory leaks.

### 6. Whether Foreground Service is fully compliant with Android 13/14
No, it is not fully compliant:
*   While the manifest declares permissions and foreground types, the service restarts using `startService()` inside `initPlayer()`. If the app is in the background when a retry or track transition occurs, `startService()` throws an `IllegalStateException` or a `ForegroundServiceStartNotAllowedException` on Android 14+.
*   A compliant service must be bound via `MediaController` (which is background-safe) and must immediately call `startForeground()` with a valid notification on a registered channel when promoted.

---

## Detailed Architectural Issues List

### Issue 1: Bypassing Media3 MediaController Architecture (Core Architectural Violation)
*   **Severity:** Critical
*   **File Path:** [AudioPlaybackManager.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/AudioPlaybackManager.kt)
*   **Class Name:** `AudioPlaybackManager`
*   **Method Name:** `exoPlayer` / `mediaSession` properties and `initPlayer()`
*   **Exact Reason:** Accesses `PlayerService` variables directly via a static companion object. Because the UI/ViewModel does not bind to the `PlayerSession` via `MediaController`, the Android OS treats the service as an unbound background service, killing it immediately when the Activity goes to the background.
*   **Recommended Fix:** 
    1. Remove the static companion references in `PlayerService`.
    2. Rewrite `AudioPlaybackManager` to build and hold a `MediaController` instance asynchronously using `MediaController.Builder(context, sessionToken)`.
    3. Route all player actions (play, pause, seek, listeners) through the `MediaController` instance.

### Issue 2: Infinite Nested Main Looper Loop in Uncaught Exception Handler
*   **Severity:** Critical
*   **File Path:** [QuranApplication.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/QuranApplication.kt)
*   **Class Name:** `QuranApplication`
*   **Method Name:** `onCreate()` (Uncaught Exception Handler Block)
*   **Exact Reason:** When an exception is thrown on the main thread, the handler traps it and enters a `while(true) { Looper.loop() }` nested loop. This corrupts the main thread message pump, leaks stack frames, and will freeze the UI thread when subsequent events are processed, leading directly to ANRs instead of allowing a clean crash.
*   **Recommended Fix:** Remove the nested looper loop from the uncaught exception handler. Allow the exception to crash the process cleanly, and integrate a proper crash logging utility (e.g. Firebase Crashlytics).

### Issue 3: Missing WAKE_LOCK Permission in AndroidManifest
*   **Severity:** High
*   **File Path:** [AndroidManifest.xml](file:///e:/app/al-minshawi-quran/app/src/main/AndroidManifest.xml)
*   **Class Name:** N/A
*   **Method Name:** N/A
*   **Exact Reason:** The player is configured to use `setWakeMode(C.WAKE_MODE_LOCAL)` in `PlayerService.onCreate()`, but the app fails to declare `<uses-permission android:name="android.permission.WAKE_LOCK" />` in the manifest. When the device screen is off, the CPU goes to sleep, halting background streaming.
*   **Recommended Fix:** Add `<uses-permission android:name="android.permission.WAKE_LOCK" />` to [AndroidManifest.xml](file:///e:/app/al-minshawi-quran/app/src/main/AndroidManifest.xml).

### Issue 4: Missing Notification Channel Initialization
*   **Severity:** High
*   **File Path:** [PlayerService.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/PlayerService.kt)
*   **Class Name:** `PlayerService`
*   **Method Name:** `onCreate()`
*   **Exact Reason:** The app does not create a `NotificationChannel` (required on API 26+). If the notification is blocked or fails to show, the service is not promoted to the foreground and is immediately terminated by the OS when the app is backgrounded.
*   **Recommended Fix:** Create a notification channel in `PlayerService.onCreate()` or `QuranApplication.onCreate()` using `NotificationManager.createNotificationChannel`. Specify the channel ID in the `MediaSessionService` configuration.

### Issue 5: Background Service Start Violations (`ForegroundServiceStartNotAllowedException`)
*   **Severity:** High
*   **File Path:** [AudioPlaybackManager.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/AudioPlaybackManager.kt)
*   **Class Name:** `AudioPlaybackManager`
*   **Method Name:** `initPlayer()`
*   **Exact Reason:** The app calls `startService()` inside `initPlayer()`. If the service is destroyed and the user triggers play (e.g. via Bluetooth receiver, alarm, or background queue) when the app is in the background, this throws `ForegroundServiceStartNotAllowedException` on Android 14+. The exception is caught, but the player remains null, causing playback to fail.
*   **Recommended Fix:** Bind to the service via `MediaController` which is background-safe, or start the service using `ContextCompat.startForegroundService()` and call `startForeground()` within `onCreate` or `onStartCommand` of the service immediately.

### Issue 6: Concurrent Playback Initialization Race Condition
*   **Severity:** High
*   **File Path:** [AudioPlaybackManager.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/AudioPlaybackManager.kt)
*   **Class Name:** `AudioPlaybackManager`
*   **Method Name:** `playSurah()`
*   **Exact Reason:** `playSurah()` launches a coroutine on an application-level scope but does not cancel previous active play jobs. If the user clicks surahs rapidly, multiple parallel coroutines run concurrently. They will all call `player.setMediaItem()` and `player.play()` out of order, causing state corruption and player lockups.
*   **Recommended Fix:** Store a reference to the active `Job` in `AudioPlaybackManager` (e.g., `private var playbackInitJob: Job? = null`) and call `playbackInitJob?.cancel()` at the beginning of `playSurah()`.

### Issue 7: Blocking Main Thread File I/O Operations
*   **Severity:** Medium
*   **File Path:** [QuranRepository.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/repository/QuranRepository.kt)
*   **Class Name:** `QuranRepository`
*   **Method Name:** `deleteDownload()`
*   **Exact Reason:** Deletes files synchronously using `file.delete()` inside `deleteDownload()`. When called from `QuranViewModel.deleteDownload()`, this runs on the default `viewModelScope` dispatcher (`Dispatchers.Main`), blocking the main thread for disk operations.
*   **Recommended Fix:** Wrap the file system deletion logic in `withContext(Dispatchers.IO)`.

### Issue 8: Flooding Main Thread with Download Progress Updates
*   **Severity:** Medium
*   **File Path:** [QuranViewModel.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/ui/QuranViewModel.kt)
*   **Class Name:** `QuranViewModel`
*   **Method Name:** `downloadSurah()`
*   **Exact Reason:** Updates `_downloadProgress` map on the Main thread using `withContext(Dispatchers.Main)` for *every single 8KB buffer chunk read*. This triggers hundreds of context-switches per second, flooding the main looper and freezing the UI.
*   **Recommended Fix:** Throttle progress updates to post at most every 100-200ms, or when progress increases by >= 1%.

### Issue 9: Main Thread Busy Waiting
*   **Severity:** Medium
*   **File Path:** [AudioPlaybackManager.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/AudioPlaybackManager.kt)
*   **Class Name:** `AudioPlaybackManager`
*   **Method Name:** `playSurah()`
*   **Exact Reason:** Runs a busy waiting loop checking `exoPlayer == null` on the Main dispatcher. While the coroutine suspends (`delay(50)`), running a polling loop on the Main thread is an anti-pattern.
*   **Recommended Fix:** Remove the polling loop. Rely on standard `MediaController` connection callbacks, or notify the manager from the service when the player is ready.

### Issue 10: Static References to ExoPlayer and MediaSession (Memory Leak Risk)
*   **Severity:** Medium
*   **File Path:** [PlayerService.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/PlayerService.kt)
*   **Class Name:** `PlayerService`
*   **Method Name:** `companion object`
*   **Exact Reason:** Storing references to context-heavy instances of `ExoPlayer` and `MediaSession` in static variables. If the service is destroyed and recreated but the static fields are not cleared or if references are retained elsewhere, it causes severe memory leaks.
*   **Recommended Fix:** Remove the companion object's static variables. Manage the player and session instances purely within the service instance, and let clients interact via `MediaController`.

### Issue 11: Battery Drain from Frequent Database Writes
*   **Severity:** Low
*   **File Path:** [QuranViewModel.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/ui/QuranViewModel.kt)
*   **Class Name:** `QuranViewModel`
*   **Method Name:** `init` (State collection block)
*   **Exact Reason:** Progress is written to Room SQLite database every 5 seconds. Continuous disk writes prevent the flash storage and device CPU from entering low-power states, causing premature battery drain.
*   **Recommended Fix:** Increase the write interval to 15-30 seconds, or write only when the player is paused, stopped, or backgrounded.

### Issue 12: Network Connectivity Check Missing
*   **Severity:** Low
*   **File Path:** [AudioPlaybackManager.kt](file:///e:/app/al-minshawi-quran/app/src/main/java/com/example/data/audio/AudioPlaybackManager.kt)
*   **Class Name:** `AudioPlaybackManager`
*   **Method Name:** `validateAndRankSources()`
*   **Exact Reason:** Initiates pings and requests network resources without checking if the device is connected to the internet. If the device is offline, all connections will block and fail, leading to latency and errors.
*   **Recommended Fix:** Check network connectivity before attempting to call `validateAndRankSources()`. If offline, fall back to offline playback or show a connection warning instantly.
