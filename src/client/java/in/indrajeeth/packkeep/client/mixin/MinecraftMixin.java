package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.ResidentPacks;

import net.minecraft.client.Minecraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {

    /**
     * Marks a real disconnect so the empty reload its pack cleanup triggers is recognised as "keep
     * these packs", rather than a server dropping its pack (which also reloads empty but must not
     * be held). This fires only from the disconnect path, not from server-initiated pack removal.
     */
    @Inject(method = "clearDownloadedResourcePacks", at = @At("HEAD"), require = 0)
    private void packkeep$markLeaving(CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (cfg.enabled && cfg.keepResidentAcrossDisconnect) {
            ResidentPacks.markLeaving();
        }
    }
}
