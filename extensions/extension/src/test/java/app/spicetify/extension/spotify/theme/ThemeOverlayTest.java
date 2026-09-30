package app.spicetify.extension.spotify.theme;

import android.content.res.Resources;
import app.spicetify.extension.spotify.settings.PatchSettings;
import java.io.File;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static app.spicetify.extension.spotify.theme.ThemeOverlayTestAccess.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class ThemeOverlayTest {
    @Before public void attachOverlay() {
        var application = RuntimeEnvironment.getApplication();
        application.deleteSharedPreferences("spicetify_patch_settings");
        PatchSettings.initialize(application);
        attach(application);
    }

    @After public void detachOverlay() {
        detach();
    }

    @Test public void savedColorsResolveAndResetClearsThem() {
        Resources resources = RuntimeEnvironment.getApplication().getResources();
        PatchSettings.setThemeColors(0xFF0B1026, null);
        assertTrue(ThemeOverlay.refresh());
        assertEquals(0xFF0B1026, resources.getColor(BACKGROUND, null));
        assertEquals(EncorePalette.lighten(0xFF0B1026, 13), resources.getColor(ELEVATED, null));
        assertThrows(Resources.NotFoundException.class, () -> resources.getColor(ACCENT, null));

        PatchSettings.setThemeColors(0xFF0B1026, 0xFFFF6437);
        assertTrue(ThemeOverlay.refresh());
        assertEquals(0xFFFF6437, resources.getColor(ACCENT, null));
        assertEquals(EncorePalette.pressed(0xFFFF6437), resources.getColor(PRESSED_ACCENT, null));
        assertEquals(1, tables().length);

        PatchSettings.setThemeColors(null, null);
        assertTrue(ThemeOverlay.refresh());
        assertThrows(Resources.NotFoundException.class, () -> resources.getColor(BACKGROUND, null));
        assertEquals(0, tables().length);
    }

    @Test public void accentOnlyLeavesBackgroundResourcesAlone() {
        Resources resources = RuntimeEnvironment.getApplication().getResources();
        PatchSettings.setThemeColors(null, 0xFFFF6437);
        assertTrue(ThemeOverlay.refresh());
        assertEquals(0xFFFF6437, resources.getColor(ACCENT, null));
        assertThrows(Resources.NotFoundException.class, () -> resources.getColor(BACKGROUND, null));
    }

    private File[] tables() {
        File directory = new File(RuntimeEnvironment.getApplication().getNoBackupFilesDir(), "spicetify-theme");
        File[] found = directory.listFiles((dir, name) -> name.endsWith(".arsc"));
        return found == null ? new File[0] : found;
    }
}
