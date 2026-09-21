package app.spicetify.extension.spotify.localserver;

import java.net.URI;

/** Immutable selected account and music library. Credentials stay off track URLs. */
public final class JellyfinConnection {
    public final URI root;
    public final String userId, userName, libraryId, libraryName;
    private final JellyfinClient.Account account;
    private final String identity;

    JellyfinConnection(JellyfinClient.Account account, String libraryId, String libraryName) {
        this.account = account; root = account.root(); userId = account.userId; userName = account.userName;
        this.libraryId = JellyfinClient.id(libraryId);
        if (libraryName == null || libraryName.isEmpty() || libraryName.length() > 512) throw new IllegalArgumentException("Invalid library name.");
        this.libraryName = libraryName;
        identity = "jellyfin\n" + root + "\n" + userId + "\n" + this.libraryId;
    }
    static JellyfinConnection restore(String root, String deviceId, String userId, String userName,
            String token, String libraryId, String libraryName) {
        return new JellyfinConnection(new JellyfinClient.Account(new ServerConnection(root, "", ""), deviceId, userId, userName, token), libraryId, libraryName);
    }
    JellyfinClient.Account account() { return account; }
    String deviceId() { return account.deviceId(); }
    String token() { return account.token(); }
    String identity() { return identity; }
}
