# APK Build Guide — ClickFlow Android

## Prerequisites

- **JDK 17** (e.g. Temurin/OpenJDK 17).
- **Android SDK** with:
  - Platform `android-34` (compileSdk/targetSdk 34)
  - Build-tools 34.x
  - Platform-tools (for `adb`)
- Either **Android Studio** (bundles everything) or the **command-line SDK tools**.

Point Gradle at the SDK by creating `local.properties` in the project root:

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

(Android Studio writes this automatically when you open the project. `local.properties` is
git-ignored.)

## Build the debug APK

```bash
# Linux / macOS
./gradlew assembleDebug
```

```bat
:: Windows
gradlew.bat assembleDebug
```

The first run downloads Gradle 8.7 (via the wrapper) and dependencies.

## APK location

```
app/build/outputs/apk/debug/app-debug.apk
```

This APK is **debug-signed** with the default Android debug keystore. It is **not** release-signed
and must not be treated as a production artifact.

## Install on a device

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Enable USB debugging on the device first, or use an emulator (API 26+).

## Command-line SDK install (no Android Studio)

```bash
# 1. Download "Command line tools" from https://developer.android.com/studio#command-tools
# 2. Unzip to $ANDROID_HOME/cmdline-tools/latest
export ANDROID_HOME=$HOME/Android/Sdk
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH"

# 3. Accept licenses and install required packages
yes | sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4. Build
./gradlew assembleDebug
```

## Verify it is a debug (not release) APK

```bash
# The debug build uses applicationId com.clickflow.android.debug and is signed
# with the Android debug keystore (CN=Android Debug).
$ANDROID_HOME/build-tools/34.0.0/aapt dump badging \
  app/build/outputs/apk/debug/app-debug.apk | grep package
```
