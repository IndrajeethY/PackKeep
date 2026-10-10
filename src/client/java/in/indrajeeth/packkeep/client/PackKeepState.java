package in.indrajeeth.packkeep.client;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class PackKeepState {
    private static final Set<String> resident = Collections.synchronizedSet(new HashSet<>());
    private static final Set<String> pending = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean disconnecting = false;
    private static volatile boolean retained = false;

    private PackKeepState() {}

    public static boolean isResident(String hash) {
        return hash != null && !hash.isEmpty() && resident.contains(norm(hash));
    }

    public static void markPending(String hash) {
        if (hash != null && !hash.isEmpty()) {
            pending.add(norm(hash));
        }
    }

    public static void promotePending() {
        synchronized (pending) {
            resident.addAll(pending);
            pending.clear();
        }
    }

    public static void clearPending() {
        pending.clear();
    }

    public static void clearResident() {
        resident.clear();
    }

    public static void beginDisconnect() {
        disconnecting = true;
    }

    public static boolean consumeDisconnectRetain() {
        if (disconnecting) {
            disconnecting = false;
            return true;
        }
        return false;
    }

    public static void setRetained(boolean value) {
        retained = value;
    }

    public static boolean isRetained() {
        return retained;
    }

    private static String norm(String h) {
        return h.toLowerCase(Locale.ROOT);
    }
}
