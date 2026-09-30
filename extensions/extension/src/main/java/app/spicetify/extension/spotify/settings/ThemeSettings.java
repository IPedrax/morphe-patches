package app.spicetify.extension.spotify.settings;

import android.app.Activity;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import app.spicetify.extension.spotify.theme.ThemeOverlay;
import java.util.regex.Pattern;

/** The Appearance page: in-app theme colours on Android 11+, otherwise a note about Morphe Manager. */
final class ThemeSettings {
    private static final Pattern HEX = Pattern.compile("#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})");
    static final int[] BACKGROUNDS = {0xFF000000, 0xFF121212, 0xFF0B1026, 0xFF1E1233, 0xFF0E2016, 0xFF261010};
    static final int[] ACCENTS = {0xFF1ED760, 0xFF509BF5, 0xFFF573A0, 0xFFFF6437, 0xFFF59B23, 0xFFB49BC8};

    private ThemeSettings() {}

    static void build(Activity activity, LinearLayout content) {
        if (!ThemeOverlay.active()) {
            SpotifyStyle.infoRow(content, "Theme colors",
                    "Your colors were selected in Morphe Manager. Change those options and repatch Spotify to use different colors.");
            return;
        }
        TextView intro = SpotifyStyle.body(activity, "Choose colors here instead of repatching. Restart Spotify to apply them. "
                + "Some screens and hardcoded colors keep Spotify's own colors.");
        intro.setPadding(SpotifyStyle.dp(activity, 16), SpotifyStyle.dp(activity, 16), SpotifyStyle.dp(activity, 16), SpotifyStyle.dp(activity, 8));
        content.addView(intro);
        SpotifyStyle.colorRow(content, "Background", ThemeOverlay.background(),
                view -> pick(activity, "Background color", ThemeOverlay.background(), BACKGROUNDS,
                        color -> save(activity, content, color, PatchSettings.themeAccent())));
        SpotifyStyle.colorRow(content, "Accent", ThemeOverlay.accent(),
                view -> pick(activity, "Accent color", ThemeOverlay.accent(), ACCENTS,
                        color -> save(activity, content, PatchSettings.themeBackground(), color)));
        if (PatchSettings.themeBackground() != null || PatchSettings.themeAccent() != null) {
            SpotifyStyle.actionRow(content, "Use Morphe Manager colors",
                    "Return to " + SpotifyStyle.hex(ThemeOverlay.patchedBackground()) + " and "
                            + SpotifyStyle.hex(ThemeOverlay.patchedAccent()) + ", the colors chosen while patching.",
                    view -> save(activity, content, null, null));
        }
    }

    interface Choice {
        void chosen(int color);
    }

    static Integer parse(String value) {
        String trimmed = value.trim();
        if (!HEX.matcher(trimmed).matches()) return null;
        String digits = trimmed.startsWith("#") ? trimmed.substring(1) : trimmed;
        long parsed = Long.parseLong(digits, 16);
        return digits.length() == 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
    }

    private static void pick(Activity activity, String title, int current, int[] presets, Choice choice) {
        EditText hex = new EditText(activity);
        hex.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        hex.setSingleLine(true);
        hex.setText(SpotifyStyle.hex(current));
        hex.setHint("#RRGGBB");
        hex.setContentDescription("Hex color");
        SpotifyStyle.style(hex);
        hex.setTypeface(SpotifyStyle.font(activity, SpotifyStyle.Font.REGULAR));
        hex.setGravity(Gravity.CENTER);

        LinearLayout swatches = new LinearLayout(activity);
        swatches.setOrientation(LinearLayout.HORIZONTAL);
        swatches.setGravity(Gravity.CENTER);
        View[] views = new View[presets.length];
        for (int i = 0; i < presets.length; i++) {
            int color = presets[i];
            View swatch = new View(activity);
            swatch.setContentDescription(SpotifyStyle.hex(color));
            swatch.setOnClickListener(view -> hex.setText(SpotifyStyle.hex(color)));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(SpotifyStyle.dp(activity, 40), SpotifyStyle.dp(activity, 40));
            params.setMargins(SpotifyStyle.dp(activity, 6), 0, SpotifyStyle.dp(activity, 6), 0);
            swatches.addView(swatch, params);
            views[i] = swatch;
        }
        Runnable highlight = () -> {
            Integer typed = parse(hex.getText().toString());
            for (int i = 0; i < presets.length; i++) {
                views[i].setBackground(SpotifyStyle.swatch(activity, presets[i], typed != null && typed == presets[i]));
            }
        };
        highlight.run();
        hex.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { hex.setError(null); highlight.run(); }
            @Override public void afterTextChanged(Editable text) {}
        });

        LinearLayout body = SpotifyStyle.column(activity);
        body.addView(swatches);
        LinearLayout.LayoutParams fieldParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        fieldParams.topMargin = SpotifyStyle.dp(activity, 16);
        body.addView(hex, fieldParams);

        new SpotifySheet(activity, title, null)
                .view(body)
                .primary("Save", () -> {
                    Integer color = parse(hex.getText().toString());
                    if (color == null) {
                        hex.setError("Use #RRGGBB or #AARRGGBB.");
                        return false;
                    }
                    choice.chosen(color);
                    return true;
                })
                .secondary("Cancel")
                .show();
    }

    private static void save(Activity activity, LinearLayout content, Integer background, Integer accent) {
        PatchSettings.setThemeColors(background, accent);
        content.removeAllViews();
        build(activity, content);
        boolean applied = ThemeOverlay.refresh();
        if (!applied) {
            new SpotifySheet(activity, "Colors not applied",
                    "Spotify could not load the new colors. They are saved and will be tried again when Spotify restarts.")
                    .primary("OK", () -> true).show();
        }
    }
}
