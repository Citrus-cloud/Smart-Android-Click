# Android Build Troubleshooting — ClickFlow Android

The debug APK builds cleanly with JDK 17 + Android SDK (compileSdk 34). This doc lists requirements
and fixes for common issues. A verified headless toolchain setup is included at the end.

## Requirements

- **JDK 17** (Temurin/OpenJDK 17). JDK 21 also works; JDK 8/11 will not.
- **Android SDK** with `platforms;android-34`, `build-tools;34.0.0`, `platform-tools`.
- **Gradle 8.7** — provided by the wrapper (`./gradlew`); do not install Gradle manually.

## Point Gradle at the SDK

Create `local.properties` in the project root (git-ignored):

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Android Studio writes this automatically when you open the project.

## Common errors

| Symptom | Cause | Fix |
|---|---|---|
| `SDK location not found` | no `local.properties`/`ANDROID_HOME` | set `sdk.dir` or `ANDROID_HOME` |
| `Failed to find Build Tools revision 34.0.0` | build-tools missing | `sdkmanager "build-tools;34.0.0"` |
| `Failed to find target with hash string 'android-34'` | platform missing | `sdkmanager "platforms;android-34"` |
| `Could not determine java version` / unsupported class | wrong JDK | use JDK 17 (`JAVA_HOME`) |
| `License for package ... not accepted` | SDK licenses | `yes \| sdkmanager --licenses` |
| Gradle download blocked | no network on first run | run once with network, or pre-seed `~/.gradle` |

## SDK licenses

```bash
yes | sdkmanager --licenses
```

## Windows commands

```bat
gradlew.bat assembleDebug
```

## APK output path

```
app/build/outputs/apk/debug/app-debug.apk
```

## Verified headless toolchain (no Android Studio)

This exact sequence produced a successful `assembleDebug` (Linux x64):

```bash
# 1) JDK 17 (Temurin tarball)
curl -fsSL -o jdk17.tar.gz \
  "https://api.adoptium.net/v3/binary/latest/17/ga/linux/x64/jdk/hotspot/normal/eclipse?project=jdk"
tar -xzf jdk17.tar.gz
export JAVA_HOME="$PWD/jdk-17.0.19+10"      # adjust to extracted dir
export PATH="$JAVA_HOME/bin:$PATH"

# 2) Android command-line tools
export ANDROID_HOME="$HOME/android-sdk"
mkdir -p "$ANDROID_HOME/cmdline-tools"
curl -fsSL -o cmdtools.zip \
  "https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip"
unzip -q cmdtools.zip -d "$ANDROID_HOME/cmdline-tools"
mv "$ANDROID_HOME/cmdline-tools/cmdline-tools" "$ANDROID_HOME/cmdline-tools/latest"
export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"

# 3) SDK packages + licenses
yes | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
sdkmanager --sdk_root="$ANDROID_HOME" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 4) Point the project at the SDK and build
echo "sdk.dir=$ANDROID_HOME" > local.properties
./gradlew --no-daemon assembleDebug
```

Build time on a clean cache: ~4 min (Gradle + dependency download), ~50 s incremental.
