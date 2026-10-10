package in.indrajeeth.packkeep.client;

import net.minecraft.client.resources.server.PackReloadConfig;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Tracks which server packs are currently live and decides when an incoming reload would just
 * rebuild what is already applied. A pack is keyed by the SHA-1 filename Minecraft gives its
 * download, so the same bytes under a new server id still key the same. Files not named by a hash
 * (singleplayer resources, dev packs) key to a sentinel that never matches, so they always reload.
 *
 * <p>Live state is committed only once a reload actually succeeds, and is scoped to the server
 * address it came from so a held pack is never carried onto a different server.
 */
public final class ResidentPacks {
    private static final String NO_KEY = "\u0000";

    private static volatile List<String> live = List.of();
    private static volatile boolean everLoaded = false;
    private static volatile boolean leaving = false;
    private static volatile String heldServer = null;

    private ResidentPacks() {}

    public static List<String> keysOf(List<PackReloadConfig.IdAndPath> packs) {
        List<String> keys = new ArrayList<>(packs.size());
        for (PackReloadConfig.IdAndPath pack : packs) {
            Path name = pack.path().getFileName();
            String s = name == null ? "" : name.toString().toLowerCase(Locale.ROOT);
            keys.add(looksHashed(s) ? s : NO_KEY);
        }
        return keys;
    }

    private static boolean looksHashed(String s) {
        if (s.length() != 40) {
            return false;
        }
        for (int i = 0; i < 40; i++) {
            char c = s.charAt(i);
            if (!((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f'))) {
                return false;
            }
        }
        return true;
    }

    public static boolean identifiable(List<String> keys) {
        return !keys.contains(NO_KEY);
    }

    /** The incoming set is exactly what is already applied, so the reload would change nothing. */
    public static boolean matchesLive(List<String> incoming) {
        return identifiable(incoming) && !live.isEmpty() && incoming.equals(live);
    }

    /** Empty reload fired while packs are held; the caller gates this on a real disconnect. */
    public static boolean holding(List<String> incoming) {
        return incoming.isEmpty() && everLoaded && !live.isEmpty();
    }

    public static void markLeaving() {
        leaving = true;
    }

    public static boolean consumeLeaving() {
        if (leaving) {
            leaving = false;
            return true;
        }
        return false;
    }

    public static void markLive(List<String> applied, String server) {
        live = List.copyOf(applied);
        heldServer = server;
        if (!applied.isEmpty()) {
            everLoaded = true;
        }
    }

    public static List<String> live() {
        return live;
    }

    public static String heldServer() {
        return heldServer;
    }

    /** A held pack belongs to one server; joining a different address must not inherit it. */
    public static boolean foreignTo(String server) {
        return !live.isEmpty() && !Objects.equals(server, heldServer);
    }

    public static void drop() {
        live = List.of();
        everLoaded = false;
        heldServer = null;
    }
}
