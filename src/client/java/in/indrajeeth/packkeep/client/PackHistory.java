package in.indrajeeth.packkeep.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.minecraft.client.resource.server.ReloadScheduler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Remembers the last server's applied packs on disk (with the server address they belong to) so
 * they can be reapplied during the startup splash. Entries whose file no longer exists are dropped
 * on read.
 */
public final class PackHistory {
    private static final Gson GSON = new GsonBuilder().create();

    private PackHistory() {}

    public static final class Snapshot {
        public final String server;
        public final List<ReloadScheduler.PackInfo> packs;

        Snapshot(String server, List<ReloadScheduler.PackInfo> packs) {
            this.server = server;
            this.packs = packs;
        }
    }

    private static final class Stored {
        String server;
        List<Entry> packs;
    }

    private static final class Entry {
        String id;
        String path;

        Entry(String id, String path) {
            this.id = id;
            this.path = path;
        }
    }

    public static void write(Path file, String server, List<ReloadScheduler.PackInfo> packs)
            throws IOException {
        Stored stored = new Stored();
        stored.server = server;
        stored.packs = new ArrayList<>(packs.size());
        for (ReloadScheduler.PackInfo p : packs) {
            stored.packs.add(new Entry(p.id().toString(), p.path().toAbsolutePath().toString()));
        }
        Files.createDirectories(file.toAbsolutePath().getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".writing");
        Files.writeString(tmp, GSON.toJson(stored), StandardCharsets.UTF_8);
        Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }

    public static Snapshot read(Path file) {
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            Stored stored =
                    GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), Stored.class);
            if (stored == null || stored.packs == null) {
                return null;
            }
            List<ReloadScheduler.PackInfo> packs = new ArrayList<>();
            for (Entry e : stored.packs) {
                try {
                    Path path = Path.of(e.path);
                    if (Files.isRegularFile(path)) {
                        packs.add(new ReloadScheduler.PackInfo(UUID.fromString(e.id), path));
                    }
                } catch (RuntimeException skip) {
                }
            }
            return new Snapshot(stored.server, packs);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }
}
