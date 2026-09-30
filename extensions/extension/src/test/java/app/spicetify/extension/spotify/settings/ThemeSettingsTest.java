package app.spicetify.extension.spotify.settings;

import android.app.Dialog;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import app.spicetify.extension.spotify.theme.ThemeOverlayTestAccess;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadows.ShadowDialog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE, shadows = ThemeSettingsTest.Capabilities.class)
public class ThemeSettingsTest {
    @Implements(value = InstalledPatches.class, isInAndroidSdk = false)
    public static class Capabilities {
        @Implementation public static boolean themeColors() { return true; }
    }

    @Before public void initialize() {
        var application = RuntimeEnvironment.getApplication();
        application.deleteSharedPreferences("spicetify_patch_settings");
        PatchSettings.initialize(application);
        if (Build.VERSION.SDK_INT >= 30) ThemeOverlayTestAccess.attach(application);
    }

    @After public void detach() {
        if (Build.VERSION.SDK_INT >= 30) ThemeOverlayTestAccess.detach();
    }

    @Test public void inactiveOverlayExplainsManagerColors() {
        ThemeOverlayTestAccess.detach();
        try (var controller = Robolectric.buildActivity(SpicetifySettingsActivity.class,
                SpicetifySettingsActivity.page(RuntimeEnvironment.getApplication(), SpicetifySettingsActivity.PAGE_APPEARANCE)).setup()) {
            View root = controller.get().getWindow().getDecorView();
            assertNull(row(root, "Background, #000000"));
            assertTrue(hasText(root, "Theme colors"));
        }
    }

    @Test public void parsesOpaqueAndTranslucentHex() {
        assertEquals(Integer.valueOf(0xFF112233), ThemeSettings.parse("#112233"));
        assertEquals(Integer.valueOf(0xFF112233), ThemeSettings.parse(" 112233 "));
        assertEquals(Integer.valueOf(0x80112233), ThemeSettings.parse("#80112233"));
        assertNull(ThemeSettings.parse("#1122"));
        assertNull(ThemeSettings.parse("green"));
    }

    @Test public void savingABackgroundKeepsTheAccentAndOffersReset() {
        try (var controller = Robolectric.buildActivity(SpicetifySettingsActivity.class,
                SpicetifySettingsActivity.page(RuntimeEnvironment.getApplication(), SpicetifySettingsActivity.PAGE_APPEARANCE)).setup()) {
            View root = controller.get().getWindow().getDecorView();
            assertNull(row(root, "Use Morphe Manager colors"));
            row(root, "Background, #000000").performClick();
            Dialog sheet = ShadowDialog.getLatestDialog();
            EditText hex = first(sheet.getWindow().getDecorView(), EditText.class);
            hex.setText("nope");
            button(sheet, "Save").performClick();
            assertTrue(sheet.isShowing());
            assertNull(PatchSettings.themeBackground());
            hex.setText("#0B1026");
            button(sheet, "Save").performClick();
            assertFalse(sheet.isShowing());
            assertEquals(Integer.valueOf(0xFF0B1026), PatchSettings.themeBackground());
            assertNull(PatchSettings.themeAccent());
        }
        // Robolectric cannot resolve styles once a loader has providers, so the next Activity uses a fresh loader.
        ThemeOverlayTestAccess.detach();
        ThemeOverlayTestAccess.attach(RuntimeEnvironment.getApplication());
        try (var controller = Robolectric.buildActivity(SpicetifySettingsActivity.class,
                SpicetifySettingsActivity.page(RuntimeEnvironment.getApplication(), SpicetifySettingsActivity.PAGE_APPEARANCE)).setup()) {
            View root = controller.get().getWindow().getDecorView();
            assertNotNull(row(root, "Background, #0B1026"));
            row(root, "Use Morphe Manager colors").performClick();
            assertNull(PatchSettings.themeBackground());
        }
    }

    @Test @Config(sdk = 29) public void androidTenExplainsManagerColors() {
        try (var controller = Robolectric.buildActivity(SpicetifySettingsActivity.class,
                SpicetifySettingsActivity.page(RuntimeEnvironment.getApplication(), SpicetifySettingsActivity.PAGE_APPEARANCE)).setup()) {
            View root = controller.get().getWindow().getDecorView();
            assertNull(row(root, "Background, #000000"));
            assertTrue(hasText(root, "Theme colors"));
        }
    }

    private View row(View view, String description) {
        if (view.isClickable() && description.contentEquals(view.getContentDescription())) return view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = row(group.getChildAt(i), description);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Button button(Dialog dialog, String label) {
        Button found = find(dialog.getWindow().getDecorView(), label);
        if (found == null) throw new AssertionError("Missing button: " + label);
        return found;
    }

    private Button find(View view, String label) {
        if (view instanceof Button && label.contentEquals(((Button) view).getText())) return (Button) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                Button found = find(group.getChildAt(i), label);
                if (found != null) return found;
            }
        }
        return null;
    }

    private <T extends View> T first(View view, Class<T> kind) {
        if (kind.isInstance(view)) return kind.cast(view);
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                T found = first(group.getChildAt(i), kind);
                if (found != null) return found;
            }
        }
        return null;
    }

    private boolean hasText(View view, String text) {
        if (view instanceof TextView && text.contentEquals(((TextView) view).getText())) return true;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) if (hasText(group.getChildAt(i), text)) return true;
        }
        return false;
    }
}
