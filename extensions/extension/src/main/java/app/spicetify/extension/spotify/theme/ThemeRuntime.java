package app.spicetify.extension.spotify.theme;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/** Loads the selected theme into Spotify's resources at startup, and applies new selections. */
public final class ThemeRuntime {
    private static final String TAG = "Spicetify";
    /** The role colors of the theme in use, for the screens Spicetify draws itself. */
    private static volatile Map<String, Integer> roles = Collections.emptyMap();

    private ThemeRuntime() {}

    /** Themes need Android 11, which can lay more resources over Spotify's with a {@link ThemeTable}. */
    public static boolean supported() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R;
    }

    /** Injection point, from PatchSettings.initialize in SpotifyApplication.onCreate. */
    public static void install(Context context) {
        try {
            ThemeState.migrate(context);
            if (!supported()) return;
            Application application = (Application) context.getApplicationContext();
            ThemeTable.applyTo(application.getResources());
            application.registerActivityLifecycleCallbacks(new Callbacks());
            // The table follows the installed Spotify's resource IDs, so the saved theme is loaded on
            // every start. Material You gets the current wallpaper colors this way too.
            Map<String, Integer> colors = roleColors(application, ThemeState.load(application));
            Map<String, Integer> values = values(colors);
            // Compose reads the values from memory, so at startup it follows the saved theme even if
            // the resources can't take it.
            follow(colors, values);
            load(application, values);
        } catch (Exception e) {
            Log.w(TAG, "Theme could not be loaded", e);
        }
    }

    /** Applies and saves a selection. Returns false, and changes and saves nothing, when it can't be applied. */
    public static boolean select(Context context, ThemeState.Selection selection) {
        try {
            // The role map is empty only when the theme patch didn't inject its table.
            if (!supported() || ThemeRoleMap.load().isEmpty()) return false;
            Map<String, Integer> colors = roleColors(context, selection);
            Map<String, Integer> values = values(colors);
            // The resources first: when they can't take the theme, Compose keeps the current one.
            load(context, values);
            follow(colors, values);
            ThemeState.save(context, selection);
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Theme could not be applied", e);
            return false;
        }
    }

    /** A role's color in the theme in use, or {@code stock} where the theme keeps Spotify's. */
    public static int color(String role, int stock) {
        Integer color = roles.get(role);
        return color == null ? stock : color;
    }

    /** A selection's role colors; none for Spotify's own colors. */
    public static Map<String, Integer> roleColors(Context context, ThemeState.Selection selection) {
        return ThemeState.CUSTOM.equals(selection.kind)
                ? ThemeResolver.resolve(selection.colors, "button").colors
                : ThemePresets.colors(context, selection.kind);
    }

    /** One value for each color resource the role colors map. */
    private static Map<String, Integer> values(Map<String, Integer> colors) {
        return ThemeRoleMap.overlayValues(ThemeRoleMap.load(), colors);
    }

    /** Hands a theme to Compose, and to the screens Spicetify draws itself. */
    private static void follow(Map<String, Integer> colors, Map<String, Integer> values) {
        roles = colors;
        ComposeTheme.update(values);
    }

    /** Lays a theme's values over Spotify's resources; none restore Spotify's own. */
    private static void load(Context context, Map<String, Integer> values) throws IOException {
        ThemeTable.load(context, values);
    }

    /** Loads the theme into each activity's resources before any of its views exist. */
    @TargetApi(Build.VERSION_CODES.Q)
    private static final class Callbacks implements Application.ActivityLifecycleCallbacks {
        @Override
        public void onActivityPreCreated(Activity activity, Bundle state) {
            try {
                ThemeTable.applyTo(activity.getResources());
            } catch (RuntimeException e) {
                Log.w(TAG, "Theme could not be loaded into " + activity.getClass().getName(), e);
            }
        }

        @Override public void onActivityCreated(Activity activity, Bundle state) {}
        @Override public void onActivityStarted(Activity activity) {}
        @Override public void onActivityResumed(Activity activity) {}
        @Override public void onActivityPaused(Activity activity) {}
        @Override public void onActivityStopped(Activity activity) {}
        @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
        @Override public void onActivityDestroyed(Activity activity) {}
    }
}
