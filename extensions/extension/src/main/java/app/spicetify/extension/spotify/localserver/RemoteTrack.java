package app.spicetify.extension.spotify.localserver;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class RemoteTrack {
    public final URI url;
    public final String name;
    public final long size;
    public final String etag;
    public final String title;
    public final String album;
    public final String artist;
    public final int durationSeconds;
    public final String id;
    final String providerIdentity;

    RemoteTrack(ServerConnection connection, URI url, long size, String etag) {
        this(connection, url, size, etag, "", "", "", 0);
    }

    private RemoteTrack(ServerConnection connection, URI url, long size, String etag,
            String title, String album, String artist, int durationSeconds) {
        if (size <= 0 || size > 2L * 1024 * 1024 * 1024) throw new IllegalArgumentException("Unsupported audio file size.");
        this.providerIdentity = null;
        this.url = connection.resolve(connection.root, url.toASCIIString());
        this.name = url.getPath().substring(url.getPath().lastIndexOf('/') + 1);
        this.size = size;
        this.etag = etag;
        this.title = title;
        this.album = album;
        this.artist = artist;
        this.durationSeconds = durationSeconds;
        id = hash(connection.root + "\n" + connection.username + "\n" + url + "\n" + size + "\n" + etag);
    }

    RemoteTrack(JellyfinConnection connection, URI url, String itemId, String sourceId, long size, String version,
            String container, String title, String album, String artist, int duration) {
        if (size <= 0 || size > 2L * 1024 * 1024 * 1024) throw new IllegalArgumentException("Unsupported audio file size.");
        this.providerIdentity = connection.identity();
        this.url = url; this.size = size; this.etag = null;
        this.title = bounded(title); this.album = bounded(album); this.artist = bounded(artist);
        this.name = (this.title.isEmpty() ? itemId : this.title.replace('/', '_').replace('\\', '_')) + "." + container;
        this.durationSeconds = Math.max(0, duration);
        id = hash(providerIdentity + "\n" + itemId + "\n" + sourceId + "\n" + size + "\n" + version);
    }
    private static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            char[] hex = "0123456789abcdef".toCharArray(); char[] text = new char[digest.length * 2];
            for (int i = 0; i < digest.length; i++) { text[i * 2] = hex[(digest[i] & 255) >>> 4]; text[i * 2 + 1] = hex[digest[i] & 15]; }
            return new String(text);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new AssertionError(impossible); }
    }

    RemoteTrack withMetadata(ServerConnection connection, String title, String album, String artist, int duration) {
        return new RemoteTrack(connection, url, size, etag, bounded(title), bounded(album), bounded(artist), Math.max(0, duration));
    }
    private static String bounded(String value) { return value == null ? "" : value.substring(0, Math.min(value.length(), 512)); }
    public String displayTitle() {
        if (!title.isEmpty()) return title;
        int dot = name.lastIndexOf('.');
        return dot > 0 ? name.substring(0, dot) : name;
    }
}
