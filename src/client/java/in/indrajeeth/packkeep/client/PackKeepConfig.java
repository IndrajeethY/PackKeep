package in.indrajeeth.packkeep.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PackKeepConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path PATH =
            FabricLoader.getInstance().getConfigDir().resolve("packkeep.json");
    private static volatile PackKeepConfig INSTANCE = new PackKeepConfig();

    public boolean enabled = true;
    public boolean keepResidentAcrossDisconnect = true;
    public boolean skipReloadOnHashMatch = true;
    public boolean preWarmPinnedAtLaunch = true;
    public boolean autoAcceptRequired = false;
    public boolean verboseLog = false;

    public static PackKeepConfig get() {
        return INSTANCE;
    }

    public static void load() {
        try {
            if (Files.exists(PATH)) {
                PackKeepConfig loaded = GSON.fromJson(Files.readString(PATH), PackKeepConfig.class);
                INSTANCE = (loaded != null) ? loaded : new PackKeepConfig();
            } else {
                save();
            }
        } catch (IOException | RuntimeException e) {
            PackKeepClient.LOGGER.warn("[PackKeep] config load failed, using defaults", e);
            INSTANCE = new PackKeepConfig();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(PATH.getParent());
            Files.writeString(PATH, GSON.toJson(INSTANCE));
        } catch (IOException e) {
            PackKeepClient.LOGGER.warn("[PackKeep] config save failed", e);
        }
    }
}
