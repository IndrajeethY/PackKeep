package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.ResidentPacks;

import net.minecraft.client.MinecraftClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {

    /**
     * Marks a real disconnect so the empty reload it triggers is recognised as "keep these packs",
     * rather than a server dropping its pack (which also reloads empty but must not be held).
     */
    @Inject(method = "onDisconnected", at = @At("HEAD"), require = 0)
    private void packkeep$markLeaving(CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (cfg.enabled && cfg.keepResidentAcrossDisconnect) {
            ResidentPacks.markLeaving();
        }
    }
}
