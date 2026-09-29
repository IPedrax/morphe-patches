# Verification record

The initial target is experimental. This record separates source checks from
patching, installation, and Android runtime evidence. Unchecked items remain
release blockers.

## Target and tools

The stock fixture was copied from an installed Android application on
September 17, 2026. Only APK files were copied, without account or app data.

| Field | Value |
| --- | --- |
| Package | `com.spotify.music` |
| Version | `9.1.80.2221` |
| Version code | `145767611` |
| ABI | `arm64-v8a` |
| Stock base APK SHA-256 | `3dc0c561236d4dc01c17acc12dd219914fa2de61992a3d1428ff6ee677cd45ee` |
| Stock signing certificate SHA-256 | `6505b181933344f93893d586e399b94616183f04349cb572a9e81a3335e28ffd` |
| Build JDK | Temurin 21 |
| Morphe Patcher | `1.13.0` |
| Morphe Gradle plugin | `1.3.4` |
| Morphe Desktop | `1.16.0` |

## Evidence

Static inspection found one current share URL builder with the expected
parameter signature and tracking strings. The historical `ShareUrl(url=`
constructor fingerprint no longer matches this build. All ten selected
theme resource names exist in the stock resource table.

The bundle builds with Java 21. Five Java URL tests, 22 Kotlin resource
cases, and three option-submission cases pass. Android extension lint passes.
Generated metadata contains exactly two public patches, with the expected
options, defaults, ARM64 version code, and experimental target.

Morphe Desktop 1.16.0 patched the sharing-only, theme-only, and combined
profiles successfully. Independent artifact checks verify 1,145 default
color entries and resource IDs. Theme profiles change the ten selected
colors; other default colors stay equivalent. Where resource rebuilding
relocates XML selectors, the checker compares decoded XML contents.
The sharing helper and its call appear exactly once when enabled and are
absent from the theme-only output. Android signature verification passes.
These runs use FULL bytecode mode. The input base APK hash is listed above.
The combined output rebuilt after the option-validation fix has SHA-256
`f82cfe6de544ffddd6c90f3addfe7360e22a2056a93e0a4e7c5fea73520bcbc7`.

Selecting the sharing patch for the unrelated Manager package applies zero
patches. Desktop still signs an unchanged output and returns success in that
case. A CLI exit code alone is insufficient; check the report's applied patch
names and run the artifact verifier.

An invalid color initially exposed Morphe's option setter retaining defaults
after validator errors. Commit `56c4cc0` moves format validation into patch
execution. The repeated CLI test now exits 1, reports the invalid format, and
produces no APK. Regression tests use Morphe's actual option setter for all
three fields.

On September 18, 2026, `scripts/verify-failures.py` checked the published
`v1.0.0-dev.1` bundle against disposable copies of the stock base APK. Removing
the sharing fingerprint's string marker reports zero share URL builders.
Renaming `dark_base_background_base` reports that missing resource. Submitting
an invalid value for each of the three color options reports the option's
format requirement. All five cases exit 1, identify the expected failed patch,
and produce no output APK. These altered fixtures simulate input drift; they
do not establish compatibility with another Spotify version or verify Manager's
failure screens.

A source review found a verifier gap: a same-name call with a mismatched
method descriptor could pass. Synthetic DEX tests reproduced that false pass.
The verifier now requires the exact String-to-String descriptor and a public
static helper. It accepts the valid fixture and rejects four malformed cases.

Initial Android 16 emulator startup was overloaded and showed a Spotify ANR.
Stock Spotify subsequently reached its welcome screen in the same emulator.
The combined patched APK installs and reaches the same welcome-screen
accessibility controls on a controlled retry. Hardware-rendered screenshots
showed a black app area. Restarting the emulator with SwiftShader and Vulkan
disabled restored visible Manager rendering. Spotify's welcome and login
controls respond through accessibility. Its login activity sets Android's
`SECURE` window flag, so black captures there do not prove a rendering bug.
Spotify's visual color checks and logged-in behavior remain unverified.

Morphe Manager 1.31.1 imports the Android bundle and lists both patches.
Enabling **Experimental app versions** makes Spotify visible. The default
flow accepts the stock split archive and starts the sharing patch. Cancelling
that job and confirming **Stop patcher** returns to the app list.

An earlier local import contained JVM classes without `classes.dex`, and
Manager reported zero patches. Replacing that stale file with the
`buildAndroid` output resolved it. A `.mpp` filename alone does not establish
Android compatibility.

## Published prerelease

[CI run 35241568299](https://github.com/spicetify/morphe-patches/actions/runs/35241568299)
passed tests, extension lint, bundle compilation, release, and attestation.
It published [v1.0.0-dev.1](https://github.com/spicetify/morphe-patches/releases/tag/v1.0.0-dev.1)
as a prerelease from source commit `22b1524`. Stable publishing remains gated.

[CI run 35247182789](https://github.com/spicetify/morphe-patches/actions/runs/35247182789)
also passes on `07fd462`. Both runs were explicitly dispatched. Subsequent
SSH pushes did not create Actions runs during this test, despite enabled
workflows and matching branch triggers. Automatic push-triggered release
admission remains unverified.

The downloaded `patches-1.0.0-dev.1.mpp` contains `classes.dex` and
`extensions/spotify.mpe`. Its SHA-256 is
`fec60b3452aa9af9f195d2ccc544015cc2f04444c7ef1352052507b2590d76fa`.
`gh attestation verify` succeeds for this file and repository. Manager downloads
this release from the following feed and lists both patches:

```text
https://raw.githubusercontent.com/spicetify/morphe-patches/refs/heads/dev/patches-bundle.json
```

The explicit `refs/heads/dev` URL keeps this feed on experimental releases.
Enable **Experimental app versions** in the source's controls to show Spotify.
The remote source was tested with the duplicate local source disabled.
In **Expert mode**, both patches were selected with default colors. Manager
merged the five stock splits, patched with its default **Fast** bytecode mode
and 896 MB process limit, and reported success. Its installation flow detected
the existing Desktop-signed test app, explained the certificate conflict and
data loss, and offered uninstall. After uninstalling that unauthenticated test
app, Android installed the Manager-built APK and Manager marked it installed.

The installed APK was copied back from the emulator and independently checked.
Its sharing call and helper, all 1,145 default colors and resource IDs, the ten
expected theme changes, and Android signing verification pass. Its SHA-256 is
`23dcb4098406f60c70bb6657f22ece21d524f6f36332afe0401e5f57f69b43b3`.
The Manager-built app reaches its welcome and login screens after an emulator
restart. A later source version update and same-key reinstall still need
verification.

## Physical-device preparation

On September 18, 2026, Morphe Manager 1.31.1 on a Pixel 8 running Android 17
downloaded `v1.0.0-dev.1` from the experimental feed and listed both patches.
After enabling **Experimental app versions**, its default flow accepted the
stock split archive through Android's file picker and built the clean-sharing
patch. Manager did not need **All files access** for this flow.

The APK exported through Manager's **Save** action has SHA-256
`3e89dcc081a4da519821156734b54dba95851299e9bd156e728bffe4466d9653`.
Independent checks confirm one sharing call and helper, all 1,145 default color
values and IDs unchanged, and a valid APK signature. This is a phone-built
artifact, not evidence of installation or authenticated runtime behavior.

After explicit approval of local-data loss, Manager uninstalled stock Spotify
and started installing this APK. Android's Play Protect prompt requested
biometric confirmation for a one-time installation without scanning. After
the device owner confirmed, installation completed. No Play Protect setting
was disabled.

The installed package reports Spotify `9.1.80.2221`, version code `145767611`,
and `arm64-v8a`. Its sole installed APK has the same SHA-256 as the exported
Manager build above. Android accepted the normal launcher intent. The device
owner signed in successfully.

## Signed-in phone tests and sharing fix

The installed `v1.0.0-dev.1` build loads Home and an album page. Playback
advances through tracks, the queue opens, and Android reports local playback.
Playback continues after leaving Spotify. Android's media card pauses and
resumes playback, with matching `PAUSED` and `PLAYING` session states. The
test ended paused. The device owner confirmed normal audible playback.
The device picker reaches the nearby-devices permission explanation; no
permission was granted and no Connect transfer was tested.

The normal album flow, **Share > More sharing options**, exposed a defect:
Android's share preview still contained `si` and
`utm_source=native-share-menu`. Static inspection found a server URL generator
that bypasses the patched local builder. Both generators produce a common
result with separate shareable URL, share ID, Spotify URI, and full URL fields.
The native share intent reads that result's URL. This proves the bypass exists;
the phone's exact generator branch was not instrumented.

The fix also sanitizes the two final URL arguments before storage, preserving
the separate share ID and Spotify URI. Resolution follows named protobuf
fields and their getter-to-constructor dataflow, and rejects incompatible or
ambiguous layouts. The original local hook remains for its other caller.

The updated artifact checker rejects the old phone APK because both final URL
hooks are absent. A newly patched local APK passes all three hook checks,
signature verification, and preservation of all 1,145 default colors and IDs.
Its SHA-256 is
`cea209048243490de3dcbc07f85df40fc1c01002c5175ca3644882006d97aeaa`.
A combined build using Fast mode and an 896 MB heap also passes, including
the ten expected default theme changes. Its SHA-256 is
`cb5068005b5a3ee6e9bc266ea2e6780872d61ed9d741ca597b6d11ae19ae32e3`.
Seven resolver tests, twelve verifier cases, the existing unit suite, and six
actual-APK refusal cases pass. Phone verification follows below.

## Verified dev.2 update on Pixel 8

The fix shipped in experimental release `v1.0.0-dev.2`. Its published bundle
has SHA-256
`9d12f13ef91afde27bdeba029735acf6994f07a8b2a06e5dd1f2d6fa296143d5`.
The release tag contains fix commit `c01bddb`, and GitHub build provenance
verification passed. Release run `35332792343` passed tests, publishing, and
attestation. The push did not start Actions; manual workflow dispatch was
required again. Stable publishing remains disabled.

Manager's source update control fetched `dev.2`. The existing Spotify entry's
**Patch** action rebuilt the sharing-only profile, and **Save** exported it.
The export passed all three hook checks, signature verification, and all 1,145
unchanged default color values and IDs. Its signing certificate matches the
previous Manager build. Installing through Manager updated Spotify without
an uninstall. The installed APK exactly matches the verified export:
`28e4f09b2cf395afce4ef3dd36e36aaa61c3c1ffc8f9a489ba6112777ea3c6c2`.

The signed-in Home screen and paused player state survived the update. The
device owner confirmed the updated app works. Android's share preview showed
the expected album, track, and playlist destinations with no query parameters.
The playlist preview includes Spotify's introductory text before the clean
URL. No recipient was selected and no message was sent. Episode sharing,
timestamp preservation, copied-link contents, and opening the resulting links
still need runtime checks. Device interaction stopped after the owner's
confirmation. The nearby-device prompt was declined; Connect remains untested.

CI also uploaded a bundle with the previous version's filename alongside the
correct `dev.2` asset. The extra asset was removed; the metadata feed always
pointed to the correct bundle. Commit `1caf103` removes validation-build bundles
before release version selection. Workflow run `35333603978` passed. The
`dev.3` release below confirms only the correctly versioned asset was uploaded.

## Local settings implementation

The local settings build adds a native **Spicetify** row before **Log out**,
a private Activity, persistent clean-sharing preferences, and installed-patch
capability flags. The theme-only profile bundles the shared extension but has
zero sharing hooks. Older evidence above describes the release tested at that
time, including the previous helper-absence check.

Build, unit tests, extension lint, and all three patch profiles pass. The
sharing verifier checks one local and two result hooks into `onShareUrl`, plus
the branch that preserves the original URL when cleanup is disabled. Its 26
regression cases pass. Four preference tests and two native snapshot tests
pass. Independent settings DEX checks verify application initialization, menu
insertion, capability constants, the reserved analytics mapping, bridge
references, and navigation signatures. Fifteen mutated artifact cases are
rejected. Manifest inspection confirms one non-exported settings Activity,
no intent filter, and unchanged app permissions.

Seven actual-APK refusal cases pass with no output file. Altering the root
settings marker fails the `xlt` snapshot. Altering the sharing marker fails
the earlier `ion` snapshot because this obfuscated class also contains the
marker. Changed server response fields and all existing theme refusal cases
still report their expected failures.

The README source button uses `github=spicetify/morphe-patches/tree/dev`.
Official website code preserves the branch, and Manager 1.31.1 converts it to
the raw dev metadata URL and enables prereleases. Isolated execution of the
website script confirms the generated intent. On the Pixel, the deployed
page opened in Firefox and **Open in Morphe** reached Manager's **Add source**
confirmation with the correct GitHub repository and `dev` branch. Cancelling
preserved the existing source without adding a duplicate.

Manager 1.31.1 with Patcher 1.14.0 built the combined local settings bundle
on the Android 16 emulator using **Fast** mode and its cached original APK.
The same-key installation preserved app data, and Android's bytecode
verification returned **Success**. The installed APK independently passes
the sharing, settings, manifest, signature, and 1,145 color-resource checks.
Its SHA-256 is
`916f8d56ae85fc7674f6dd4608083cba15b12fad20e293eb5e6a9d7ee2b09239`.
Spotify reaches its login screen, so this run does not verify the signed-in
settings menu.

The first settings release attempt passed tests and bundle compilation but
failed while generating the source archive. Commit `740d7a9` makes Gradle
track the native bridge producer for every resource consumer. Running
`generatePatchesList buildAndroid` together then passed locally.

[Release run 35339516642](https://github.com/spicetify/morphe-patches/actions/runs/35339516642)
passed tests, Android lint, source packaging, publishing, and attestation.
The `v1.0.0-dev.3` tag contains settings commit `7822e51` and packaging fix
`740d7a9`. Only the correctly versioned bundle was uploaded. Its SHA-256 is
`5a36f432cc717e08bb6a3b2998a54537d7ad1e6809d65f79ea0a9d31836f8eba`.
GitHub provenance verification passes, and the dev feed points to this asset.
Manager's existing source update control fetched `dev.3` on the Pixel.
This run was manually dispatched; stable publishing remains disabled.

The first dedicated review attempt could not complete because reviewer
capacity was unavailable. A later six-part review completed against `757d279`,
covering correctness, tests, maintainability, security, reliability, and
adversarial cases. It found no confirmed production-code defect. It reproduced
an output-verifier gap: replacing the menu-insertion method with `return-void`
still passed. A separate probe also accepted a missing settings Activity.

The verifier now requires the Activity and its constructor, lifecycle method,
and launch method. It resolves extension-owned bridge references and compares
all four complete installed bridge classes with the exact patch bundle's
canonical `extensions/settings.dex`. It serializes each class separately to
avoid differences from APK-level DEX layout. The artifact report records the
bundle hash as well as the APK hashes. Matching the bundle complements source
review and runtime testing; it does not prove arbitrary bundle code is correct.

Both false passes were reproduced before the fixes. All 22 mutation cases now
pass, including empty menu and navigation methods, missing Activity methods,
and the previous hook and ABI cases. Real Manager and Desktop outputs match
the published bundle's bridge. A focused follow-up review independently reran
the 22 cases and confirmed the argument wiring and refusal of a bundle without
the bridge. Both findings are resolved. No external code-review service was
used.

## Verified dev.3 settings on Pixel 8

Manager 1.31.1 fetched `dev.3` through the existing source update control.
Spotify's **Patch** action rebuilt the default sharing-only profile from the
cached original APK. The exported APK passed the independent sharing, settings,
manifest, signature, and 1,145 unchanged color-resource checks. Its certificate
matches the installed `dev.2` build. Installing through Manager updated the
app without uninstalling it and preserved the signed-in Home screen.

The installed APK matches the verified export byte for byte, with SHA-256
`3099526fa835387c3ba6f16a2ba8729e20bdcb4e291ebb6f33ca2af486c6962a`.
The device remains Pixel 8, Android 17, ARM64, Spotify `9.1.80.2221`.

The normal **Settings and privacy** list shows one **Spicetify** row above
**Log out**. It opens the private Activity with **Clean sharing links** enabled
and no theme control, as expected for this profile. Toolbar Back and Android
Back return to Spotify. Repeated opening and a full process restart retain
one row. Spotify's existing **Playback** settings still open normally.

Turning cleanup off makes Android's share preview for the current track retain
`si` and `utm_source`. Reopening the menu and restarting Spotify preserve the
off state. Turning cleanup on immediately produces the same track destination
without query parameters, with no further restart. No recipient was selected
and no message was sent. The final switch state is enabled.

The settings screen remains responsive during playback. Android reports
`PLAYING`, and the player proceeds from the original track through ads to
another track. Playback was paused at the end. Portrait captures at normal
and 150% text size, plus landscape at 150%, show readable labels, no overlapping
controls, and content clear of system bars. The switch retains its state
through Activity recreation and exposes a labeled, checkable Android switch
to accessibility.
The original font size and portrait rotation were restored. TalkBack speech
was not tested.

Manager's Expert mode then built a combined profile using Patcher 1.14.0,
**Fast** mode, a 1024 MB process limit, and the cached original APK. Only this
source's two patches were selected. The exported APK passed the updated
artifact checker, including exact bridge comparison, all ten expected theme
changes, and unchanged app permissions. Its SHA-256 is
`44c018ec587245c18d215c4d93439110d2795560169fdea63a374a4a1a33413d`.
Installation again preserved login. The sharing switch had been set off
before updating and remained off afterward, proving preference retention
across a same-key settings update. The combined menu shows both the switch
and the theme repatching instructions, with readable labels and spacing.

Manager also built and installed the theme-only profile. Its exported APK has
SHA-256 `665daba026014fe5c9b829e4ebde101dff7a8d3b37eff54e9b3c7baa8c7180b2`
and passes artifact checks with zero sharing hooks, the theme capability,
ten color changes, and the canonical settings bridge. The signed-in settings
screen shows only theme instructions, without a sharing switch. Back returns
to Spotify. Library and its sorting dialog remain readable and usable.

After these tests, ADB installed the previously verified, Manager-signed
default APK as a data-preserving update. This restoration used ADB rather
than repeating the already-tested Manager installation flow. Clean sharing
was restored to enabled through Spotify's visible settings screen, and
Manager's Expert mode was turned off. Playback remained paused.

Another notification/queue pass on `dev.3` and the broader checklist below
remain open.

## Optional feature development checkpoint

On September 18, the additional settings, Home pinning, and server-file source
compiled and applied to the stock Spotify fixture in an isolated evaluation.
Five characterization tests against that unchanged source reproduced
cross-origin credential transmission, incorrect bytes after an ignored range
request, colliding track identifiers, stale indexes after changing servers,
and missing shortcut discovery on first use. Only fake credentials and
loopback fixtures were used. Its APK was not installed on the Pixel.

The port uses the existing private Spicetify settings screen. Home pinning
copies the verified native list and identifies shortcuts by URI. Server files
use an immutable configuration, constrained HTTPS URLs, validated byte ranges,
bounded scans, and a private provider without disk audio caching. Both new
patches are disabled by default. Android 7 keeps the other patches available;
server streaming requires Android 8 or later.

The final local suite passes 53 extension tests, 34 patch tests, 26 sharing
verifier cases, 22 settings-verifier negative cases, Android lint, and bundle
packaging. Independent review
verified the eight Home snapshots and four local-file snapshots against stock
DEX, then checked the four injected hooks and their public static targets.
Review findings about excess pin selections, disabling with invalid draft
fields, disappearing validation errors, and repeated HTTP headers were fixed
and covered by regression tests.

All four patches apply together. The initial tested local bundle has SHA-256
`4df88f5e62a958ae972fd5d00827f27cba3d93667846c6ddf453f65c062a521a`.
Gradle repackaged the bundle while running the settings-verifier cases.
The frozen checkpoint bundle has SHA-256
`49bd91d851af7dae330081b157214f35e31589422a3035c59b386fe59b10d388`;
the independent artifact check also passes against that exact bundle.
The signed base-APK artifact has SHA-256
`bfac0f27e3796ffd3abe9322091787eea07914b077c89138f3cd30df50ec608c`.
Independent checks pass for the sharing hooks, exact settings bridge, four
capabilities, private Activity/provider, unchanged permissions, signature,
and 1,145 default color resources with ten expected changes. This base-only
artifact is for inspection, not installation without its split resources.
Both new altered-model cases exit with status 1, report the expected ABI
failure, and leave no output APK.

These optional features are not published. Their remaining release checks
are the normal Manager installation flow, signed-in Home ordering and
unpinning, Android HTTPS validation, metadata extraction, Local Files
playback/seeking, disabling during reads, and configuration replacement.
The default Pixel installation remains the published `dev.3` sharing profile.

### Recreated emulator installation check

After the temporary test files were removed, the stock archive and the
Manager-signed default recovery APK were recovered from their known Downloads
paths on the Pixel. Their checksums match the recorded fixtures. The original
Java 21 build environment was restored from a checksum-verified distribution.

Commit `47a04d5` avoids unchanged status updates, repeated audio-pattern
compilation, and repeated linear searches for duplicate tracks. The 53
extension tests, 34 patch tests, 26 sharing-verifier cases, Android lint, and
Android bundle build pass. The frozen bundle has SHA-256
`25e717af66944f3396eb8b0f5e54a4939913bb9f8cc7811d33be1a3b7690d70e`.

A recreated disposable Android 16 ARM64 emulator used Manager 1.31.1, the
640 MB process runtime, and the stock split archive. Through Manager's visible
flow, the local bundle imported, all four patches were selected, patching
completed, and Android installed the result. Launching Spotify's normal main
Activity reached **Log in** and **Sign up free**. No account was entered. The
emulator was then closed.

The installed APK has SHA-256
`523eb6690b6b56962423511960ec4f075f3b97303e3dbea5dd80ef85637b638e`.
Independent artifact checks pass against the frozen bundle for sharing hooks,
all four capabilities, the canonical settings bridge, private components,
unchanged permissions, signature, and ten expected theme-color changes.
This proves installation and unauthenticated launch, not signed-in Home
ordering or server-file playback.

The Pixel's local bundle import works through Android's system picker after
turning off Manager's **System > Custom file picker** preference. It does not
require granting **All files access** on the phone. Manager built the same
four-patch profile from its saved original APK with the 1,024 MB process
runtime. Its exported APK has SHA-256
`7b64bd88ff6ba397a1f182376c2da6ba477de5e59acf68130332657684ae6b85`.
It passes the same artifact checks and uses the existing Pixel Manager
signing certificate. After direct fingerprint confirmation, Android completed
the update. The installed APK matches the exported artifact, Spotify remains
signed in, and all four settings sections appear through the native row.
The 22 settings-verifier negative cases also pass against this APK.

Home displayed eight shortcuts, and the picker discovered all eight on first
use. Selecting the last shortcut, saving, and restarting moved it to first
while preserving the relative order of the other seven. Its native click
action still opened the correct playlist. The picker retained the checked
selection after restart. Clearing it, saving, and restarting restored the
exact original eight-shortcut order. The temporary pin is cleared.

A synthetic HTTPS WebDAV fixture served an eight-second PCM WAV with fake
credentials. The Pixel completed folder discovery and Android metadata
extraction through the private provider. Enabling Spotify's local audio
option exposed the fixture in **Local Files**, with an eight-second duration
in the native player. Seeking to four seconds produced HTTP 206 reads from
nonzero offsets. Resuming advanced the displayed position from four to six
seconds with the Pause control visible, then playback was paused. Changing
to a fixture that deliberately ignores byte ranges produced the documented
scan-failure message instead of a ready track. Saving a working replacement
folder recovered to one ready track, with all new requests confined to that
folder. Turning server files off immediately changed the status to Disabled.
**Forget server** cleared the saved fields, and Local Files returned to its
empty state. Android rejected a shell UID's provider query because the
provider is not exported. The user confirmed hearing the synthetic tone.
Disabling during an active read still needs a runtime check. No personal
server credentials or music were used.

Two additional loopback regression tests pause the response body until the
client is blocked in a network read, then disable the connection. Audio reads
already reject the completed response and subsequent requests. Folder reads
initially returned the final listing despite cancellation; a cancellation
check before returning the response fixes that gap. Both cases now pass,
alongside all 55 extension tests, 34 patch tests, 26 sharing-verifier cases,
Android lint, and bundle packaging. The existing configuration guard also
prevents an obsolete scan from publishing its index. These JVM tests do not
replace the pending Android test. Cancellation does not immediately close a
stalled socket; the ten-second network timeout still applies.

The live review also found awkward singular track-count text and an oversized
password hint. The source now uses **Tracks ready: 1**, smaller proportional
input text, and a short **Saved password** hint with the retention rule in
the field description. Unit tests, lint, and bundle packaging pass; these
presentation changes still need a final device rendering check.

After the tests, the known-good default sharing APK was restored with a
data-preserving same-key ADB update, and its installed checksum was verified.
Spotify's local audio option is off again. Manager's Expert mode is off,
and its custom file-picker preference was restored. The temporary emulator,
HTTPS tunnel, and synthetic audio server are stopped.

### Cancellation and final field layout

Manager updated the existing local source to commit `9e844a5`, then built
all four patches from its cached original APK. The frozen bundle has SHA-256
`24c7edf7043b3cc7af0717970226a5caa015481f86af9f8e11c64a0c05eba7fb`.
The exported APK has SHA-256
`2b1ee2fcb2d46f9eefc015a10e157612449aaa8dca7bcce30ba4131f32969d87`.
Independent checks pass for all four patches, exact settings bridge, private
components, unchanged permissions, signature, and ten theme-color changes.
Manager installed the same-key update, and the installed checksum matches.
Login remained intact.

Through the native settings row, the Pixel renders the smaller proportional
fields and complete **Saved password** hint without overlapping controls.
The long synthetic URL scrolls horizontally within its input. A working scan
shows **Tracks ready: 1**. These observations close the field-layout check
for that artifact.

A slow HTTPS folder listing remained in **Scanning** until the switch was
turned off. The screen changed to **Disabled** and remained there after the
fixture finished sending the listing, with no audio requests for that folder.
A second fixture returned the listing immediately but streamed its audio
response slowly. The visible UI reported **Reading tags: 1 / 1** immediately
before disabling, then **Disabled** afterward; that sequence took 5.9 seconds.
Saving the normal fixture afterward recovered to **Tracks ready: 1**. This
tests cancellation during metadata's real provider audio reads, not a switch
change during native-player playback. The HTTPS relay continued draining
some origin responses, so its logs do not prove immediate socket closure.

**Forget server** cleared the fake connection, and the known-good default
sharing APK was restored through a data-preserving ADB update. Local audio
was not enabled during this pass. The fixture and tunnel are stopped; the
emulator remained closed throughout. A subsequent help-text-only correction
says disabling stops new requests and clears the track list, avoiding the
previous promise of immediate server-access shutdown. That sentence was
checked in source and the build, not reinstalled for another visual pass.

## Published optional-feature prerelease

[Release run 35360203169](https://github.com/spicetify/morphe-patches/actions/runs/35360203169)
passed tests, lint, bundle packaging, release preparation, and attestation
on source `5cff6d2`. It published
[v1.0.0-dev.4](https://github.com/spicetify/morphe-patches/releases/tag/v1.0.0-dev.4)
as an experimental prerelease. The tag points to generated release commit
`39bb8b1`, which contains that source. No push-triggered run appeared; this
was an explicit dispatch. Stable publishing remains disabled.

The downloaded bundle has SHA-256
`21057c225d942aa311c64573c62c85b36587bd9abb9715c82f3589f7f43fcfaf`,
matching GitHub's asset digest. `gh attestation verify` passes and identifies
the repository's `release.yml`, `dev`, source `5cff6d2`, and the run above.
The bundle's extension is byte-for-byte identical to the final local
`c98c63f` build. The generated catalog contains exactly four public patches;
only clean sharing is enabled by default. Every target remains experimental
Spotify `9.1.80.2221`, ARM64 version code `145767611`.

Morphe Desktop applies all four patches from the downloaded release to the
private stock base APK. The inspection APK has SHA-256
`2eb6c2ab70f7b661bf21840100db08bf45637c3511749a0257881eaa01c3ec5e`.
Independent checks pass for the exact bridge, all four capabilities,
private components, unchanged permissions, APK signature, and ten expected
theme-color changes. This base-only APK was not installed. The two altered
Home and local-files models each report the expected incompatibility, exit
with status 1, and produce no output APK.

On the Pixel, Manager's existing remote source updates to `dev.4` and lists
four patches. The obsolete local test source is disabled. With Expert mode
off, the normal patch flow selects one patch from the published source.
Its exported default APK has SHA-256
`e509751fdb2239706449f2bd9d3682e67193fb453cfa6a0144ee4fdb536ea27a`.
Independent verification confirms clean sharing, the exact release bridge,
no optional capabilities or server provider, unchanged colors and
permissions, and the existing Manager signing certificate.

Manager and Android complete the same-key update. The installed checksum
matches the export, login is preserved, and the native settings row opens
the sharing-only screen with its saved switch enabled. This default `dev.4`
build is now installed on the phone. The earlier `dev.3` recovery APK remains
available. No emulator or fixture server was started for this release pass.

### Desktop comparison after phone catalog failures

On September 18, 2026, the Pixel returned no songs for **Teardrop** while
still returning playlist results. Opening **Untrue** from Home reported
**The tracks on this release are not available.** A same-key ADB update to
the previously verified default `dev.3` APK reproduced that album failure.
Its installed checksum matched the retained recovery artifact. Restoring
default `dev.4` preserved login, and its installed checksum again matched
`e509751fdb2239706449f2bd9d3682e67193fb453cfa6a0144ee4fdb536ea27a`.
A public podcast episode also showed **Something went wrong** on `dev.4`;
the episode was not checked on `dev.3`. Android reported validated Internet
connectivity and no captive portal, which does not prove Spotify endpoint
access.

The running macOS Spotify client loaded all 13 tracks from **Untrue** through
its visible Home shortcut. Playing **Archangel** advanced from 0:00 to 0:25
and 0:38 with the Pause control visible. This verifies desktop player
progress, not audible output. Opening that exact album's public URL on the
Pixel still produced the unavailable-tracks message.

Desktop's Connect menu listed the Pixel. Selecting it changed the device
label to **Playing on Pixel 8**, but cleared the desktop track and timeline;
the phone showed no player. Device selection alone does not pass the Connect
playback check. The desktop menu was then used to select **This computer**
again. The disposable emulator is closed.

The album failure spans both tested phone builds while desktop playback
works. Its cause remains unresolved; these observations neither establish a
general service outage nor isolate the patches as the cause. Phone playback,
Connect, and content-dependent sharing checks remain open. No phone logout,
uninstall, data reset, or network-setting change was used in this comparison.

### Manager failure screens and stock recovery

On September 18, 2026, the disposable Android 16 ARM64 emulator imported
the published `dev.4` bundle through Manager's local-source update picker.
Manager `1.31.1` listed all four patches. These tests used Patcher `1.14.0`
with a 640 MB process limit; they did not change the Pixel installation.

Manager's theme editor accepted `notacolor` as the primary background value.
Applying the patches then opened **Patching failed**, with the message
**Primary background color must be #RRGGBB or #AARRGGBB.** Closing diagnostics
offered **Home** and **Error**, with no install action. Choosing
**Enable recommended patches** afterward selected only clean sharing and
removed the customized theme-option indicator.

A disposable copy of the original base APK changed the settings marker
`aboutPage` to `aboutPagg`, using the existing refusal-test fixture builder.
Selecting that file through Manager and applying clean sharing failed with
**Spotify settings ABI changed: Lp/xlt;. Use the verified Spotify 9.1.80.2221
APK.** No install action was offered. This exercises controlled input drift,
not another supported Spotify version. Neither failure fixture was installed.

Android's visible app-info flow uninstalled the emulator's unauthenticated
patched Spotify. ADB confirmed its removal, then installed the five original
stock splits. Every installed APK checksum matched its original input.
Launching Spotify displayed **Millions of songs. Free on Spotify.**,
**Sign up free**, and **Log in**. The emulator is closed afterward. This
proves recovery to the original app's welcome screen; ADB substituted for
the stock installer, and no Play Store reinstall or stock login was tested.

A subsequent single-commit push of `4062d13` to `dev` still produced no
Actions run for that exact SHA. Actions remained enabled, workflows were
active, and the commit contained no skip annotation. Other integrations
created check suites for the SHA. The missing automatic trigger remains
unresolved; successful manual runs do not close it.

### Phone catalog isolation and album sharing

The same album failure persists after disconnecting the phone's VPN and
restarting Spotify. Android's VPN screen shows no connected VPN, and a
current-network check confirms no VPN transport. The phone still has a
validated Internet connection. Available Spotify process logs contain no
HTTP status or exception that establishes the cause.

On the default `dev.4` APK, album sharing reaches Android's share preview
even while artwork and track loading fail. With **Clean sharing links** on,
the outgoing URL is the correct `open.spotify.com/album/` URL with no query
parameters. Turning it off preserves `si` and `utm_source`. Both cases use
the same album ID. No recipient was selected. The album-loading failure
persists with cleanup off; cleanup was restored to on afterward. This
verifies the outgoing album intent, not clipboard access, successful target
loading on the phone, or sharing other content types.

A private no-op bundle, built separately from source `8616490`, declares
the same experimental target and executes no patch changes. Its SHA-256 is
`b786244703d69747ae4dca002c3bc811fab1f4dbd2177d38d6f19212f169bc1d`.
Manager built two controls from its original input with only that source
selected, using a 1024 MB process limit:

| Manager bytecode mode | Exported and installed APK SHA-256 | Album result |
| --- | --- | --- |
| Fast | `21cf23773e2915f9256823b964fe0049092600ba8c7baa5f62fd1ff60896faa1` | Tracks unavailable |
| Full | `3f8b9269ffaa0aecae80caccde7b9a4298647dcec0b6c318a0cf31539dbb122a` | Tracks unavailable |

Independent checks find no Spicetify DEX descriptors, manifest components,
sharing hooks, sanitizers, or preference wrappers in either control. Both
retain the original permissions and all 1,145 checked default colors, pass
signature verification, and use the existing Manager certificate. Each
installed checksum matches its export. Same-key ADB updates preserve the
signed-in session. These are repackaged controls, not stock-signed APKs;
Morphe still processes their bytecode. They were not published.

The failure therefore occurs with the patch code absent and in both tested
bytecode modes. This does not rule out retained app state, the common APK
merging/signing path, or Spotify's handling of the mobile session. Its root
cause remains open, and no production-code workaround was made from these
observations.

### Repeatable device check and cleanup

The Pixel is restored to the default public `dev.4` APK. The new
`scripts/device-check.py` independently confirms its installed SHA-256
`e509751fdb2239706449f2bd9d3682e67193fb453cfa6a0144ee4fdb536ea27a`
and the existing Manager certificate on Android 17. Candidate inspection,
installed-byte comparison, and the album observation completed in 10.32
seconds. The album still reports tracks unavailable. This run did not need
an installation and does not measure build or patch time.
The final helper also passed its `--install` path with the same APK in 12.62
seconds, correctly skipping the redundant install. Ten automated tests cover
certificate and digest refusal, split-install refusal, update verification,
and the distinction between visible UI and playback. Updating to different
bytes with this helper remains covered by a simulated ADB test, not a live
installation in this pass.

Manager lists the public `dev.4` source enabled and the old local `dev.3`
source disabled. The temporary diagnostic source is absent. Its previously
saved build entry can still describe a diagnostic build; the installed-byte
check establishes which APK is actually installed. The emulator remains
closed. Tailscale remains disconnected as requested.

## Local Premium navigation patch

The unpublished source adds an optional **Hide Premium tab** patch for
Spotify `9.1.80.2221`. Before patching, the Pixel shows five navigation tabs:
Home, Search, Your Library, Premium, and Create. The patch changes only the
consumer of the `premium_tab_enabled` flag; it does not change account tier.
Five inspected navigation classes must match their stored ABI snapshots.

The reviewed Android bundle has SHA-256
`1a96497621575e15f49ad7890e80ec32f07ee77544b2cee862525c7c957a950f`.
The extension suite passes 59 tests and the patch suite passes 45 tests,
including 11 navigation-verifier fixtures. The existing 26 sharing-verifier
regression cases and Android lint pass. The navigation checker verifies the
original flag, hook location, argument and result registers, following branch,
capability, and compiled helper's `original && !hidePreference` behavior.
It rejects altered helper arguments, branch conditions, targets, and values.

Desktop patching of the two-patch sharing/navigation profile and the full
five-patch profile passed independent artifact checks before the final
settings-row width adjustment. Both are base-only inspection APKs and were
not installed. A fixture with the navigation marker changed fails with
**Spotify navigation ABI changed**, exits 1, and produces no patched APK.

A separate Gradle test invocation replaced the shared `.mpp` output with a
JVM-only archive. Desktop could load it, but Manager showed zero patches.
Rebuilding with `buildAndroid` restored the root `classes.dex` and Manager
listed all five patches. The artifact checker now rejects missing or invalid
Android patch DEX entries before inspecting an APK. Contributor instructions
require regenerating the Android bundle after separate test/build tasks.

Morphe Manager 1.31.1 on the Pixel built the reviewed bundle using its saved
original APK, Fast bytecode mode, and a 1024 MB process limit. Only **Clean
sharing links** and **Hide Premium tab** were selected. The exported APK has
SHA-256 `62401539844939c1b3a36fe91ff4663ce687ca855dd7d46ee11a568a14d803c1`.
Independent checks pass for the sharing hooks, native settings bridge,
navigation hook and helper, manifest permissions, all 1,145 unchanged default
colors, and APK signature.

The same-key update retained the signed-in session. The device checker
confirmed the installed bytes exactly match the export and use the existing
Manager certificate. Installation took 12.13 seconds; the complete candidate,
certificate, update, and installed-byte check took 15.91 seconds. This excludes
bundle compilation, Manager patching, export, and UI checks. ADB `install -r`
substituted for Manager's installer in this pass.

With the new patch enabled, the Pixel shows four evenly spaced tabs: Home,
Search, Your Library, and Create. A screenshot confirms no empty Premium slot.
Search, Your Library, and Home can be opened. The normal **Settings and
privacy > Spicetify** entry shows both installed switches at the same width.
Turning **Hide Premium tab** off and restarting restores all five original
tabs, including Premium. Reopening settings confirms the off value persisted.
Turning it back on and restarting restores the four-tab layout. Clean sharing
stays on throughout. Restarts used ADB force-stop followed by the normal
launcher activity; no app data was cleared. This is navigation/settings
evidence, not a new music-playback or ad-suppression pass.

## Standalone development app

On September 21, 2026, the separate `app.spicetify.development` debug app
was built and installed on the Pixel 8 running Android 17. It compiles the
production extension sources with development capability flags. The normal
launcher opens the real settings Activity. A clean direct Java compile,
unit tests, lint, and APK assembly passed; lint reported zero errors and 13
warnings. The update installation took 1.12 seconds, excluding compilation
and UI checks. This is a development app measurement, not a Spotify patching
measurement.

Five development-app tests pass, including player release and callback
cancellation on stop, completion, and error, plus rejection of stale callbacks
after starting another track. These use Robolectric's MediaPlayer shadow;
production code has no test-only playback factory. The final incremental
test/lint/assembly invocation took seven seconds with the APK tasks already
up to date. It does not measure a full code-change rebuild.

The first live pass exposed content overlapping the action bar. Applying
window insets fixed the layout, confirmed by a fresh screenshot. The Premium
preference persisted after force-stop and launcher restart, then was restored
to its default. No Spotify installation or data was changed during this pass.

An HTTPS tunnel served only a generated eight-second WAV tone using synthetic
credentials. The visible settings flow scanned one track. Preview playback
used the production private provider: screenshots show 0/8 seconds before a
five-second seek and 6/8 afterward, followed by **Finished**. Explicit stop
showed **Stopped** and disabled seeking. Opening settings during playback and
returning also showed **Stopped**. These are player/UI and server-read checks;
audible output was not independently confirmed during this pass.

A fixture that ignored Range requests produced the expected **Scan failed**
message and an empty track list. Restoring the valid folder and scanning again
recovered the one-track index. This development pass does not establish
Spotify's playback hook, queue, background audio, notifications, catalog
behavior, or ad suppression. The emulator remained closed.

The visible **Forget server** flow cleared the synthetic credentials and
tracks. The test server and temporary public tunnel were stopped, and both
processes were confirmed absent. The development app remains installed with
server access disabled.

## Runtime and release checklist

Use the normal Manager entry point before claiming release readiness.

- [x] Build the Android bundle and run all unit tests.
- [x] Inspect generated patch metadata and extension contents.
- [x] Patch sharing, colors, and their combination against the stock fixture.
- [x] Check informative failures for unsupported inputs and invalid options.
- [x] Install and launch the output in a disposable Android environment.
- [x] Add the source in Manager and patch through its visible flow.
- [ ] Test login, playback, queue, Connect, background playback, notifications.
- [ ] Test sharing for tracks, albums, playlists, episodes, and timestamps.
- [ ] Check Home, library, player, settings, and dialog colors.
- [x] Test source updates, same-key reinstall, and patch cancellation.
- [x] Restore stock Spotify in the disposable emulator, with the ADB substitution above.
- [ ] Verify a push starts CI and prerelease automation for the expected commit.

The local clean-sharing and Premium-navigation build is installed on the source phone. No stable
compatibility claim is made from installation or static verification alone.

## Home and Browse brand-ad experiment, September 21, 2026

The unpublished source adds **Hide Home and Browse ads** for Spotify
`9.1.80.2221`, ARM64 version code `145767611`. Patch selection defaults off;
when installed, the in-app switch defaults on. Restart guidance is visible.
This filters only the identified Home image/video and Browse brand-ad
sections. Audio ads, player display ads, pop-ups, upgrade prompts, and account
flags are outside this patch.

The host build uses Java 21, Android SDK 36, and Morphe Desktop 1.16.0 on
macOS ARM64. Eight class snapshots guard three list consumers, two structure
holders, two section models, and the native list interface. The filter copies
only when it encounters an ad, keeps the original section objects and order,
preserves unknown section kinds, and leaves protobuf storage unchanged.
Disabled or unexpected models return the original list.

The final source passes 65 extension tests, 49 patch/verifier tests, and the
26 existing sharing-verifier cases. New tests cover the Home and Browse tags,
empty/all-ad lists, unchanged input identity, mixed or null models, toggle
persistence, and absence of the switch when the patch is not installed.
Verifier mutations exercise the wrong argument, result, getter, filter,
iterator, caller, missing/duplicate hooks, and capability mismatch. A private
APK with an altered discriminator field is refused with no output APK.
Android lint reports zero errors and seven extension warnings. The separate
development app passes five tests and lint with zero errors and 13 warnings.

All six patches apply together to the stock base APK with SHA-256
`3dc0c561236d4dc01c17acc12dd219914fa2de61992a3d1428ff6ee677cd45ee`.
The frozen local bundle has SHA-256
`76cc74d662569ebd98e40abc5f12d7fbd4dd2cc11d2316ac0c38f0921e47830e`.
The combined APK, signed with a local debug key only for artifact checking,
has SHA-256
`87357d2b1937a9b1be86fdfe5e2e8962255f92ed756398c10dd0d2e0798a357b`.
It was not installed over the Manager-signed Spotify app.

`verify-artifact.py --hide-brand-ads` passes alongside all existing feature
flags and the default theme colors. It verifies the three exact caller and
list-getter hooks, input/output registers, downstream iterator calls,
capability, compiled helper against the bundle, and unchanged native model
classes. Existing sharing, settings, Premium navigation, manifest,
permissions, 1,145 color resources, and signing checks also pass. The ad
verifier accepts the previous Premium-tab APK with the ad patch unselected.

On the Pixel 8 running Android 17, the normal **Spicetify Development**
launcher opens the production settings screen. The new switch displays on by
default, switches off, stays off after a process restart, and retains that
value through a same-key development-app update. Turning it back on also
persists after a process restart. The final screenshot shows the label,
switch, and restart description without overlap. Development APK SHA-256:
`5c15393d0b53704c301c8c0253051e8ded5a0b26b3fe5b1b536c49a595339c66`.

The account owner confirmed Spotify Free. No actual brand-ad card appeared
for an on/off comparison. The development app proves settings behavior only;
it does not run Spotify's card renderers. Live removal, empty-space behavior,
accessibility after removal, normal navigation with ads present, and playback
remain unverified. The new patch stays optional and experimental, and no
stable release is cleared by these checks. Spotify data and the installed
Spotify APK were left untouched during this unit; the emulator stayed closed.

## Jellyfin protocol foundation, September 21, 2026

At this stage, the source had Jellyfin authentication, music-library discovery,
paginated indexing, and authenticated range reads. Settings and saved
configurations still used WebDAV. Jellyfin was not yet selectable in the
Android app, and this unit did not establish Spotify playback or release
readiness.

Quick Connect was approved through an existing Jellyfin browser session. A
temporary diagnostic session confirmed the current server's music views,
media-source sizes, metadata, and original stream endpoint. Password sign-in,
Quick Connect, malformed responses, expired authentication, pagination,
unsupported sources, redirect confinement, and file-version checks have
loopback fixture coverage. Access tokens stay in authorization headers and
out of track IDs. The selected server, account, and library scope each session.

The production Java protocol classes scanned a Jellyfin 12.1.0 library on
macOS ARM64 using Java 21, with a 256 MiB heap ceiling. This diagnostic used a
desktop JSON implementation; Android behavior remains a separate check. The
scan visited all 35,274 items in 217 seconds, indexed 35,242 supported tracks,
and reported 32 skipped sources or containers. Heap usage immediately after
the scan was 99 MiB; this is neither peak usage nor an Android measurement.
An earlier API probe measured a largest 500-item page of 1,426,740 bytes.

The implementation caps catalogs at 50,000 items, response bodies at 4 MiB,
and scanning at ten minutes. It reports progress and skipped tracks. Invalid
or nonprogressing pages fail instead of publishing an incomplete catalog.
Metadata comes from Jellyfin's API, avoiding a file probe for every song.

The Java reader verified 32-byte reads at the start, middle, and end of one
original stream. The server returned exact HTTP 206 ranges and Last-Modified
headers, and rejected a stale If-Unmodified-Since value with HTTP 412. The
reader learns the HTTP validator on the first read and pins it for subsequent
reads. It does not prove that a same-size file remained unchanged between the
catalog scan and that first read. Catalog versions remain part of track IDs.

WebDAV and Jellyfin share the strict range reader. Existing WebDAV identity
bytes are preserved by a golden regression test. After simplification, the
source passed 79 extension tests, 49 patch tests, 26 sharing-verifier cases,
and five development-app tests. Both the Android bundle and development APK
built. Android lint reported zero errors, with seven extension warnings and
13 development-app warnings.

The temporary diagnostic session was logged out; a subsequent authenticated
request returned HTTP 401, and its local credential file was deleted. No
Spotify installation, account data, or playback was changed by these checks,
and the emulator remained closed. Settings integration and a normal Android
sign-in, full scan, playback, and seeking pass are the next verification step.

## Jellyfin settings and Android sign-in entry, September 28, 2026

The current source adds Jellyfin as a selectable provider in the real
server-files settings view. The saved provider, account, and music library
scope catalog scans and range reads. Saving a new provider removes credentials
for the previous provider. Disabling server files clears the index but retains
the saved connection; **Forget server** removes its credentials and tracks.

The initial settings source passed 90 extension unit tests and five
development-app tests.
Both Android lint tasks and development APK assembly passed. Twelve settings
tests cover provider selection, credential preservation until save, HTTPS
validation, password-field clearing, forgetting, and the existing WebDAV
controls. These are source and fixture checks, not a live catalog result.

On the Pixel 8, the standalone development app opened through its launcher
and displayed the Jellyfin provider, HTTPS server field, and both sign-in
methods. The entered HTTPS server URL was visible in the field. **Use Quick
Connect** requested a code and showed the approval instructions. The first
code expired after three minutes without approval, and the app displayed a
retry message. That first attempt established the visible challenge and
expiry flow.
The development APK installed on the Pixel exactly matches the locally tested
APK, SHA-256 `d1649e4d0af7d557b883c4ba1ebebe4d2fd7c01798cc106243350c5371f5c453`.
Spotify itself was not replaced or modified for this test. The emulator stayed
closed.

A fresh Quick Connect code was approved in a signed-in Jellyfin browser. The
development app displayed the **Music** library. Selecting it and enabling
server files started an Android scan that finished with **Tracks ready: 35242
(32 skipped)**. Returning through the app's normal navigation showed 35,242
server tracks, with up to 50 listed on screen. A 16-second track reached
**Playing: 4 / 16 seconds** and then finished. A longer 206-second track
reached **Playing: 2 / 206 seconds**; selecting **Seek forward 5 seconds**
advanced the displayed position to **Playing: 10 / 206 seconds**. **Stop
playback** was selected afterward. This development-app path uses the production private
file provider, but it does not exercise Spotify's Local Files hook.

The source now accepts either server hostname without a URL scheme and assumes
HTTPS; an explicit HTTP scheme remains invalid. Extension tests cover bare
WebDAV hosts, host-and-port Jellyfin web URLs, and explicit HTTP rejection.
Review-driven tests also cover saving a Jellyfin library, switching providers,
an in-flight picker callback, and enabling a saved library during selection.
The final source passed 97 extension tests, including 18 settings tests, and
five development-app tests. Android lint and `buildAndroid` passed. The local
bundle has SHA-256
`49f9725cd19ee03f30f189ee097843d1a219a23a3be93ecde45a1be19ff65ce2`;
its embedded extension DEX contains the new URL guidance. This local file has
the same versioned filename as `dev.4`, but it has not replaced the published
bundle.

A same-key development-app update retained the saved Jellyfin server, account,
and Music library. Entering only the server hostname in the draft URL field
and choosing **Change music library** loaded the Music picker through that
saved session, without another sign-in. The final update's APK has SHA-256
`6333e8265c2520577c581e05cdad240348e14c9d0c8cf09ba0716a08235c88e0`,
and its installed bytes matched that tested build. The fresh scan completed
with **Tracks ready: 35242 (32 skipped)**. On request, the 206-second track
played again, reaching **Playing: 55 / 206 seconds** before it was stopped.
The device owner confirmed hearing normal audio on the Pixel.

Final review caught a failure path where **Use server files** could remain
checked during failed Jellyfin sign-in even though the saved provider stayed
disabled. The settings view now restores the switch to the saved state on
error; a regression test covers that sequence. The rebuilt development APK
has SHA-256
`b6e7b3acead1b537341d4505456ff0f02f631e49735d60b9e18e568819c8cb91`,
and the installed APK bytes match it. A patched Spotify pass remains open.
The published `dev.4` bundle still contains the WebDAV implementation, not
these local Jellyfin changes.

## Jellyfin in patched Spotify, September 28, 2026

The local Android bundle built from `ce687a3` was loaded through Manager's
existing local source. Manager 1.32.0 and Patcher 1.14.1 applied **Clean
sharing links**, **Hide Premium tab**, and **Local files from a server** to
the saved original Spotify `9.1.80.2221` APK on the Pixel 8 running Android
17. The stock base SHA-256 was
`3dc0c561236d4dc01c17acc12dd219914fa2de61992a3d1428ff6ee677cd45ee`;
the bundle SHA-256 was
`49f9725cd19ee03f30f189ee097843d1a219a23a3be93ecde45a1be19ff65ce2`.

The exported candidate SHA-256 was
`5c0e1c712ee2306547a8a8a01e1c40d1b1caa3c4d638c4f66ec6191f235de24f`.
The independent artifact checker verified the sharing, settings, and Premium
navigation hooks, the private Local Files provider and permissions, all
1,145 unchanged default colors, and the APK signature. A device preflight
confirmed the candidate had the same package, version code, and signing
certificate as the installed Spotify app. ADB performed a data-preserving
update; the installed APK checksum matches the exported candidate. Manager's
build and export were the normal entry point, while ADB substituted for its
installer.

Spotify opened signed in, retained the four-tab layout, and exposed the
**Server files** controls through **Settings and privacy > Spicetify**. The
Jellyfin provider and Quick Connect option appeared. Entering a hostname
without `https://` preserved the exact input. Authentication, scanning,
Local Files catalog display, and playback in Spotify remain to be checked.
Manager's patch-picker description still mentioned only WebDAV in this
candidate; source commit `7aa0707` corrects that description, and the
rebuilt bundle has SHA-256
`5dc97ec09ee3033af14d81b69eadeb36a62a36c7c1ed109d7f047a848793baed`.

Quick Connect was then approved in the signed-in Jellyfin browser. On the
installed APK with SHA-256
`5c0e1c712ee2306547a8a8a01e1c40d1b1caa3c4d638c4f66ec6191f235de24f`,
the Music library saved and scanned **35,242 tracks (32 skipped)**. After
enabling Spotify's **Local audio files** setting, its **Local Files** playlist
showed the scanned tracks. A track from The Garden played in Spotify; seeking
moved its displayed position from about **0:13 to 0:47**, playback continued,
and it was paused at **1:10**. The device owner confirmed audible playback.

Spotify presents the result as one flat **Local Files** playlist. Track rows
show artist and album text, but there are no album or artist browsing groups
for this server catalog. The richer library experience is a separate feature
gap. A display ad was visible on the Search landing page and over the player
while this three-patch APK was installed. It contains no brand-ad filter, so
these observations provide a live control for later visual-ad tests. They do
not establish that the unpublished ad patch removes either placement.

Source commit `d706634` raises the Quick Connect polling window from three to
nine minutes, short of Jellyfin's ten-minute server expiry. Android unit tests,
lint, patch tests, and `buildAndroid` pass. This improvement is in the current
source and bundle with SHA-256
`5dd27fbe27b7cfd5cec6feb545e09a77f01009730af7344cc9ae33ebb2fa64ca`;
the installed Spotify APK still contains the earlier three-minute behavior.

Source commits `52a76e0` and `f1f2ca5` add a grouped server catalog and a
browser launched from Spicetify settings. The catalog keeps Jellyfin album and
artist IDs when provided, orders album tracks by disc and track number, and
offers bounded Albums, Artists, Songs, and Search views. The browser is
read-only while the exact Spotify local-track playback command is being
validated; its track rows do not start or queue music. Unit tests, Android
lint, patch tests, and `buildAndroid` passed on that source. This was a
source-only result; the installed browser check follows below.

## Grouped Jellyfin browser and combined patches, September 28, 2026

Jellyfin returns album names on many audio items without `AlbumId`. A scan
before the grouping fix made 13,060 mostly single-track album rows from
35,242 playable tracks. Commit `4ae1677` groups missing-ID tracks by album
title and album artist, joining a known album ID only when the match is
unique. Explicit IDs still distinguish same-named releases. Unit tests cover
each case. A fresh development-app scan showed 2,929 groups and 1,008 artists.
One ten-track album appeared as one row, and its tracks were ordered 1.1
through 1.10.

A read-only query of the same Jellyfin Music library counted 35,274 audio
items. Of these, 11,068 had no `AlbumId`; 2,003 distinct album IDs appeared
on the remaining items. Missing-ID items formed 967 distinct album-title and
album-artist keys: 31 matched one known ID, one was ambiguous between known
IDs, and 935 matched none. Jellyfin's own **Albums** page showed 2,007 album
objects. The app keeps the named fallback groups so those tracks remain
browsable. The counts need not match Jellyfin's album-object view. The query
did not change server metadata or expose a credential in the report. Jellyfin
documents [album folder organization](https://jellyfin.org/docs/general/server/media/music/)
and has recorded [audio items with an album name but no album ID](https://github.com/jellyfin/jellyfin/issues/2879).

Manager 1.32.0 and Patcher 1.14.1 selected all six current-source patches.
The older duplicate local source remained enabled with none of its patches
selected. A base-only export passed the
independent artifact verifier but Android rejected installation with
`INSTALL_FAILED_MISSING_SPLIT`. Rebuilding from the original stock split-APK
archive produced an installable package. Its temporary diagnostic APK had
SHA-256 `1e3d7cae02f320c1f220136e4808618fb74f503c0f7705deda588247902a8c39`.
The verifier found all six patch effects and a valid signature. A same-key
Pixel update preserved Spotify's signed-in state and installed those exact
bytes.

With all six patches active, Spotify reached Home with four navigation tabs
and no Premium tab. The **Spicetify** settings screen showed controls for
sharing, colors, Premium, Home and Browse ads, Home shortcuts, and server
files. The first automatic Jellyfin scan failed; **Rescan library** completed
with 35,242 playable tracks, 2,929 album groups, 1,008 artists, and 32
skipped tracks. The **Browse server music** screen appeared below the Android
action bar after adding system-window insets. A ten-track album appeared as
one row, and opening it showed ordered tracks. Spotify's **Local Files** list
also showed the server tracks. Tapping one changed Spotify's player to that
track. The diagnostic hooks in two suspected play-command paths did not fire,
so album-row playback and queueing remain unimplemented. The diagnostic code
was removed from source after this test.

The first clean source build passed extension unit tests, Android lint, patch
tests, and `buildAndroid`. Its bundle SHA-256 was
`501ce65e767de328c2ada412940bbac0a24f75ce2c47683655302be940f2dded`.
Morphe Desktop 1.17.0 applied all six patches from that bundle in FULL mode.
The independent verifier checked the sharing, settings, ads, Premium,
server-files, and Home-pin hooks, 1,145 default color entries, ten selected
theme changes, and Android signing. The resulting Desktop APK SHA-256 was
`6d22a6f762982dda95d1554afbda884890cec90ceae75bba1e1185cf4cf84373`.

Manager then applied that bundle to the original split-APK archive
with all six patches selected. The independent verifier passed on its full
export, SHA-256 `733353414b980c77290341ff913cce29c8119926db6a9270339eec724ddabc0a`.
The Pixel's same-key update preserved Spotify's signed-in state and installed
those exact bytes. Launched from its normal app entry point, Spotify showed
four navigation tabs without Premium, and the Spicetify settings displayed
all six patch controls. The automatic Jellyfin scan again failed on first
launch. **Rescan library** recovered with 35,242 playable tracks, 2,929
album groups, 1,008 artists, and 32 skipped tracks. **Browse server music**
placed its tabs below the Android action bar. Opening the ten-track album
showed tracks ordered from 1.1 through 1.10. The startup scan failure and
live ad-card suppression remain open; this installed run does not establish
album-row playback or queueing.

Review found that fitting system windows on the padded browser root replaced
its 16dp side gutters. An outer inset container now preserves the inner
layout's 16dp horizontal and 8dp vertical padding. Extension unit tests,
Android lint, patch tests, and `buildAndroid` passed again. The revised
bundle SHA-256 is
`3be48e86a016b9dde840cc771f56d3622f195b76bb6e172a95549994ac6ce49e`.
Manager applied all six revised patches to the stock split-APK archive. The
independent artifact verifier passed; the full APK SHA-256 was
`0ecde03ea9939c7f813b77432838b1e27272c0489a2ab28ffe1f3d7395103e5a`.
The Pixel installed those exact bytes through a same-key update and retained
the Spotify account. From the normal launcher, Spotify again showed four
navigation tabs without Premium. On the final browser screen, the four tabs
started at x=42 and y=300 on the 1080-pixel-wide Pixel display, leaving the
action bar clear and preserving the intended side gutters. The automatic
Jellyfin scan failed again on first launch. The visible **Rescan library**
action recovered to 35,242 playable tracks, 2,929 album groups, and 1,008
artists with 32 skipped tracks. The browser then opened the same ten-track
album, showing its first track as 1.1. Its playback and queue controls remain
unimplemented. The first-launch failure and live ad-card suppression remain
open.

## Player ad-card patch and seven-patch Pixel update, September 29, 2026

The optional **Hide player ad cards** patch filters the Boolean result of the
stock Now Playing image-brand-ad mapper before its existing null branch. It
checks four exact class digests for Spotify `9.1.80.2221`. The independent
artifact verifier checks its call opcode, argument and result registers,
branch, capability, and unchanged native player models. Verifier regression
fixtures reject altered hook instructions, and a modified stock player-ad
model is refused before producing an APK.

Extension unit tests, Android lint, patch tests, and `buildAndroid` passed.
Morphe Desktop 1.17.0 applied all seven patches in FULL mode to the exact
stock base APK. The independent verifier checked the combined hooks, 1,145
default color entries, ten selected theme changes, and APK signing. Manager
1.32.0 then applied seven of seven patches to the original five-part stock
split archive. The exact bundle loaded into Manager had SHA-256
`e05194e7bc1555ac0cf4b48ca4a3d6e82a04fd6875182504580d3001ade74a9c`.
The exported full APK had SHA-256
`18bd737054be208b4a587e2851eb1c766f848680c94430577897ef45b9147ec5`
and passed the same independent checks. A same-key Pixel update installed
those exact bytes, preserved the signed-in account, and opened through the
normal launcher with four navigation tabs and no Premium tab.

The Spicetify screen showed the new **Hide player ad cards** control enabled.
Tapping it turned it off, and tapping it again restored the enabled state.
Spotify did not serve a visible ad card on Search during this run, so actual
card removal, its layout, and audio-ad behavior remain unverified. Local Files
showed no tracks after the update's first-launch scan; an attempted Play
action from the previous player bar did not establish ordinary playback in
this run. The previously observed first-launch Jellyfin scan failure remains
open, and playback needs a separate live check after recovery.

A follow-up native Spotify check opened a Free-account Daily Mix with song
rows, but selecting the playlist or a song did not start playback. Android's
Spotify media session remained in state `NONE` with no current item or player
error. A process-scoped log capture contained no playback failure message.
The same Daily Mix later opened as an empty playlist. The phone had validated
connectivity through a VPN during this check, but the September 18 VPN-off
control above reproduced the album-loading failure. These observations do
not isolate the network, account, APK signing, or a patch as the cause. Native
music playback and a served-ad on/off comparison remain open.

After a force-stop and normal launcher restart, Spotify still showed four
navigation tabs. Opening **Settings and privacy > Spicetify** through the
visible settings row showed both **Hide Home and Browse ads** and **Hide
player ad cards** enabled. This verifies that the settings survived a process
restart; no ad was served during this check.

### Album and playlist failure recheck

On September 29, 2026, the Pixel still showed **The tracks on this release
are not available** for the public **Untrue** album
(`1oLxSFO8bJwsU2OmZY4cdU`) with the seven-patch APK installed. The
installed SHA-256 matched the Manager export
`18bd737054be208b4a587e2851eb1c766f848680c94430577897ef45b9147ec5`.
The repeatable `scripts/device-check.py --album-id` run recorded
`albumObservation: tracks-unavailable` and `playbackVerified: false`.
Its `status: passed` means the observation procedure succeeded, not that
the album worked.

The saved `dev.1` Manager APK that played music on September 18 was
temporarily reinstalled with the same signing key and retained account
data. Its SHA-256 matched the original export
`3e89dcc081a4da519821156734b54dba95851299e9bd156e728bffe4466d9653`.
It now showed the same album error. The seven-patch APK was restored and its
installed SHA-256 checked again. Reinstalling those same seven-patch bytes
with Google Play recorded as Android's installer also left the album empty;
the original null installer record was restored. These comparisons further
weaken a later-patch regression or installer-source explanation.

Spotify's own **Clear cache** action reduced its displayed cache from
52.0 MB to 0.0 MB without removing downloads or signing out. The exact
album still failed immediately afterward. A Free-account desktop session
loaded all 13 tracks from **Untrue** and played **Archangel** through 0:18;
**Daily Mix 2** showed 50 songs. The Pixel later displayed Daily Mix track
rows, including the same first songs, but tapping its play control did not
move Android's Spotify media session out of `NONE`. The phone's VPN was then
turned off by the device owner, but its USB debugging connection dropped
before the album and playback check could be repeated. Mobile session,
network routing, and the shared repackaging path remain unresolved.

After USB reconnection, the Pixel's active network was Wi-Fi with no VPN
transport. The same seven-patch APK again showed **The tracks on this release
are not available** for **Untrue**. **Daily Mix 2** listed tracks, but tapping
**Fight Test** left Android's Spotify media session in `NONE` with the track
metadata set and no playback. The owner then signed directly into Spotify
again after an in-app sign-out. The album still failed, and **Home** showed
**Something went wrong** even after **Try again**. A Spotify-process error-log
query returned no entries for that attempt.

With the VPN still off, a brief cellular-only run used the same album and
seven-patch APK. The album remained unavailable. Wi-Fi was restored and
confirmed enabled. These checks rule out the active VPN, the tested Wi-Fi
route, and the retained Spotify login as sufficient explanations for this
failure. They do not establish whether account, device, or APK repackaging
is responsible.

The saved Fast no-op Morphe control APK matched its earlier SHA-256
`21cf23773e2915f9256823b964fe0049092600ba8c7baa5f62fd1ff60896faa1`.
A same-key update retained app data after the fresh sign-in. Its first album
check showed a loading error, and the repeat check showed **The tracks on
this release are not available**. The seven-patch APK was then restored by
same-key update; its installed SHA-256 again matched
`18bd737054be208b4a587e2851eb1c766f848680c94430577897ef45b9147ec5`.
The no-op result isolates the failure from our seven patch implementations,
but both APKs share Morphe's processing and signing path. A stock-signed
control on this Pixel is still needed to separate that path from device or
account behavior. Installing it requires removing the current APK and its
local Spotify data because the signatures differ.

The owner approved that destructive stock control. The saved base APK alone
failed installation with `INSTALL_FAILED_MISSING_SPLIT`. The captured stock
archive contained the base plus four configuration splits. All five APKs
verified under Spotify's original signing certificate, and the archive's
base matched the stock fixture SHA-256 above. Installing all five together
succeeded. Android's installed paths and SHA-256 values matched all five
original APKs exactly.

After a fresh sign-in on that stock-signed installation, **Untrue** showed
the same **The tracks on this release are not available** message. **Home**
also showed **Something went wrong**. **Your Library** loaded its list, but
the inspected saved playlist had no tracks, so it was not a playback control.
The stock result demonstrates that neither our patches nor Morphe's APK
processing and signing are required for the album failure on this Pixel.
It does not identify why this Spotify account and phone cannot load the
album while the desktop session can.

The stock APK was removed, and the exact seven-patch APK was reinstalled.
Android reported a single installed base whose SHA-256 matched
`18bd737054be208b4a587e2851eb1c766f848680c94430577897ef45b9147ec5`.
The reinstall erased local Spotify data and Jellyfin authorization as
expected. The owner signed in again. The exact installed-byte and album check
passed its verification procedure and still observed `tracks-unavailable`;
`playbackVerified` remained false. Jellyfin Quick Connect must be authorized
again to use that service. No music playback or ad removal was verified after
restoration.

A read-only check of the official Spotify account profile showed this Free
account set to **Portugal**, while the profile page offered **Spain** as the
current location. The owner confirmed using the account from Spain for more
than 14 days. Spotify's [country and region guidance](https://support.spotify.com/mt/article/country-region-settings/)
says a Free account can be used abroad for up to 14 days and must update its
account country to continue afterward. This is a plausible account-side
explanation for the stock and patched phone failures, but it has not been
validated by changing the country and repeating playback. The account
country was not changed during this check.

### Newer Play Store build control

On September 29, 2026, the owner approved testing a newer official build.
The seven-patch app was uninstalled, which erased its local data and Jellyfin
authorization. Google Play installed Spotify `9.1.86.2432` (version code
`146555520`) under `com.spotify.music`. Android recorded
`com.android.vending` as the installer. The installed base APK had SHA-256
`1039f1ab80a37c4657a61af33cc722749e9d0f780a2963238db182947884f65a`
and Spotify's original signing certificate. This stock installation remains
on the Pixel; the seven-patch APK has not been restored.

The owner later reported that Spotify was working after some time had passed,
then clarified that only **Home** had loaded. No song or album playback was
observed. A read-only Android media-session check at that point showed
Spotify in `NONE` with no track metadata.

Our repeat check on the official build found the active default network was
Wi-Fi with no VPN transport. Spotify Home loaded recommendations, including
**Untrue** and a Daily Mix. Opening **Untrue** from its Home tile showed
**That didn't work right. A quick refresh might fix it.** Tapping
**Refresh** did not load its track list. The same album failed after a
force-stop and cold-start. Opening **Daily Mix 5** from Home showed
**Something went wrong** and **Try again**. A second Android media-session
sample showed Spotify in `NONE`, with no track metadata. No audible playback
has been verified on the newer build.

These observations rule out the seven patches and Morphe repackaging as
necessary causes of the tested album and playlist errors. They do not prove
a device rate limit or a permanent account restriction. Home loading did not
establish that catalog browsing or playback recovered. An intermittent
account, service, or network condition remains possible.

The next visible-UI check opened **Late to Set** from a Home recommendation.
Its cover art and controls loaded, but the track area remained on a loading
spinner. Tapping **Play** left Spotify's Android media session in `NONE` with
no track metadata. Wi-Fi was briefly disabled, Android selected cellular as
the default network, and the same album again showed no tracks or playback.
Wi-Fi was restored and confirmed as the default network afterward.

On restored Wi-Fi, Search for **Archangel Burial** displayed **Something
went wrong** instead of song results. A process-scoped Android log sample
contained no HTTP `401`, `403`, or `429` status; its two timeout matches
came from Bluetooth logging. Absence of an HTTP status in those logs does
not establish that Spotify received a successful response. The broader
catalog failure also occurs on the official build across both tested
network routes, so patch changes are not a useful next diagnostic step.

### Free-account country change

On September 29, 2026, the owner approved changing the Free account country
from Portugal to Spain and signed in directly on Spotify's account page.
The owner made the change. The signed-in **Edit profile** page then displayed
**Profile saved**, with **Spain** selected as the account country. The Pixel
still had the official Play Store build installed. A subsequent playback
check remains pending while the phone completes Spotify's login flow.

The phone's email login screen displayed **Please provide a correct email
address** after an address was entered. A format-only inspection found one
`@`, a domain dot, ASCII characters, and no whitespace. A fingerprint-only
comparison confirmed the phone entry matched the signed-in account page's
registered email exactly; neither address was retained in the report.
Replacing the last character with the same character cleared the visible
error, but tapping **Continue** brought it back. The account page listed
Google as a connected login method. Tapping **Google** on the phone reached
Google's account chooser. The first listed account matched the registered
email in the same comparison. The owner selected that account on the phone;
Spotify returned **Something went wrong**, with **Try again** and **Dismiss**.
No successful mobile login was observed.

The phone's default Wi-Fi was validated, no always-on VPN was configured, and
its clock matched the current date and time. A controlled repeat disabled
Wi-Fi, confirmed cellular was the default network, and submitted the same
registered email after clearing the visible validation error. Spotify again
displayed **Please provide a correct email address**. A second fingerprint
check confirmed the entry remained identical to the registered address.
Wi-Fi was restored afterward. Neither the tested Wi-Fi nor cellular route
explains the email rejection by itself.

The owner's Google Play country-change request led to a read-only check of
**Play Store > Settings > General > Account and device preferences > Country
and profiles**. Portugal was selected, and Spain was not offered on that
screen. No Play Store country or payment profile was changed. Google's
[country-change guidance](https://support.google.com/googleplay/answer/7431675?co=GENIE.Platform%3DAndroid&hl=en)
requires a local payment method to add a new country and warns that content,
subscriptions, and Play balance can change. The observed Spotify login error
does not identify a Play Store country mismatch as its cause.

At the owner's request, the official `9.1.86.2432` installation was removed
and reinstalled from all five APK splits captured from that same Play Store
installation. Each APK passed signature verification and shared the same
signer certificate. After installation, every split's SHA-256 matched its
captured original, including the base APK recorded above. Android now reports
no installer package because ADB installed the APKs. The app opened to its
fresh **Log in** screen. This cleared local Spotify data; the phone was
already signed out. The owner's first login attempt after reinstall returned
**Something went wrong** without reaching Home; Android still showed
`LoginActivity`.

An explicit Android `pm clear com.spotify.music` then returned `Success`.
On the first launch after that clear, Spotify displayed **Something went
wrong** before another account was selected. **Try again** produced the same
screen on cellular, and Wi-Fi was restored. Selecting the linked Google
account in the app again returned to that error screen. On the same Pixel,
Spotify's website in Chrome accepted the same linked Google account and
opened **Account Overview**. Signing into the website did not change the
app's result: one final in-app Google attempt still returned **Something
went wrong**. The phone is not signed into the app, and playback after the
account-country change remains unverified. These checks isolate the current
failure to the installed app's path, but do not identify its server response
or root cause.
