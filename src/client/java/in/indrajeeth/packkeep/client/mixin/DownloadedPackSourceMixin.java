package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackHistory;
import in.indrajeeth.packkeep.client.PackKeepClient;
import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.ResidentPacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.resources.server.DownloadedPackSource;
import net.minecraft.client.resources.server.PackReloadConfig;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.RepositorySource;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Mixin(DownloadedPackSource.class)
public class DownloadedPackSourceMixin {
    @Shadow @Final private static RepositorySource EMPTY_SOURCE;
    @Shadow private RepositorySource packSource;
    @Shadow private PackReloadConfig.Callbacks pendingReload;

    @Shadow
    private List<Pack> loadRequestedPacks(List<PackReloadConfig.IdAndPath> packs) {
        throw new AssertionError();
    }

    @Shadow
    private static RepositorySource configureSource(List<Pack> packs) {
        throw new AssertionError();
    }

    @Unique private boolean packkeep$fromHistory = false;
    @Unique private List<String> packkeep$pendingKeys = null;
    @Unique private List<PackReloadConfig.IdAndPath> packkeep$pendingSave = null;

    @Unique
    private static Path packkeep$historyFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("packkeep-history.json");
    }

    @Unique
    private static String packkeep$currentServer() {
        ServerData server = Minecraft.getInstance().getCurrentServer();
        return server == null ? null : server.ip;
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void packkeep$warmFromHistory(
            Minecraft minecraft, Path directory, GameConfig.UserData user, CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled || !cfg.preWarmPinnedAtLaunch) {
            return;
        }
        try {
            PackHistory.Snapshot saved = PackHistory.read(packkeep$historyFile());
            if (saved == null || saved.packs.isEmpty()) {
                return;
            }
            List<Pack> loaded = loadRequestedPacks(saved.packs);
            if (loaded == null) {
                return;
            }
            this.packSource = configureSource(loaded);
            ResidentPacks.markLive(ResidentPacks.keysOf(saved.packs), saved.server);
            packkeep$fromHistory = true;
            if (cfg.verboseLog) {
                PackKeepClient.LOGGER.info(
                        "[PackKeep] warmed {} pack(s) from the last server at startup",
                        saved.packs.size());
            }
        } catch (Exception e) {
            PackKeepClient.LOGGER.warn("[PackKeep] startup warm failed", e);
        }
    }

    @Inject(method = "configureForServerControl", at = @At("HEAD"))
    private void packkeep$onConnect(CallbackInfo ci) {
        if (!PackKeepConfig.get().enabled) {
            return;
        }
        if (ResidentPacks.foreignTo(packkeep$currentServer())) {
            ResidentPacks.drop();
            this.packSource = EMPTY_SOURCE;
            packkeep$fromHistory = false;
        }
    }

    /**
     * Skips the reload when it would change nothing: the requested packs are already applied, or it
     * is the empty reload a real disconnect fires while packs are held. Success is reported through
     * the vanilla callbacks (no forged status packets); cancelling leaves the live source
     * untouched, which is what keeps the pack resident. {@code pendingReload} stays null, which is
     * safe: every vanilla reader guards on non-null.
     */
    @Inject(method = "startReload", at = @At("HEAD"), cancellable = true)
    private void packkeep$maybeSkip(PackReloadConfig.Callbacks callbacks, CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled) {
            return;
        }
        List<String> keys;
        try {
            keys = ResidentPacks.keysOf(callbacks.packsToLoad());
        } catch (Exception e) {
            PackKeepClient.LOGGER.warn("[PackKeep] could not read requested packs; reloading", e);
            return;
        }
        // Consume the disconnect flag on every reload so it can never linger past the empty reload
        // it was meant for (otherwise a later mid-session pack removal would be wrongly held).
        boolean leaving = ResidentPacks.consumeLeaving();
        boolean skip =
                (cfg.skipReloadOnHashMatch && ResidentPacks.matchesLive(keys))
                        || (cfg.keepResidentAcrossDisconnect
                                && ResidentPacks.holding(keys)
                                && leaving);
        if (skip) {
            if (cfg.verboseLog) {
                PackKeepClient.LOGGER.info(
                        "[PackKeep] {} pack(s) already live; skipping reload",
                        ResidentPacks.live().size());
            }
            callbacks.onSuccess();
            ci.cancel();
            return;
        }
        packkeep$pendingKeys = keys;
        packkeep$fromHistory = false;
        packkeep$pendingSave =
                (cfg.preWarmPinnedAtLaunch && !keys.isEmpty() && ResidentPacks.identifiable(keys))
                        ? List.copyOf(callbacks.packsToLoad())
                        : null;
    }

    @Inject(method = "onReloadSuccess", at = @At("HEAD"))
    private void packkeep$onSuccess(CallbackInfo ci) {
        List<String> keys = packkeep$pendingKeys;
        List<PackReloadConfig.IdAndPath> toSave = packkeep$pendingSave;
        packkeep$pendingKeys = null;
        packkeep$pendingSave = null;
        if (this.pendingReload == null) {
            return;
        }
        String server = packkeep$currentServer();
        if (keys != null && this.packSource != EMPTY_SOURCE) {
            ResidentPacks.markLive(keys, server);
        } else {
            ResidentPacks.drop();
        }
        if (toSave != null && this.packSource != EMPTY_SOURCE) {
            try {
                PackHistory.write(packkeep$historyFile(), server, toSave);
            } catch (Exception e) {
                PackKeepClient.LOGGER.warn("[PackKeep] could not save pack history", e);
            }
        }
    }

    @Inject(
            method = {"onRecovery", "onRecoveryFailure"},
            at = @At("HEAD"))
    private void packkeep$onFailure(CallbackInfo ci) {
        packkeep$pendingKeys = null;
        packkeep$pendingSave = null;
        ResidentPacks.drop();
        if (this.pendingReload == null && packkeep$fromHistory) {
            packkeep$fromHistory = false;
            this.packSource = EMPTY_SOURCE;
            try {
                Files.deleteIfExists(packkeep$historyFile());
            } catch (Exception ignored) {
            }
            PackKeepClient.LOGGER.warn(
                    "[PackKeep] the last server's packs failed to load at startup; dropped them");
        }
    }
}
