package app.spicetify.extension.spotify.settings;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import app.spicetify.extension.spotify.home.HomePins;
import app.spicetify.extension.spotify.localserver.ServerConfig;
import app.spicetify.extension.spotify.theme.ThemeOverlay;

public final class PatchSettings {
    private static final String FILE = "spicetify_patch_settings";
    private static final String CLEAN_SHARING = "clean_sharing";
    private static final String HIDE_PREMIUM_TAB = "hide_premium_tab";
    private static final String HIDE_BRAND_ADS = "hide_brand_ads";
    private static final String HIDE_PLAYER_AD_CARDS = "hide_player_ad_cards";
    private static final String THEME_BACKGROUND = "theme_background";
    private static final String THEME_ACCENT = "theme_accent";
    private static volatile SharedPreferences preferences;

    private PatchSettings() {}

    public static void initialize(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
        if (InstalledPatches.homePins()) HomePins.initialize(context);
        if (InstalledPatches.serverFiles()) ServerConfig.initialize(context);
        if (InstalledPatches.themeColors() && context instanceof Application) ThemeOverlay.install((Application) context);
    }

    /** False until Spotify's Application has loaded the Spicetify settings. */
    public static boolean initialized() {
        return preferences != null;
    }

    public static boolean cleanSharingEnabled() {
        SharedPreferences current = preferences;
        return current == null || current.getBoolean(CLEAN_SHARING, true);
    }

    public static void setCleanSharingEnabled(boolean enabled) {
        SharedPreferences current = preferences;
        if (current == null) throw new IllegalStateException("Spicetify settings are not initialized.");
        current.edit().putBoolean(CLEAN_SHARING, enabled).apply();
    }

    public static boolean hidePremiumTabEnabled() {
        SharedPreferences current = preferences;
        return current != null && current.getBoolean(HIDE_PREMIUM_TAB, true);
    }

    public static boolean showPremiumTab(boolean spotifyEnabled) {
        return spotifyEnabled && !hidePremiumTabEnabled();
    }

    public static void setHidePremiumTabEnabled(boolean enabled) {
        SharedPreferences current = preferences;
        if (current == null) throw new IllegalStateException("Spicetify settings are not initialized.");
        current.edit().putBoolean(HIDE_PREMIUM_TAB, enabled).apply();
    }

    public static boolean hideBrandAdsEnabled() {
        SharedPreferences current = preferences;
        return current != null && current.getBoolean(HIDE_BRAND_ADS, true);
    }

    public static void setHideBrandAdsEnabled(boolean enabled) {
        SharedPreferences current = preferences;
        if (current == null) throw new IllegalStateException("Spicetify settings are not initialized.");
        current.edit().putBoolean(HIDE_BRAND_ADS, enabled).apply();
    }

    public static boolean hidePlayerAdCardsEnabled() {
        SharedPreferences current = preferences;
        return current != null && current.getBoolean(HIDE_PLAYER_AD_CARDS, true);
    }

    public static void setHidePlayerAdCardsEnabled(boolean enabled) {
        SharedPreferences current = preferences;
        if (current == null) throw new IllegalStateException("Spicetify settings are not initialized.");
        current.edit().putBoolean(HIDE_PLAYER_AD_CARDS, enabled).apply();
    }

    /** Returns the in-app background color, or null to keep Spotify's own. */
    public static Integer themeBackground() {
        SharedPreferences current = preferences;
        return current != null && current.contains(THEME_BACKGROUND) ? current.getInt(THEME_BACKGROUND, 0) : null;
    }

    /** Returns the in-app accent color, or null to keep Spotify's own. */
    public static Integer themeAccent() {
        SharedPreferences current = preferences;
        return current != null && current.contains(THEME_ACCENT) ? current.getInt(THEME_ACCENT, 0) : null;
    }

    /** Saves both theme colors; null keeps Spotify's own color for that part. */
    public static void setThemeColors(Integer background, Integer accent) {
        SharedPreferences current = preferences;
        if (current == null) throw new IllegalStateException("Spicetify settings are not initialized.");
        SharedPreferences.Editor editor = current.edit();
        if (background == null) editor.remove(THEME_BACKGROUND); else editor.putInt(THEME_BACKGROUND, background);
        if (accent == null) editor.remove(THEME_ACCENT); else editor.putInt(THEME_ACCENT, accent);
        editor.apply();
    }
}
