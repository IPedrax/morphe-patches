# Advertising UI research

This note records source-level leads for separate **Hide upgrade prompts** and
**Hide visual ads** experiments, including the implemented Home and Browse
brand-ad filter. It does not establish runtime ad removal in Spotify
`9.1.80.2221`, and it excludes account-tier spoofing and audio-ad suppression.

## Historical evidence

The pinned historical reference is anddea/revanced-patches commit
[`3174510163d9787571bd93275b54932b575f82ed`][historical-tree], dated February
13, 2026. Its only Spotify implementation that removes visual advertising is
called [`Unlock Premium`][historical-patch] and declares Spotify
`9.0.90.1229`. It also mutates account attributes such as `ads`,
`player-license`, `type`, and `on-demand`; that mechanism is out of scope and
must not be ported for a UI-only patch.

The only direct historical navigation patch,
[`PremiumNavbarTabPatch.kt`][historical-navbar], is deprecated and depends on
`unlockPremiumPatch`. It is therefore not an independent non-spoofing
implementation. The visual branches below are inspection leads extracted from
the mixed historical patch, not reusable patch code.

The same historical patch has three separable UI-oriented leads:

- **Upgrade reminders in context menus.** Its extension filters menu models
  carrying `context_menu_remove_ads` or
  `playlist_entity_reinventfree_adsfree_context_menu_item`; it also filters
  `group_session_context_menu_start` only when the model contains
  `isPremiumUpsell=true`. The bytecode side finds a context-menu model via the
  stable string `ContextMenuViewModel(header=` and, for a newer implementation,
  the pair `?displayReason=` and `play-without-ads-exp`.
  [The helper][historical-extension] and
  [its fingerprints][historical-fingerprints] make this a concrete
  model-inspection lead, not proof that these resources or strings survive in
  the current APK.
- **Home and Browse brand-ad cards.** It removes protobuf sections selected by
  `featureTypeCase_` values `20` (`VIDEO_BRAND_AD`) and `21`
  (`IMAGE_BRAND_AD`) in Home, and `sectionTypeCase_` value `6`
  (`BRAND_ADS`) in Browse. The historical patch finds `sections_` in
  `HomeStructure` and `BrowseStructure`, then calls an injected list filter.
  [The patch][historical-patch], [fingerprints][historical-fingerprints], and
  [stub definitions][historical-stubs] provide the exact historical ABI names
  and field numbers to compare against the private APK.
- **Pendragon pop-up requests.** It detects constructors ending in
  `FetchMessageRequest;` and `FetchMessageListRequest;`, then replaces the
  reactive request path with its existing `onErrorReturn` value. This is a
  network behavior change, not a visual-container removal, so it needs a
  separate decision after proving the requests are only for the target popup
  surfaces. It must not be grouped with a UI-only visual-ad patch.
  [The historical patch][historical-patch] documents both anchors and the
  replacement behavior.

## What to inspect in Spotify 9.1.80.2221

The current repository declares only Spotify `9.1.80.2221`, ARM64 version code
`145767611`, as an experimental target in
[the current compatibility declaration][current-compat].
The existing local **Hide Premium tab** patch deliberately changes one
navigation flag and explicitly excludes other prompts and ads. It cannot be
reused as evidence that a Premium flag controls advertising. [The local
feature notes][premium-tab] state that boundary.

Local static inspection of the stock base APK with SHA-256
`3dc0c561236d4dc01c17acc12dd219914fa2de61992a3d1428ff6ee677cd45ee`
produced these current candidates. They are not runtime advertising evidence:

- `Lp/nw3;->a()Z` reads `premium_upsell_panel_enabled` for
  `android-context-menu`, but the complete DEX reference scan found no
  external direct call to that getter. It is unsuitable as an injection point
  until a consuming path is found.
- `Lp/a24;->a()Z` reads `enable_premium_banner` for
  `android-feature-premium-banner`. Its one external direct getter call is in
  `Lp/bvy;->invokeSuspend` at instruction 393, where the result in `v3` flows
  into `IF_EQZ`. A true getter result immediately emits `Boolean.FALSE`; a
  false getter result continues payment-state and type eligibility. Forcing
  this flag false could therefore enable a legacy eligibility path, so it is
  not a safe hide-banner toggle.
  The related constructor loads the settings-card strings **Explore Premium**,
  **Get full Premium**, and **You're on our free plan**. This ties the trace to
  a settings promotion, rather than establishing a global advertising switch.
- `com.spotify.adsdisplay.display.DisplayAdActivity`,
  `mobile-display-ad-card`, `mobile-ads-display-ad-element`, and
  `ScrollCardType.DISPLAY_AD` are display-ad trace candidates. A current Pixel
  Home screenshot showed neither an upgrade banner nor a display ad. A later
  Free-account Search screenshot showed a L'Oréal Paris display card labeled
  **Advertisement**, and the same campaign appeared over the player while a
  Jellyfin Local Files track played. These are two observed visual targets;
  neither has been tied to a patch hook yet.
- The current resource table lacks `context_menu_remove_ads` and
  `playlist_entity_reinventfree_adsfree_context_menu_item`.
  `group_session_context_menu_start` survives, but that alone does not prove
  an upgrade reminder is present.

## Implemented brand-ad experiment

The current APK preserves the Home discriminators `20` and `21` and Browse
discriminator `6`. The **Hide Home and Browse ads** patch filters lists in
three consumers: `Lp/jb20;->invoke`, `Lp/vot;->g`, and `Lp/x7v0;->a`.
Each consumer obtains its section list, then iterates it through `Iterable`.
This lets the helper return a filtered copy without changing protobuf
storage or its mutability guard. Patching the structure getter to return an
`ArrayList` would violate its native `Lp/ih40;` return type.

Eight class snapshots guard the consumers, structure holders, section models,
and list interface. The filter retains original section objects and order,
preserves unknown discriminator values, and returns the original list when
disabled or when a model is unexpected. It does not mutate account flags.
The artifact verifier checks hook placement, registers, downstream iterator
calls, unchanged native model classes, and the compiled helper against the
selected bundle. See [the verification record](verification.md).

The Pixel account owner confirmed a Free account. The Search display card is
now an observed candidate for an on/off comparison, but its connection to the
Browse `BRAND_ADS` model is unverified. Keep the patch optional and
experimental until removal, layout, and ordinary navigation pass that test.

## Remaining inspection

Inspect the private stock APK before designing a patch:

1. Find the current context-menu view-model construction path and verify every
   candidate resource identifier and serialized marker. Record which actual
   upgrade reminder each candidate renders, then filter only that model.
2. Find Home and Browse section holders with a `sections_` list and identify
   their discriminant fields. Prove that any matching case number renders a
   visible brand-ad card before removing it. Preserve list mutability and
   renderer behavior; the historical code globally relaxes a protobuf
   mutability guard, so that part is not a safe copy-forward.
3. Locate `FetchMessageRequest` and `FetchMessageListRequest` only if a
   repeatable popup is present. Trace each result to its displayed surface.
   Do not convert a request to an error merely because its class name matches.
4. Add separate switches and prove on/off behavior, empty-space handling,
   accessibility, navigation, restart behavior, and normal playback. A
   successful fingerprint or patched APK alone does not establish runtime
   support.

## Current Morphe third-party source

The current public `cvnfork/morphe-spotify-patches` head resolved to
[`289d59e2bdae0fa6f16ffa0e2769351be46a94e8`][cvnfork-tree] during this
research. Its README documents settings, Home-pin ordering, and WebDAV local
files, but no Spotify advertisement or upgrade-reminder implementation.
[The source README][cvnfork-readme] is therefore useful only as a Morphe
integration reference, not as an advertising patch source.

No source above supports a runtime claim for Spotify `9.1.80.2221`. Treat the
historical names, strings, and field numbers as comparison probes for the
private APK, and reject a patch when its target does not match exactly.

[historical-tree]: https://github.com/anddea/revanced-patches/tree/3174510163d9787571bd93275b54932b575f82ed
[historical-patch]: https://github.com/anddea/revanced-patches/blob/3174510163d9787571bd93275b54932b575f82ed/patches/src/main/kotlin/app/revanced/patches/spotify/misc/UnlockPremiumPatch.kt
[historical-fingerprints]: https://github.com/anddea/revanced-patches/blob/3174510163d9787571bd93275b54932b575f82ed/patches/src/main/kotlin/app/revanced/patches/spotify/misc/Fingerprints.kt
[historical-extension]: https://github.com/anddea/revanced-patches/blob/3174510163d9787571bd93275b54932b575f82ed/extensions/shared/src/main/java/app/revanced/extension/spotify/misc/UnlockPremiumPatch.java
[historical-stubs]: https://github.com/anddea/revanced-patches/tree/3174510163d9787571bd93275b54932b575f82ed/extensions/shared/stub/src/main/java/com/spotify
[historical-navbar]: https://github.com/anddea/revanced-patches/blob/3174510163d9787571bd93275b54932b575f82ed/patches/src/main/kotlin/app/revanced/patches/spotify/navbar/PremiumNavbarTabPatch.kt
[cvnfork-tree]: https://github.com/cvnfork/morphe-spotify-patches/tree/289d59e2bdae0fa6f16ffa0e2769351be46a94e8
[cvnfork-readme]: https://github.com/cvnfork/morphe-spotify-patches/blob/289d59e2bdae0fa6f16ffa0e2769351be46a94e8/README.md
[current-compat]: ../patches/src/main/kotlin/app/spicetify/patches/spotify/Compatibility.kt
[premium-tab]: optional-features.md#hide-the-premium-tab-in-local-builds
