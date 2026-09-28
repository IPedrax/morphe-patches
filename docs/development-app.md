# Test settings and servers without rebuilding Spotify

The standalone **Spicetify Development** app compiles the production
extension Java sources into a separate Android app. Use it for settings,
preferences, server configuration, indexing, and file playback while editing
the extension. You do not need Spotify APKs or GitHub Packages credentials.

The app has its own preferences and server credentials. Its settings expose
every installed-patch capability for development. Spotify navigation,
sharing, Home pins, ad filtering, and the Spotify playback hook still require
a patched Spotify build. A successful preview here does not prove Spotify
integration.

## Build and install

Use Java 21, Android SDK platform 36, and an unlocked Android 8 or newer
device with USB debugging authorized. Set `JAVA_HOME` and `ANDROID_HOME`
to your installations, then run these commands from the repository root:

```sh
./gradlew -p dev-app testDebugUnitTest lintDebug assembleDebug
"$ANDROID_HOME/platform-tools/adb" devices
"$ANDROID_HOME/platform-tools/adb" -s YOUR_DEVICE_SERIAL install -r \
  dev-app/build/outputs/apk/debug/spicetify-development-debug.apk
```

Open **Spicetify Development** from the device launcher. The package is
`app.spicetify.development`; installing it leaves Spotify and its data alone.
The debug signing key stays in your normal Android development environment.
Release variants are disabled, and patch release workflows do not publish
this app.

After editing extension code, repeat `assembleDebug` and the install command.
Reopen the app after installation. `install -r` keeps the development app's
preferences when the signing key is unchanged. Changing a setting inside the
app needs no rebuild.

## Exercise settings and playback

Start from the launcher to follow the same settings entry point each time:

1. Select **Open patch settings**. Change a preference, leave settings, and
   reopen it to check persistence. Restart the app when testing startup.
2. In the server section, choose a provider and configure it:
   - For WebDAV, enter an HTTPS folder URL and its credentials, enable
     **Use server files**, and select **Save and scan**.
   - For Jellyfin, enter its HTTPS server URL, select **Use Quick Connect**,
     and approve the displayed code in a signed-in Jellyfin browser. If Quick
     Connect is unavailable, enter a username and password and select
     **Sign in with password**. Choose a music library, enable **Use server
     files**, and select **Save library and scan**. The password field clears
     after submission; the selected account session is saved privately.

   You can omit `https://` from either server URL. The app assumes HTTPS when
   no scheme is given and rejects an explicit `http://` URL.
3. Wait for the scan result, then return to the launcher and select
   **Refresh tracks**. The launcher shows up to 50 indexed tracks.
4. Select a track's **Play** button. Check audible playback, advancing time,
   **Seek forward 5 seconds**, completion, and **Stop playback**.
5. Start playback again and leave the launcher. Playback must stop. Check
   invalid credentials or an unavailable server, then restore the connection
   and scan again to check recovery.

Preview playback uses Android's `MediaPlayer` through the production private
`ServerFileProvider`, including its range reader. It stops when the launcher
leaves the foreground. It does not exercise Spotify's queue, background
playback, notifications, catalog, or codec selection.

Use **Rescan library** after changing tracks in Jellyfin. Use **Change music
library** to select another library on the saved server. Turning **Use server
files** off clears the track list while keeping the saved configuration;
**Forget server** removes the saved credentials and tracks. Server credentials
are private app data, but this is a debuggable app; use a test account and
keep real credentials out of logs, screenshots, and repository files.

## Checks and boundaries

The independent Gradle project copies the extension sources into its build
directory and replaces only `InstalledPatches` with a development capability
implementation. A generated-source task tracks production source changes.
Do not edit the generated copies.

Unit tests exercise the real settings entry point, preference initialization,
player release, callback cancellation, completion, error recovery, and stale
callbacks. CI runs these tests, Android lint, and debug APK assembly. Device
playback checks remain necessary. Follow the
[full patch workflow](patch-authoring.md#workflow-in-this-repository) before
claiming a Spotify feature works.
