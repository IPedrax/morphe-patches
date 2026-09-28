package app.spicetify.extension.spotify.settings;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Build;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.window.OnBackInvokedCallback;
import android.window.OnBackInvokedDispatcher;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import app.spicetify.extension.spotify.localserver.MusicCatalog;
import app.spicetify.extension.spotify.localserver.ServerIndex;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Browses the current server scan inside Spotify; playback remains in Local Files for now. */
public final class ServerMusicActivity extends Activity {
    private static final int PAGE_SIZE = 80;
    private static final int WHITE = Color.WHITE;
    private static final int MUTED = Color.rgb(179, 179, 179);
    private static final int SURFACE = Color.rgb(18, 18, 18);
    private enum ViewMode { ALBUMS, ARTISTS, SONGS, SEARCH, ALBUM, ARTIST }
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Row> rows = new ArrayList<>();
    private final ArrayDeque<NavState> history = new ArrayDeque<>();
    private final RowAdapter adapter = new RowAdapter();
    private final Runnable refresh = new Runnable() {
        @Override public void run() {
            MusicCatalog current = ServerIndex.catalog();
            if (current.trackCount() > 0 && catalog != current) {
                catalog = current;
                mode = ViewMode.ALBUMS;
                selectedId = null;
                offset = 0;
                history.clear();
                render();
            } else if (catalog.trackCount() > 0 && !ServerIndex.isCurrent(catalog)) {
                catalog = current;
                stale();
            } else if (catalog.trackCount() == 0) {
                showEmptyStatus();
            }
            handler.postDelayed(this, 1000);
        }
    };
    private MusicCatalog catalog;
    private ViewMode mode = ViewMode.ALBUMS;
    private String selectedId;
    private int offset;
    private TextView status;
    private EditText search;
    private Button previous, next;
    private ListView list;
    private Object backCallback;

    public static void open(Context context) {
        context.startActivity(new Intent(context, ServerMusicActivity.class));
    }

    @Override protected void onCreate(Bundle state) {
        setTheme(android.R.style.Theme_Material);
        super.onCreate(state);
        setTitle("Server music");
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
            getActionBar().setBackgroundDrawable(new ColorDrawable(SURFACE));
        }
        catalog = ServerIndex.catalog();
        LinearLayout insetRoot = new LinearLayout(this);
        insetRoot.setFitsSystemWindows(true);
        insetRoot.setBackgroundColor(SURFACE);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(SURFACE);
        root.setPadding(dp(16), dp(8), dp(16), dp(8));

        LinearLayout tabs = new LinearLayout(this);
        tabs.setOrientation(LinearLayout.HORIZONTAL);
        for (ViewMode tab : new ViewMode[] {ViewMode.ALBUMS, ViewMode.ARTISTS, ViewMode.SONGS, ViewMode.SEARCH}) {
            Button button = new Button(this);
            button.setText(tab == ViewMode.SEARCH ? "Search" : title(tab));
            button.setTextSize(12);
            button.setMinHeight(dp(48));
            tabs.addView(button, new LinearLayout.LayoutParams(0, dp(56), 1));
            button.setOnClickListener(view -> { mode = tab; offset = 0; selectedId = null; history.clear(); render(); });
        }
        root.addView(tabs);

        search = new EditText(this);
        search.setSingleLine(true);
        search.setHint("Search server music");
        search.setTextColor(WHITE);
        search.setHintTextColor(MUTED);
        search.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence value, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (mode == ViewMode.SEARCH) render();
            }
            @Override public void afterTextChanged(Editable value) {}
        });
        root.addView(search);
        status = new TextView(this);
        status.setTextColor(MUTED);
        status.setTextSize(14);
        status.setPadding(dp(8), dp(8), dp(8), dp(8));
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        root.addView(status);

        list = new ListView(this);
        list.setDivider(new ColorDrawable(Color.rgb(45, 45, 45)));
        list.setDividerHeight(dp(1));
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent, view, position, id) -> openRow(rows.get(position)));
        root.addView(list, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));

        LinearLayout pager = new LinearLayout(this);
        previous = new Button(this);
        previous.setText("Previous");
        previous.setOnClickListener(view -> { offset = Math.max(0, offset - PAGE_SIZE); render(); list.setSelection(0); });
        next = new Button(this);
        next.setText("Next");
        next.setOnClickListener(view -> { offset += PAGE_SIZE; render(); list.setSelection(0); });
        pager.addView(previous, new LinearLayout.LayoutParams(0, dp(52), 1));
        pager.addView(next, new LinearLayout.LayoutParams(0, dp(52), 1));
        root.addView(pager);
        TextView guidance = new TextView(this);
        guidance.setText("Browse albums and artists here. Play server tracks from Spotify's Local Files while album playback is being tested.");
        guidance.setTextColor(MUTED);
        guidance.setTextSize(12);
        guidance.setPadding(dp(8), dp(8), dp(8), dp(8));
        root.addView(guidance);
        insetRoot.addView(root, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(insetRoot);
        if (Build.VERSION.SDK_INT >= 33) backCallback = Api33.register(this);
        render();
    }

    @Override protected void onResume() { super.onResume(); handler.post(refresh); }
    @Override protected void onPause() { handler.removeCallbacks(refresh); super.onPause(); }
    @Override protected void onDestroy() {
        if (Build.VERSION.SDK_INT >= 33 && backCallback != null)
            Api33.unregister(this, backCallback);
        super.onDestroy();
    }

    private void render() {
        if (status == null) return;
        search.setVisibility(mode == ViewMode.SEARCH ? View.VISIBLE : View.GONE);
        rows.clear();
        if (catalog.trackCount() == 0) {
            showEmptyStatus();
            previous.setVisibility(View.GONE);
            next.setVisibility(View.GONE);
            adapter.notifyDataSetChanged();
            return;
        }
        int total = 0;
        switch (mode) {
            case ALBUMS:
                total = catalog.albumCount();
                for (MusicCatalog.Album album : catalog.albums(offset, PAGE_SIZE))
                    rows.add(new Row(album.title, album.artist + " · " + album.tracks.size() + " tracks", ViewMode.ALBUM, album.id));
                break;
            case ARTISTS:
                total = catalog.artistCount();
                for (MusicCatalog.Artist artist : catalog.artists(offset, PAGE_SIZE))
                    rows.add(new Row(artist.name, artist.albumIds.size() + " albums", ViewMode.ARTIST, artist.id));
                break;
            case SONGS:
                total = catalog.trackCount();
                for (MusicCatalog.Track track : catalog.songs(offset, PAGE_SIZE))
                    rows.add(new Row(track.title, track.artist + " · " + track.album, null, track.id));
                break;
            case SEARCH:
                String query = search.getText().toString().trim();
                if (!query.isEmpty()) {
                    MusicCatalog.SearchResults results = catalog.search(query, 30);
                    for (MusicCatalog.Artist artist : results.artists)
                        rows.add(new Row(artist.name, "Artist", ViewMode.ARTIST, artist.id));
                    for (MusicCatalog.Album album : results.albums)
                        rows.add(new Row(album.title, "Album · " + album.artist, ViewMode.ALBUM, album.id));
                    for (MusicCatalog.Track track : results.tracks)
                        rows.add(new Row(track.title, "Song · " + track.artist, null, track.id));
                }
                break;
            case ALBUM:
                MusicCatalog.Album album = catalog.album(selectedId);
                if (album == null) { stale(); return; }
                total = album.tracks.size();
                for (MusicCatalog.Track track : page(album.tracks))
                    rows.add(new Row(track.title, position(track) + track.artist,
                            null, track.id));
                break;
            case ARTIST:
                MusicCatalog.Artist artist = catalog.artist(selectedId);
                if (artist == null) { stale(); return; }
                total = artist.albumIds.size() + artist.otherTrackIds.size();
                for (int i = offset; i < Math.min(total, offset + PAGE_SIZE); i++) {
                    if (i < artist.albumIds.size()) {
                        MusicCatalog.Album artistAlbum = catalog.album(artist.albumIds.get(i));
                        if (artistAlbum != null)
                            rows.add(new Row(artistAlbum.title, "Album · " + artistAlbum.artist, ViewMode.ALBUM, artistAlbum.id));
                    } else {
                        MusicCatalog.Track artistTrack = catalog.track(artist.otherTrackIds.get(i - artist.albumIds.size()));
                        if (artistTrack != null)
                            rows.add(new Row(artistTrack.title, "Song · " + artistTrack.album, null, artistTrack.id));
                    }
                }
                break;
        }
        String location = mode == ViewMode.ALBUM ? catalog.album(selectedId).title
                : mode == ViewMode.ARTIST ? catalog.artist(selectedId).name : title(mode);
        if (mode == ViewMode.SEARCH) {
            status.setText(search.getText().length() == 0 ? "Search albums, artists, and songs"
                    : rows.isEmpty() ? "No matches in this server library"
                    : "Search · " + rows.size() + " results shown");
        } else {
            String shown = total == 0 ? "0" : rows.size() == 1 ? Integer.toString(offset + 1)
                    : (offset + 1) + "–" + (offset + rows.size());
            status.setText(location + " · " + shown
                    + " of " + total);
        }
        previous.setVisibility(offset > 0 && mode != ViewMode.SEARCH ? View.VISIBLE : View.INVISIBLE);
        next.setVisibility(offset + PAGE_SIZE < total ? View.VISIBLE : View.INVISIBLE);
        adapter.notifyDataSetChanged();
    }

    private <T> List<T> page(List<T> source) {
        return offset >= source.size() ? Collections.emptyList()
                : source.subList(offset, Math.min(source.size(), offset + PAGE_SIZE));
    }

    private void showEmptyStatus() {
        status.setText("No server tracks are ready.\nScan status: " + ServerIndex.status()
                + "\nReturn to Spicetify settings to scan a library.");
    }

    private void stale() {
        mode = ViewMode.ALBUMS;
        selectedId = null;
        offset = 0;
        history.clear();
        render();
    }

    private void openRow(Row row) {
        if (row.destination == null) return;
        history.push(new NavState(mode, selectedId, offset));
        mode = row.destination;
        selectedId = row.id;
        offset = 0;
        render();
    }

    private static String title(ViewMode mode) {
        switch (mode) {
            case ARTISTS: return "Artists";
            case SONGS: return "Songs";
            case SEARCH: return "Search";
            default: return "Albums";
        }
    }

    private static String position(MusicCatalog.Track track) {
        if (track.trackNumber == 0) return "";
        return (track.discNumber > 0 ? track.discNumber + "." : "") + track.trackNumber + " · ";
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (!backToList()) finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("GestureBackNavigation")
    @Override public void onBackPressed() {
        if (!backToList()) super.onBackPressed();
    }

    private boolean backToList() {
        if (history.isEmpty()) return false;
        NavState previous = history.pop();
        mode = previous.mode;
        selectedId = previous.id;
        offset = previous.offset;
        render();
        return true;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    private static final class Row {
        final String title, subtitle, id;
        final ViewMode destination;
        Row(String title, String subtitle, ViewMode destination, String id) {
            this.title = title; this.subtitle = subtitle; this.destination = destination; this.id = id;
        }
    }

    private static final class NavState {
        final ViewMode mode;
        final String id;
        final int offset;
        NavState(ViewMode mode, String id, int offset) {
            this.mode = mode; this.id = id; this.offset = offset;
        }
    }

    @TargetApi(33)
    private static final class Api33 {
        static Object register(ServerMusicActivity activity) {
            OnBackInvokedCallback callback = () -> { if (!activity.backToList()) activity.finish(); };
            activity.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback);
            return callback;
        }
        static void unregister(ServerMusicActivity activity, Object callback) {
            activity.getOnBackInvokedDispatcher().unregisterOnBackInvokedCallback((OnBackInvokedCallback) callback);
        }
    }

    private final class RowAdapter extends BaseAdapter {
        @Override public int getCount() { return rows.size(); }
        @Override public Object getItem(int position) { return rows.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public View getView(int position, View reusable, ViewGroup parent) {
            LinearLayout cell;
            if (reusable instanceof LinearLayout) cell = (LinearLayout) reusable;
            else {
                cell = new LinearLayout(ServerMusicActivity.this);
                cell.setOrientation(LinearLayout.VERTICAL);
                cell.setPadding(dp(12), dp(8), dp(12), dp(8));
                cell.setMinimumHeight(dp(64));
                TextView title = new TextView(ServerMusicActivity.this);
                title.setTextColor(WHITE);
                title.setTextSize(16);
                title.setTypeface(null, Typeface.BOLD);
                cell.addView(title);
                TextView subtitle = new TextView(ServerMusicActivity.this);
                subtitle.setTextColor(MUTED);
                subtitle.setTextSize(13);
                cell.addView(subtitle);
            }
            Row row = rows.get(position);
            ((TextView) cell.getChildAt(0)).setText(row.title);
            ((TextView) cell.getChildAt(1)).setText(row.subtitle);
            cell.setEnabled(row.destination != null);
            return cell;
        }
    }
}
