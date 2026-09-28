# Optional Home pins and server files

The experimental bundle includes two optional patches in the Spicetify
settings screen. Both patches are disabled by default. Update older bundles
such as `dev.3` in Manager's **Sources** before selecting them.

## Home shortcuts

Enable **Pin shortcuts on Home** when patching. Open Spotify's Home page and
let its shortcuts load, then open **Settings and privacy > Spicetify >
Choose pinned shortcuts**. Select shortcuts and save. Restart Spotify to
refresh Home.

The patch moves selected shortcuts ahead of other shortcuts that Spotify
supplies. It does not add a playlist absent from Spotify's current Home list.
Saved pins remain available in the picker when temporarily absent from Home.
Shortcuts use Spotify URIs as identifiers. Duplicate names show their URIs in
the picker so you can distinguish them. You can save up to 64 pins.

The patch copies the native shortcut list before reordering it. It preserves
the original row objects, images, titles, analytics, and click handlers.
Eight class snapshots guard the constructor, data model, and renderers in
Spotify `9.1.80.2221`.

## Server files

Enable **Local files from a server** when patching. This feature requires
Android 8 or later and an HTTPS WebDAV folder with byte-range support.
The published `dev.4` bundle supports WebDAV. The current source also has
experimental Jellyfin settings, catalog indexing, and direct streaming;
Jellyfin has not been verified in a patched Spotify installation.

1. Open **Settings and privacy > Spicetify**.
2. Enter the full WebDAV folder URL, username, and password or app password.
3. Enable **Use server files**, then select **Save and scan**.
4. Wait for **Tracks ready**, then return to Spotify's settings. Under
   **Apps and devices**, enable **Local audio files**. Open **Local Files**
   in your library.

The URL must identify the WebDAV folder, not the server's web interface.
You can omit `https://`; the app assumes HTTPS. An explicit `http://` URL is
rejected.
Blank password fields retain a saved password only when the folder and
username stay unchanged. Changing the server or account never transfers the
old password. **Forget server** removes the saved configuration and index.
Turning **Use server files** off stops new requests and clears the track
list, including when the form contains an invalid draft URL. An in-flight
network operation can take up to its timeout to stop; its cancelled result
does not restore the track list.

Credentials stay in Spotify's private app preferences. The feature does not
log passwords or include them in content URIs. All file and redirect URLs
must stay inside the configured HTTPS origin and folder. Android validates
the server certificate. The provider is private and grants no URI access to
other apps.

Scans accept up to 500 audio files, 64 folders, six directory levels, and
2 MB per folder listing. Audio files must report a positive size of at most
2 GB. A server must return the requested HTTP 206 byte range and total size.
Unsupported responses fail instead of returning audio from the wrong offset.
An unreadable file currently fails the scan. Check the folder, credentials,
and server's range support, then scan again.

The index is scoped to the saved configuration. A scan from an old
configuration cannot publish into a new one. Audio is streamed through
Android's file-descriptor API without a persistent audio cache. The app scans
again after starting; this feature does not provide offline downloads.

### Jellyfin in local builds

Use the [standalone development app](development-app.md) to test Jellyfin
without replacing Spotify. Enter an HTTPS Jellyfin server URL, then approve
the displayed Quick Connect code in a browser already signed in to Jellyfin.
You can omit `https://` from the URL; an explicit `http://` URL is rejected.
You can use the password sign-in control if Quick Connect is unavailable.
Choose a music library, enable **Use server files**, then select **Save
library and scan**. The app waits up to nine minutes for approval; Jellyfin
expires an unapproved code after ten minutes in its current
[Quick Connect implementation](https://github.com/jellyfin/jellyfin/blob/master/Emby.Server.Implementations/QuickConnect/QuickConnectManager.cs).
Start Quick Connect again if the screen reports expiry.

Jellyfin indexing supports up to 50,000 items and reports how many items were
processed and skipped. The direct stream must support exact HTTP byte ranges.
The selected server, account, and library identify the saved catalog; access
tokens stay out of track IDs. **Rescan library** refreshes the saved library.
**Change music library** keeps the saved server session, while **Forget
server** removes it. A local-source build passed a full Music-library scan,
playback, and seeking on a Pixel 8. Spotify shows these tracks in one flat
**Local Files** playlist; album and artist browsing is not available yet.
The installed build and remaining checks are in the
[verification record](verification.md#jellyfin-in-patched-spotify-september-28-2026).

## Hide the Premium tab in local builds

The current source includes an optional **Hide Premium tab** patch. It is
not included in the published `dev.4` bundle. Build the source and select
this patch when testing it locally.

Open Spotify's **Settings and privacy > Spicetify** and change **Hide Premium
tab**. Close and restart Spotify to refresh navigation. The installed patch
hides the tab by default; turning the switch off restores Spotify's own
decision about whether to show it. The switch does not change your
subscription, suppress audio or visual ads, or hide other upgrade prompts.

The patch targets the inspected Spotify `9.1.80.2221` navigation flag
consumer and refuses changed navigation code. Runtime verification is tracked
separately in [the verification record](verification.md).

## Hide Home and Browse ads in local builds

The current source also includes an optional **Hide Home and Browse ads**
patch. It is experimental and has not been published. Select it when
patching, then open **Settings and privacy > Spicetify**. The switch starts
on. Restart Spotify after changing it.

The patch filters the identified image and video brand-ad sections before
Home and Browse build their card lists. It preserves ordinary sections and
leaves the original protobuf lists unchanged. Turning the switch off returns
the original lists. Unexpected models also return the original lists.
Audio ads, player display ads, pop-ups, and upgrade reminders need separate
patches.

The patch targets Spotify `9.1.80.2221` and rejects changed models or callers.
Static APK checks and development-app settings tests pass. An actual ad card
has not yet been observed for an on/off comparison, so live ad removal and
layout after removal remain unverified. See [advertising research](advertising-research.md)
and [the verification record](verification.md).

## Verification

The extension tests cover URL confinement, range responses, index scoping,
disabled access, protobuf serialization, pin identity and persistence, and
settings recovery. Patch-time snapshots match the inspected stock APK, and
all four patches apply together.

On a Pixel 8 with Spotify `9.1.80.2221`, Manager installation, Home ordering
after restart, unpinning, HTTPS metadata extraction, native Local Files
playback, seeking, and configuration replacement pass. The device owner
confirmed audible playback of the synthetic test track. Disabling during
folder and metadata audio reads, followed by a successful rescan, also passes.
This does not establish compatibility with every WebDAV server or another
Spotify version. See the remaining checks and artifact details in
[the verification record](verification.md).
