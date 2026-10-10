package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.PackKeepState;

import net.minecraft.client.MinecraftClient;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class MinecraftClientMixin {
    @Inject(method = "onDisconnected", at = @At("HEAD"), require = 0)
    private void packkeep$beginDisconnect(CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (cfg.enabled && cfg.keepResidentAcrossDisconnect) {
            PackKeepState.beginDisconnect();
        }
    }
}
