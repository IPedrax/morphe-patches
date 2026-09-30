package app.spicetify.extension.spotify.localserver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable browse view of one completed server scan. No network or Android state lives here. */
public final class MusicCatalog {
    public static final class Track {
        public final String id, title, album, artist;
        public final int durationSeconds, discNumber, trackNumber;

        private Track(RemoteTrack source) {
            id = source.id;
            title = source.displayTitle();
            album = source.album;
            artist = source.artist;
            durationSeconds = source.durationSeconds;
            discNumber = source.browse.discNumber;
            trackNumber = source.browse.trackNumber;
        }
    }

    public static final class Album {
        public final String id, title, artist;
        /** Jellyfin's primary image tag for the album, or empty when the server has none. */
        public final String imageTag;
        public final List<Track> tracks;

        private Album(String id, String title, String artist, String imageTag, List<Track> tracks) {
            this.id = id;
            this.title = title;
            this.artist = artist;
            this.imageTag = imageTag;
            this.tracks = Collections.unmodifiableList(new ArrayList<>(tracks));
        }
    }

    public static final class Artist {
        public final String id, name;
        public final List<String> albumIds;
        public final List<String> otherTrackIds;

        private Artist(String id, String name, List<String> albumIds, List<String> trackIds) {
            this.id = id;
            this.name = name;
            this.albumIds = Collections.unmodifiableList(albumIds);
            this.otherTrackIds = Collections.unmodifiableList(trackIds);
        }
    }

    public static final class SearchResults {
        public final List<Artist> artists;
        public final List<Album> albums;
        public final List<Track> tracks;

        private SearchResults(List<Artist> artists, List<Album> albums, List<Track> tracks) {
            this.artists = Collections.unmodifiableList(artists);
            this.albums = Collections.unmodifiableList(albums);
            this.tracks = Collections.unmodifiableList(tracks);
        }
    }

    private static final class AlbumBuilder {
        final String id, title, artist;
        String imageTag = "";
        final List<Track> tracks = new ArrayList<>();

        AlbumBuilder(String id, String title, String artist) {
            this.id = id; this.title = title; this.artist = artist;
        }
    }

    private static final class ArtistBuilder {
        final String id, name;
        final LinkedHashSet<String> albumIds = new LinkedHashSet<>();
        final LinkedHashSet<String> trackIds = new LinkedHashSet<>();

        ArtistBuilder(String id, String name) { this.id = id; this.name = name; }
    }

    private final List<Album> albums;
    private final List<Artist> artists;
    private final List<Track> tracks;
    private final Map<String, Album> albumsById;
    private final Map<String, Artist> artistsById;
    private final Map<String, Track> tracksById;
    private final List<String> albumSearch;
    private final List<String> artistSearch;
    private final List<String> trackSearch;

    private MusicCatalog(List<Album> albums, List<Artist> artists, List<Track> tracks) {
        this.albums = Collections.unmodifiableList(albums);
        this.artists = Collections.unmodifiableList(artists);
        this.tracks = Collections.unmodifiableList(tracks);
        Map<String, Album> byAlbum = new HashMap<>();
        for (Album album : albums) byAlbum.put(album.id, album);
        albumsById = Collections.unmodifiableMap(byAlbum);
        Map<String, Artist> byArtist = new HashMap<>();
        for (Artist artist : artists) byArtist.put(artist.id, artist);
        artistsById = Collections.unmodifiableMap(byArtist);
        Map<String, Track> byTrack = new HashMap<>();
        for (Track track : tracks) byTrack.put(track.id, track);
        tracksById = Collections.unmodifiableMap(byTrack);
        albumSearch = new ArrayList<>(albums.size());
        for (Album album : albums) albumSearch.add(folded(album.title) + "\n" + folded(album.artist));
        artistSearch = new ArrayList<>(artists.size());
        for (Artist artist : artists) artistSearch.add(folded(artist.name));
        trackSearch = new ArrayList<>(tracks.size());
        for (Track track : tracks) trackSearch.add(folded(track.title) + "\n" + folded(track.artist));
    }

    static MusicCatalog empty() {
        return new MusicCatalog(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
    }

    static MusicCatalog from(List<RemoteTrack> sources) {
        Map<String, AlbumBuilder> albumBuilders = new LinkedHashMap<>();
        Map<String, ArtistBuilder> artistBuilders = new LinkedHashMap<>();
        Map<String, LinkedHashSet<String>> jellyfinAlbumIds = new HashMap<>();
        for (RemoteTrack source : sources) {
            if (source.providerIdentity != null && !source.browse.albumId.isEmpty())
                jellyfinAlbumIds.computeIfAbsent(albumNameKey(source), ignored -> new LinkedHashSet<>())
                        .add(source.browse.albumId);
        }
        List<Track> tracks = new ArrayList<>(sources.size());
        for (RemoteTrack source : sources) {
            Track track = new Track(source);
            tracks.add(track);
            String albumId = albumId(source, jellyfinAlbumIds);
            String albumTitle = source.album.isEmpty() ? "Unknown album" : source.album;
            String albumArtist = source.browse.albumArtist.isEmpty() ? source.artist : source.browse.albumArtist;
            AlbumBuilder album = albumBuilders.computeIfAbsent(albumId,
                    ignored -> new AlbumBuilder(albumId, albumTitle, albumArtist));
            album.tracks.add(track);
            if (album.imageTag.isEmpty()) album.imageTag = source.browse.albumImageTag;

            List<BrowseMetadata.ArtistCredit> credits = source.browse.artists;
            if (credits.isEmpty() && !source.artist.isEmpty())
                credits = Collections.singletonList(new BrowseMetadata.ArtistCredit("", source.artist));
            if (credits.isEmpty())
                credits = Collections.singletonList(new BrowseMetadata.ArtistCredit("", "Unknown artist"));
            for (BrowseMetadata.ArtistCredit credit : credits) {
                ArtistBuilder artist = artist(artistBuilders, credit);
                artist.trackIds.add(track.id);
            }
            List<BrowseMetadata.ArtistCredit> albumCredits = source.browse.albumArtists;
            if (albumCredits.isEmpty() && !albumArtist.isEmpty())
                albumCredits = Collections.singletonList(new BrowseMetadata.ArtistCredit("", albumArtist));
            for (BrowseMetadata.ArtistCredit credit : albumCredits) {
                ArtistBuilder artist = artist(artistBuilders, credit);
                artist.albumIds.add(albumId);
                artist.trackIds.add(track.id);
            }
        }

        Comparator<Track> albumOrder = Comparator
                .comparingInt((Track track) -> knownOrder(track.discNumber))
                .thenComparingInt(track -> knownOrder(track.trackNumber))
                .thenComparing(track -> folded(track.title))
                .thenComparing(track -> track.id);
        List<Album> albums = new ArrayList<>(albumBuilders.size());
        for (AlbumBuilder value : albumBuilders.values()) {
            value.tracks.sort(albumOrder);
            albums.add(new Album(value.id, value.title, value.artist, value.imageTag, value.tracks));
        }
        albums.sort(Comparator.comparing((Album album) -> folded(album.title))
                .thenComparing(album -> folded(album.artist)).thenComparing(album -> album.id));
        Map<String, Album> albumsById = new HashMap<>();
        Map<String, Integer> albumOrderIndex = new HashMap<>();
        for (int i = 0; i < albums.size(); i++) {
            albumsById.put(albums.get(i).id, albums.get(i));
            albumOrderIndex.put(albums.get(i).id, i);
        }
        Map<String, Track> tracksById = new HashMap<>();
        for (Track track : tracks) tracksById.put(track.id, track);
        List<Artist> artists = new ArrayList<>(artistBuilders.size());
        for (ArtistBuilder value : artistBuilders.values()) {
            List<String> albumIds = new ArrayList<>(value.albumIds);
            albumIds.sort(Comparator.comparingInt(albumOrderIndex::get));
            LinkedHashSet<String> albumTracks = new LinkedHashSet<>();
            for (String albumId : albumIds)
                for (Track track : albumsById.get(albumId).tracks) albumTracks.add(track.id);
            List<String> otherTracks = new ArrayList<>();
            for (String trackId : value.trackIds)
                if (!albumTracks.contains(trackId)) otherTracks.add(trackId);
            otherTracks.sort(Comparator.comparing((String id) -> folded(tracksById.get(id).album))
                    .thenComparing(id -> folded(tracksById.get(id).title)).thenComparing(id -> id));
            artists.add(new Artist(value.id, value.name, albumIds, otherTracks));
        }
        artists.sort(Comparator.comparing((Artist artist) -> folded(artist.name)).thenComparing(artist -> artist.id));
        tracks.sort(Comparator.comparing((Track track) -> folded(track.title)).thenComparing(track -> track.id));
        return new MusicCatalog(albums, artists, tracks);
    }

    private static ArtistBuilder artist(Map<String, ArtistBuilder> artists, BrowseMetadata.ArtistCredit credit) {
        String name = credit.name.isEmpty() ? "Unknown artist" : credit.name;
        String id = credit.id.isEmpty() ? "name:" + folded(name) : "id:" + credit.id;
        return artists.computeIfAbsent(id, ignored -> new ArtistBuilder(id, name));
    }

    private static String albumId(RemoteTrack track, Map<String, LinkedHashSet<String>> jellyfinAlbumIds) {
        if (!track.browse.albumId.isEmpty()) return "id:" + track.browse.albumId;
        if (track.providerIdentity != null) {
            if (track.album.isEmpty()) return "unidentified:" + track.id;
            String name = albumNameKey(track);
            LinkedHashSet<String> knownIds = jellyfinAlbumIds.get(name);
            if (knownIds != null && knownIds.size() == 1) return "id:" + knownIds.iterator().next();
            return "name:" + name;
        }
        String path = track.url.getPath();
        String folder = path.substring(0, path.lastIndexOf('/') + 1);
        return "folder:" + folder + "\n" + folded(track.album) + "\n" + folded(track.browse.albumArtist);
    }

    private static String albumNameKey(RemoteTrack track) {
        String artist = track.browse.albumArtist.isEmpty() ? track.artist : track.browse.albumArtist;
        return folded(track.album) + "\n" + folded(artist);
    }

    private static int knownOrder(int value) { return value == 0 ? Integer.MAX_VALUE : value; }
    private static String folded(String value) { return value.toLowerCase(Locale.ROOT).trim(); }

    public int trackCount() { return tracks.size(); }
    public int albumCount() { return albums.size(); }
    public int artistCount() { return artists.size(); }
    public Album album(String id) { return albumsById.get(id); }
    public Artist artist(String id) { return artistsById.get(id); }
    public Track track(String id) { return tracksById.get(id); }
    public List<Album> albums(int offset, int limit) { return page(albums, offset, limit); }
    List<Album> allAlbums() { return albums; }
    List<Artist> allArtists() { return artists; }
    public List<Artist> artists(int offset, int limit) { return page(artists, offset, limit); }
    public List<Track> songs(int offset, int limit) { return page(tracks, offset, limit); }

    public SearchResults search(String query, int limit) {
        if (limit < 1 || limit > 100) throw new IllegalArgumentException("Search page size must be 1 to 100.");
        String needle = folded(query);
        if (needle.isEmpty()) return new SearchResults(Collections.emptyList(), Collections.emptyList(), Collections.emptyList());
        List<Artist> artistMatches = new ArrayList<>();
        List<Album> albumMatches = new ArrayList<>();
        List<Track> trackMatches = new ArrayList<>();
        for (int i = 0; i < artists.size() && artistMatches.size() < limit; i++)
            if (artistSearch.get(i).contains(needle)) artistMatches.add(artists.get(i));
        for (int i = 0; i < albums.size() && albumMatches.size() < limit; i++)
            if (albumSearch.get(i).contains(needle)) albumMatches.add(albums.get(i));
        for (int i = 0; i < tracks.size() && trackMatches.size() < limit; i++)
            if (trackSearch.get(i).contains(needle)) trackMatches.add(tracks.get(i));
        return new SearchResults(artistMatches, albumMatches, trackMatches);
    }

    private static <T> List<T> page(List<T> values, int offset, int limit) {
        if (offset < 0 || limit < 1 || limit > 100) throw new IllegalArgumentException("Invalid catalog page.");
        if (offset >= values.size()) return Collections.emptyList();
        return values.subList(offset, Math.min(values.size(), offset + limit));
    }
}
