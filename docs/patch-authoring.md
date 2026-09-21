# Writing Spotify patches

Use Morphe's patcher documentation for API details and this repository's
existing patches for the supported Spotify build. Start with one observable
behavior and prove its hook before adding more controls.

## Official guides

These sources cover authoring, porting, and local iteration. Reviewed on
September 21, 2026; use this repository's pinned dependencies when examples
refer to older or newer API versions.

| Guide | Why it helps |
| --- | --- |
| [Anatomy of a Morphe patch](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_patch_anatomy.md) | Explains a sample ad-disabling patch, patch dependencies, options, and runtime extensions. |
| [Fingerprinting](https://github.com/MorpheApp/morphe-patcher/blob/main/docs/2_2_1_fingerprinting.md) | Explains how to locate methods and instruction sequences in obfuscated code. |
| [ReVanced-to-Morphe migration](https://github.com/MorpheApp/morphe-patcher/issues/22) | Describes changed class lookup, instruction matching, and extension APIs. Its dependency examples are historical. |
| [Development setup and terminal loop](https://github.com/MorpheApp/morphe-documentation/blob/main/docs/morphe-development/1_setup.md) | Shows building a bundle and applying it with Desktop from a script or IDE. |
| [Desktop CLI](https://github.com/MorpheApp/morphe-desktop/blob/main/docs/documentation.md) | Documents patch selection, options files, split inputs, and command-line patching. |
| [JADX scripting](https://github.com/skylot/jadx/wiki/Jadx-scripts-guide) | Automates repeated class and method searches during investigation. |

The [JADX smali debugger](https://github.com/skylot/jadx/wiki/Smali-debugger)
also supports breakpoints and stepping. It needs a debuggable app or an
appropriately configured rooted test environment. It is not immediately
available against an ordinary release app on an unrooted phone.

## Workflow in this repository

Keep APKs, decompiled Spotify code, credentials, and signing keys outside
the repository. Follow this sequence for each new patch:

1. Record the original APK version and hash. Find the relevant resource,
   string, or method with JADX, then inspect its actual DEX instructions and
   callers. A suggestive method name alone does not establish behavior.
2. Define a narrow fingerprint and the expected match count. Refuse unknown
   layouts with a useful error. Inspect register types and control flow at
   the insertion point before injecting instructions.
3. Put patch-time changes in `patches/` and runtime logic in
   `extensions/extension/`. Keep settings-dependent behavior in the extension
   so a user can toggle it without rebuilding. Connect installed capabilities
   through the existing settings patch.
4. Test the extension logic, build the bundle, and apply it to the exact
   original APK. Building alone cannot resolve or validate a fingerprint.
5. Inspect the resulting DEX and manifest with the artifact verifier. Extend
   that verifier for the new hook and test a deliberately mismatched input.
6. Use the [device check command](../CONTRIBUTING.md#repeat-a-device-check)
   for repeated comparisons of signed builds. Keep the same signing key.
   Desktop and Manager keys can differ; do not assume their APKs can update
   one another. The helper stops on a certificate mismatch.
7. Exercise the visible behavior on and off, including restart, failure,
   and recovery. Complete the normal Manager flow before a release claim.
   Record source, artifact, device, and observed results separately in
   [the verification record](verification.md).

For settings layouts, preferences, server indexing, and file reads, use the
[standalone development app](development-app.md) between full patch builds.
It compiles the production extension sources and keeps its data separate
from Spotify. Actual Spotify hooks still need the full sequence above.

For a concrete example, read
[SharingLinksPatch.kt](../patches/src/main/kotlin/app/spicetify/patches/spotify/privacy/SharingLinksPatch.kt)
alongside
[SharingLinks.java](../extensions/extension/src/main/java/app/spicetify/extension/spotify/privacy/SharingLinks.java).
The patch locates and modifies URL handling; the extension applies the user's
setting at runtime. A future ad patch needs its own proven target, rather
than reusing the guide's illustrative method or assuming a Premium flag
controls every advertising path.

## Skill search

No installed skill specifically covers Morphe or ReVanced patch authoring.
The public catalog search found
[android-apk-patch](https://github.com/codeatcode/oss-ai-skills/blob/main/frameworks/android-apk-patch/SKILL.md),
a broad manual decompile, rebuild, sign, and test reference. It is not a
Morphe-specific workflow and was not installed. The catalog's
[zenmux Morphe skill](https://github.com/zenmux/skills/blob/main/skills/morphe/SKILL.md)
deploys web applications to a different service with the same name.

The official guides above and this repository's verifiers are the preferred
starting point. A future repository skill can wrap this sequence and the
actual build/check commands without importing unrelated deployment or APK
distribution instructions.
