package app.spicetify.extension.spotify.localserver;

/** Artwork URLs for catalog items, scaled by the server to {@code size} pixels; empty when there is none. */
public final class ServerArtwork {
    private ServerArtwork() {}

    public static String album(MusicCatalog.Album album, int size) {
        return album == null ? "" : LibraryRows.image(ServerConfig.snapshot().jellyfinConnection(), album.id, album.imageTag, size);
    }

    public static String artist(MusicCatalog.Artist artist, int size) {
        return artist == null ? "" : LibraryRows.image(ServerConfig.snapshot().jellyfinConnection(), artist.id, "", size);
    }
}
