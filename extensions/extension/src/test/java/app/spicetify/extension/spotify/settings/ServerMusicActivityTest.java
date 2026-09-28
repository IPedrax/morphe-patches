package app.spicetify.extension.spotify.settings;

import static org.junit.Assert.*;
import android.app.Application;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import app.spicetify.extension.spotify.localserver.ServerConfig;
import java.time.Duration;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 32, manifest = Config.NONE)
public class ServerMusicActivityTest {
    @Test public void emptyLibraryShowsRecoveryAndKeepsSearchAvailable() {
        Application application = RuntimeEnvironment.getApplication();
        ServerConfig.initialize(application);
        ServerConfig.forget();
        ActivityController<ServerMusicActivity> controller = Robolectric.buildActivity(ServerMusicActivity.class)
                .create().start().resume();
        ServerMusicActivity activity = controller.get();
        View root = activity.findViewById(android.R.id.content);
        TextView status = findText(root, "No server tracks are ready.");
        assertNotNull(status);
        assertTrue(status.getText().toString().contains("Return to Spicetify settings"));
        Button search = (Button) findText(root, "Search");
        assertNotNull(search);
        search.performClick();
        EditText input = findInput(root);
        assertNotNull(input);
        assertEquals(View.VISIBLE, input.getVisibility());
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofSeconds(1));
        assertEquals(View.VISIBLE, input.getVisibility());
        controller.pause();
    }

    private static TextView findText(View view, String text) {
        if (view instanceof TextView && ((TextView) view).getText().toString().contains(text)) return (TextView) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                TextView match = findText(group.getChildAt(i), text);
                if (match != null) return match;
            }
        }
        return null;
    }

    private static EditText findInput(View view) {
        if (view instanceof EditText) return (EditText) view;
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                EditText match = findInput(group.getChildAt(i));
                if (match != null) return match;
            }
        }
        return null;
    }
}
