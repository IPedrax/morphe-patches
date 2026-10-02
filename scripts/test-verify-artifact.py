import importlib.util
from pathlib import Path
import unittest
from unittest.mock import patch

spec = importlib.util.spec_from_file_location("verify_artifact", Path(__file__).with_name("verify-artifact.py"))
verify = importlib.util.module_from_spec(spec)
spec.loader.exec_module(verify)

ANDROID = "http://schemas.android.com/apk/res/android"


def component(kind, name, *attributes):
    """One application component as aapt2 dumps it."""
    lines = [f"      E: {kind} (line=1)", f'        A: {ANDROID}:name(0x01010003)="{name}" (Raw: "{name}")']
    return lines + [f"        A: {ANDROID}:{attribute}" for attribute in attributes]


STOCK = component("activity", "com.example.Main") + component("provider", "com.example.Files")
BROWSER = component("activity", "app.spicetify.extension.spotify.settings.ServerMusicActivity",
                    "exported(0x01010010)=false")
PROVIDER = component("provider", "app.spicetify.extension.spotify.localserver.ServerFileProvider",
                     "exported(0x01010010)=false", "grantUriPermissions(0x0101001b)=false",
                     'authorities(0x01010018)="com.spotify.music.spicetify.localserver"')


class ManifestRuleTest(unittest.TestCase):
    def check(self, added, server_files=False):
        dumps = {apk: "\n".join([f"N: android={ANDROID} (line=1)", "  E: manifest (line=1)",
                                 "    E: application (line=1)"] + lines)
                 for apk, lines in (("stock.apk", STOCK), ("patched.apk", STOCK + added))}
        with patch.object(verify.subprocess, "check_output", side_effect=lambda command, text: dumps[command[3]]):
            verify.verify_manifest("aapt2", "stock.apk", "patched.apk", server_files)

    def test_unchanged_manifest_passes(self):
        self.check([])

    # A root mount install keeps the stock manifest, so it never registers what a patch adds.
    def test_added_activity_is_refused(self):
        with self.assertRaisesRegex(AssertionError, "must not add a manifest activity"):
            self.check(component("activity", "app.spicetify.Added", "exported(0x01010010)=false"))

    def test_added_provider_is_refused(self):
        with self.assertRaisesRegex(AssertionError, "must not add a manifest provider"):
            self.check(component("provider", "app.spicetify.Added", "exported(0x01010010)=false"))

    def test_server_files_add_only_their_browser_and_provider(self):
        self.check(BROWSER + PROVIDER, server_files=True)
        with self.assertRaisesRegex(AssertionError, "Server browser does not match"):
            self.check(BROWSER + PROVIDER)
        with self.assertRaisesRegex(AssertionError, "must not add a manifest activity"):
            self.check(BROWSER + PROVIDER + component("activity", "app.spicetify.Added"), server_files=True)


if __name__ == "__main__":
    unittest.main()
