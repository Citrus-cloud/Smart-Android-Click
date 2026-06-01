# Android Pre-alpha Tag & Release Plan — ClickFlow Android

> Run these **only when you decide to publish**. The agent does not tag or publish automatically.

## Suggested tag

```
android-v0.1.0-prealpha
```

(Android product branch — distinct from desktop ClickFlow's `v1.0.0-alpha.1`.)

## Commands

```bash
# 1) Confirm clean tree + build
git status
./gradlew assembleDebug        # Windows: gradlew.bat assembleDebug

# 2) Commit any release prep
git add .
git commit -m "Prepare ClickFlow Android pre-alpha release"

# 3) Tag
git tag -a android-v0.1.0-prealpha -m "ClickFlow Android Pre-alpha"

# 4) Push branch + tag
git push origin main
git push origin android-v0.1.0-prealpha
```

## GitHub Release

- **Title:** ClickFlow Android Pre-alpha
- **Tag:** `android-v0.1.0-prealpha`
- **Body:** contents of `docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md`
- **Asset:** upload `app/build/outputs/apk/debug/app-debug.apk`
- **Mark as pre-release:** ✅

CLI alternative (after building the APK):

```bash
gh release create android-v0.1.0-prealpha \
  app/build/outputs/apk/debug/app-debug.apk \
  --title "ClickFlow Android Pre-alpha" \
  --notes-file docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md \
  --prerelease
```

## Pre-publish gate

Tick `docs/ANDROID_PRE_ALPHA_RELEASE_CHECKLIST.md` first: build succeeds, smoke test passes, 0
permissions, no real taps, README/CHANGELOG current.
