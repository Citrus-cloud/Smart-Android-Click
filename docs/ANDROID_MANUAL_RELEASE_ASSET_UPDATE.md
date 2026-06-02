# Android Pre-alpha — Manual Release Asset Update (Step 60)

## What this guide is for

Step 60 of the ClickFlow Android project replaces the **`app-debug.apk`** asset on the existing
GitHub pre-release [`android-v0.1.0-prealpha`](https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha)
with a freshly built debug-signed APK that ships the Step 59 Simple Clicker UX.

The release tag, URL, title, and "pre-release" flag are **preserved**. Only the binary asset and
release body are refreshed. No release signing is introduced; the project stays debug-only.

There are two supported paths:

- **A. Automated path** via the parked GitHub Actions workflow `ci/release/android-release.yml`
(preferred — but requires a one-time move because the assistant's OAuth grant lacks the
`workflow` scope).
- **B. Fully manual path** (works without GitHub Actions; build locally and upload via
`gh release upload` or the web UI).

Both paths assume a clean `main` checkout at or after the Step 60 commits.

---

## Pre-flight checks (run these before either path)

From the repo root, on `main`:

```bash
# 1. Confirm versionName is the pre-alpha.
grep 'versionName' app/build.gradle.kts
#   expected:  versionName = "0.1.0-prealpha"

# 2. Confirm the manifest still declares zero permissions and zero components.
grep -E '<uses-permission|<provider|<service|<receiver' app/src/main/AndroidManifest.xml || \
echo "OK: 0 permissions, 0 providers, 0 services, 0 receivers."

# 3. Confirm the safety gate still blocks real taps.
grep -E 'canRunRealTap|attemptRealTap' \
app/src/main/java/com/clickflow/android/safety/SafetyGate.kt
#   expected: both return false unconditionally.
```

If any of these checks fail, **stop** and resolve before publishing. Step 60 must not change the
safety posture.

---

## Path A — Automated via GitHub Actions

The workflow source lives at `ci/release/android-release.yml`. It is parked there because the
assistant's GitHub OAuth grant does not include the `workflow` scope, which GitHub requires for
any write inside `.github/workflows/`.

### One-time activation

Run from your machine (any environment with `git` and push access to `main`):

```bash
git checkout main
git pull
mkdir -p .github/workflows
git mv ci/release/android-release.yml .github/workflows/android-release.yml
git commit -m "Step 60: activate android-release workflow"
git push origin main
```

After the push, the workflow appears in the repo's **Actions** tab as `android-release`.

### Running the workflow

- **Manually:** Actions → `android-release` → **Run workflow** → pick branch `main` →
leave inputs at their defaults → **Run**.
- **By tag push:** any push to a tag matching `android-v*-prealpha` triggers it. To rebuild
the asset without changing the tag, you can re-push the existing tag:

```bash
git tag -f android-v0.1.0-prealpha
git push -f origin android-v0.1.0-prealpha
```

(Forced tag re-push is what wakes the workflow; the underlying commit `b9cb875` does not move.)

The workflow:

1. Checks out the chosen ref.
2. Sets up JDK 17 + Android SDK (platform 34, build-tools 34.0.0).
3. Runs `./gradlew assembleDebug`.
4. Re-validates the manifest (0 perms / 0 providers / 0 services / 0 receivers) and
 `versionName = "0.1.0-prealpha"`.
5. Deletes the old `app-debug.apk` asset on `android-v0.1.0-prealpha` and uploads the rebuilt
 `app-debug.apk`. The pre-release flag is preserved (workflow refuses to upload if the release
 is not a pre-release).

If any safety re-check fails, the workflow exits non-zero and does not upload.

---

## Path B — Fully manual (no GitHub Actions)

If you don't want to activate the workflow, you can produce and upload the APK by hand.

### B.1 Build the APK locally

Requirements: JDK 17, Android SDK 34, Android build-tools 34.0.0.

```bash
git checkout main
git pull
./gradlew --no-daemon clean assembleDebug
```

Expected output:

```
app/build/outputs/apk/debug/app-debug.apk
```

The APK is debug-signed; no release keystore is involved.

### B.2 Verify the build before uploading

```bash
# File exists and is non-empty.
ls -la app/build/outputs/apk/debug/app-debug.apk

# Optional: confirm the APK's internal versionName via aapt2 (from build-tools 34.0.0).
$ANDROID_HOME/build-tools/34.0.0/aapt2 dump badging \
app/build/outputs/apk/debug/app-debug.apk | grep -E 'package: |versionName'
#   expected to contain  versionName='0.1.0-prealpha'  (the .debug suffix on applicationId is normal).

# Optional: confirm zero declared permissions in the built APK.
$ANDROID_HOME/build-tools/34.0.0/aapt2 dump permissions \
app/build/outputs/apk/debug/app-debug.apk
#   expected: no lines starting with "uses-permission:".
#   (AndroidX may inject a self-scoped *.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION
#    declaration at build time on targetSdk 34. That is a self-declared, signature-scoped
#    permission, NOT a dangerous runtime permission. See ANDROID_BUILD_TROUBLESHOOTING.md.)
```

### B.3 Replace the asset via `gh` CLI

Requirements: GitHub CLI authenticated with write access to the repo.

```bash
TAG="android-v0.1.0-prealpha"
APK="app/build/outputs/apk/debug/app-debug.apk"

# Sanity check: the release must still be a pre-release.
gh release view "$TAG" --json isPrerelease --jq '.isPrerelease'
#   expected: true

# Replace the asset (the --clobber flag deletes and re-uploads any existing
# asset with the same name).
gh release upload "$TAG" "$APK" --clobber
```

### B.3 (alternative) Replace the asset via the web UI

1. Open https://github.com/Citrus-cloud/Smart-Android-Click/releases/tag/android-v0.1.0-prealpha
2. Click **Edit** (pencil icon on the release).
3. Delete the existing `app-debug.apk` asset.
4. Drag the freshly built `app/build/outputs/apk/debug/app-debug.apk` onto the asset uploader.
5. Confirm **"This is a pre-release"** is still checked.
6. Click **Update release**.

### B.4 Refresh the release body (optional, recommended)

Copy the contents of `docs/ANDROID_PRE_ALPHA_RELEASE_DRAFT.md` into the release description on
GitHub (Edit release → paste → Update release). The draft already calls out the Step 59 Simple
Clicker UX.

---

## Post-publish QA

Follow `docs/ANDROID_PRE_ALPHA_POST_RELEASE_CHECKLIST.md` — the Step 60 section covers download,
install on a clean device, marker drag, Start/Stop/Emergency, persistence across restart, Audit
Log content, and Safety Center wording.

---

## Rollback

If anything looks wrong after the asset swap:

```bash
TAG="android-v0.1.0-prealpha"

# Delete the bad asset and re-upload the previous app-debug.apk
# (the original Step 58 build at commit b9cb875, 15,761,368 bytes).
gh release upload "$TAG" path/to/old-app-debug.apk --clobber
```

The tag and pre-release status are never deleted; only the asset is swapped. There is no risk to
the underlying tag or the release URL.

---

## What this update intentionally does NOT do

- Does not introduce release signing or a keystore.
- Does not change `versionName` or `versionCode` (still `0.1.0-prealpha` / `1`).
- Does not enable real taps, Accessibility, MediaProjection, or any overlay.
- Does not declare any new manifest permissions, providers, services, or receivers.
- Does not flip the pre-release flag off.
- Does not publish to Google Play or any store.
