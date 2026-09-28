# Server music library design

The current server patch makes Jellyfin and WebDAV tracks playable through
Spotify's Local Files player. Spotify displays them in one flat list. This
design adds an in-app Albums, Artists, and Search browser while keeping the
working Local Files stream path. Playback from the new browser remains gated
on a Pixel test of Spotify's internal play command and ordered queue.

## User flow

From **Settings and privacy > Spicetify**, the user opens **Browse server
music**. The browser shows paged Albums, Artists, and Songs views. An album
shows discs and tracks in album order. Search filters the indexed catalog on
device. If the scan has not completed, the browser shows scan progress and a
way back to server settings. Changing or forgetting the server invalidates an
open album rather than showing tracks from the previous account.

When the player bridge is verified, tapping an album track starts that track
in Spotify and queues the following album tracks. Until that proof passes,
the browser must not show a Play action or claim album queue support.

## Boundaries and shape

`ServerIndex` publishes one immutable catalog after a complete scan. The
catalog retains the existing stream IDs and content URIs for Local Files,
and adds separate album and artist identities, ordered track IDs, bounded
pages, and search. UI code consumes catalog rows; it never receives Jellyfin
JSON, WebDAV credentials, or media-source URLs.

Jellyfin's [server item model](https://github.com/jellyfin/jellyfin/blob/master/MediaBrowser.Model/Dto/BaseItemDto.cs)
defines `AlbumId`, `ArtistItems`, `AlbumArtists`,
`IndexNumber`, and `ParentIndexNumber`. The current scanner discards those
fields. Parse them at the provider boundary when present, then measure their
presence on the selected server with a redacted fixture. Keep a scoped,
deterministic fallback for missing IDs and WebDAV tags so unrelated albums
with the same title do not merge. Sort by disc and track number, with stable
fallback order for missing numbers. Keep multi-artist membership separate
from album-artist grouping. Artwork can follow through authenticated,
bounded image reads; it does not block text browsing.

The browser is an extension-owned Activity reached from the existing
Spicetify settings Activity. One version-pinned playback adapter validates
the catalog revision, resolves track IDs to Spotify local URIs, and sends an
ordered context to a proven in-process Spotify command. A delivered Android
`ACTION_VIEW` intent for a `spotify:local:` URI returned to Home without
starting the selected track on the Pixel, so it is not a playback adapter.

## Proof sequence

Each step must leave an independently testable result before the next begins.

1. Capture a bounded, redacted Jellyfin audio page and count missing album
   IDs, artist IDs, disc numbers, and track numbers. Build catalog grouping
   tests for duplicate album names, compilations, missing tags, and 35,000
   tracks. Keep the existing Local Files playback path working.
2. Trace a real Local Files row tap on Spotify `9.1.80.2221` to its player
   command. Try the same command with two known server tracks from a temporary
   in-app control. On the Pixel, verify the selected track, audible audio,
   seek, Next, and Previous. Fingerprint the exact bytecode used by the
   adapter. If this fails, keep browser playback disabled and revisit the
   handoff before presenting the feature as complete.
3. Add the paged browser and its visible settings entry. Walk from settings
   through Albums, Artists, Search, and playback. Test rescan, server change,
   restart, and same-key app update from the user-facing entry point. Record
   build, installed-byte, UI, and audible-playback evidence separately.

The current full scan took about 217 seconds for 35,274 Jellyfin items, and
the index is process-memory only. Measure restart frequency and scan cost
before choosing a credential-scoped disk cache. Do not present a successful
catalog build as proof that the installed Spotify workflow works.

## Design decision

An owned browser keeps the Spotify-specific code to one playback seam. A
Spotify-native Albums and Artists route would fit the host UI more closely,
but its private models and navigation have not been demonstrated on this
version. Temporarily projecting one album into the Local Files list reuses a
proven row tap, but changes the global list and has unproven ordering and
queue behavior. A reversible three-track projection is a useful experiment
if the player seam fails; it is not the primary user flow.
