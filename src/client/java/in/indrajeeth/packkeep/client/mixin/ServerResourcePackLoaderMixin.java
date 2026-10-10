package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackHistory;
import in.indrajeeth.packkeep.client.PackKeepClient;
import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.ResidentPacks;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.resource.server.ReloadScheduler;
import net.minecraft.client.resource.server.ServerResourcePackLoader;
import net.minecraft.resource.ResourcePackProfile;
import net.minecraft.resource.ResourcePackProvider;
import net.minecraft.resource.ResourcePackSource;

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

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {
    @Shadow @Final private static ResourcePackProvider NOOP_PROVIDER;
    @Shadow private ResourcePackProvider packProvider;
    @Shadow private ResourcePackSource packSource;
    @Shadow private ReloadScheduler.ReloadContext reloadContext;

    @Shadow
    private List<ResourcePackProfile> toProfiles(List<ReloadScheduler.PackInfo> packs) {
        throw new AssertionError();
    }

    @Shadow
    private static ResourcePackProvider getPackProvider(List<ResourcePackProfile> serverPacks) {
        throw new AssertionError();
    }

    @Unique private boolean packkeep$fromHistory = false;
    @Unique private List<String> packkeep$pendingKeys = null;
    @Unique private List<ReloadScheduler.PackInfo> packkeep$pendingSave = null;

    @Unique
    private static Path packkeep$historyFile() {
        return FabricLoader.getInstance().getConfigDir().resolve("packkeep-history.json");
    }

    @Unique
    private static String packkeep$currentServer() {
        ServerInfo entry = MinecraftClient.getInstance().getCurrentServerEntry();
        return entry == null ? null : entry.address;
    }

    @Inject(method = "<init>", at = @At("RETURN"), require = 0)
    private void packkeep$warmFromHistory(
            MinecraftClient client, Path dir, RunArgs.Network network, CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled || !cfg.preWarmPinnedAtLaunch) {
            return;
        }
        try {
            PackHistory.Snapshot saved = PackHistory.read(packkeep$historyFile());
            if (saved == null || saved.packs.isEmpty()) {
                return;
            }
            this.packSource = ResourcePackSource.SERVER;
            List<ResourcePackProfile> profiles = this.toProfiles(saved.packs);
            if (profiles == null) {
                return;
            }
            this.packProvider = getPackProvider(profiles);
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

    @Inject(method = "init", at = @At("HEAD"))
    private void packkeep$onConnect(CallbackInfo ci) {
        if (!PackKeepConfig.get().enabled) {
            return;
        }
        if (ResidentPacks.foreignTo(packkeep$currentServer())) {
            ResidentPacks.drop();
            this.packProvider = NOOP_PROVIDER;
            packkeep$fromHistory = false;
        }
    }

    /**
     * Skips the reload when it would change nothing: the requested packs are already applied, or it
     * is the empty reload a real disconnect fires while packs are held. Success is reported through
     * the vanilla context (no forged status packets); cancelling leaves the live provider
     * untouched, which is what keeps the pack resident.
     */
    @Inject(method = "reload", at = @At("HEAD"), cancellable = true)
    private void packkeep$maybeSkip(ReloadScheduler.ReloadContext context, CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled) {
            return;
        }
        List<String> keys;
        try {
            keys = ResidentPacks.keysOf(context.getPacks());
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
            context.onSuccess();
            ci.cancel();
            return;
        }
        packkeep$pendingKeys = keys;
        packkeep$fromHistory = false;
        packkeep$pendingSave =
                (cfg.preWarmPinnedAtLaunch && !keys.isEmpty() && ResidentPacks.identifiable(keys))
                        ? List.copyOf(context.getPacks())
                        : null;
    }

    @Inject(method = "onReloadSuccess", at = @At("HEAD"))
    private void packkeep$onSuccess(CallbackInfo ci) {
        List<String> keys = packkeep$pendingKeys;
        List<ReloadScheduler.PackInfo> toSave = packkeep$pendingSave;
        packkeep$pendingKeys = null;
        packkeep$pendingSave = null;
        if (this.reloadContext == null) {
            return;
        }
        String server = packkeep$currentServer();
        if (keys != null && this.packProvider != NOOP_PROVIDER) {
            ResidentPacks.markLive(keys, server);
        } else {
            ResidentPacks.drop();
        }
        if (toSave != null && this.packProvider != NOOP_PROVIDER) {
            try {
                PackHistory.write(packkeep$historyFile(), server, toSave);
            } catch (Exception e) {
                PackKeepClient.LOGGER.warn("[PackKeep] could not save pack history", e);
            }
        }
    }

    @Inject(
            method = {"onReloadFailure", "onForcedReloadFailure"},
            at = @At("HEAD"))
    private void packkeep$onFailure(CallbackInfo ci) {
        packkeep$pendingKeys = null;
        packkeep$pendingSave = null;
        ResidentPacks.drop();
        if (this.reloadContext == null && packkeep$fromHistory) {
            packkeep$fromHistory = false;
            this.packProvider = NOOP_PROVIDER;
            try {
                Files.deleteIfExists(packkeep$historyFile());
            } catch (Exception ignored) {
            }
            PackKeepClient.LOGGER.warn(
                    "[PackKeep] the last server's packs failed to load at startup; dropped them");
        }
    }
}
