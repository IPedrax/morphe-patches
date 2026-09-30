package app.spicetify.extension.spotify.theme;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, manifest = Config.NONE)
public class EncorePaletteTest {
    @Test public void keepsStockColorsWithoutATheme() {
        assertNull(EncorePalette.map(EncorePalette.BASE, null, null));
        assertNull(EncorePalette.map(EncorePalette.ACCENT, null, null));
    }

    @Test public void derivesSurfacesFromTheBackground() {
        int navy = 0xFF0B1026;
        assertEquals(Integer.valueOf(navy), EncorePalette.map(EncorePalette.BASE, navy, null));
        assertEquals(Integer.valueOf(0xFF181D33), EncorePalette.map(EncorePalette.ELEVATED, navy, null));
        assertEquals(Integer.valueOf(0xFF23283E), EncorePalette.map(EncorePalette.HIGHLIGHT, navy, null));
        assertNull(EncorePalette.map(EncorePalette.ACCENT, navy, null));
    }

    @Test public void derivesAccentShadesAndLeavesOtherColorsAlone() {
        int orange = 0xFFFF6437;
        assertEquals(Integer.valueOf(orange), EncorePalette.map(EncorePalette.ACCENT, null, orange));
        assertEquals(Integer.valueOf(EncorePalette.pressed(orange)), EncorePalette.map(EncorePalette.ACCENT_PRESS, null, orange));
        assertNull(EncorePalette.map(0xFF000000, 0xFF0B1026, orange));
        assertNull(EncorePalette.map(0xFFFFFFFF, 0xFF0B1026, orange));
    }

    @Test public void clampsLightBackgrounds() {
        assertEquals(0xFFFFFFFF, EncorePalette.lighten(0xFFF8F8F8, 24));
    }
}
