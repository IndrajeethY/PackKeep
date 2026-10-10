package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepClient;
import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.PackKeepState;

import net.minecraft.client.resource.server.ServerResourcePackManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerResourcePackManager.class)
public class ServerResourcePackManagerMixin {
    @Inject(method = "removeAll", at = @At("HEAD"), cancellable = true, require = 0)
    private void packkeep$retainOnDisconnect(CallbackInfo ci) {
        if (PackKeepState.consumeDisconnectRetain()) {
            PackKeepState.setRetained(true);
            if (PackKeepConfig.get().verboseLog) {
                PackKeepClient.LOGGER.info("[PackKeep] retained server packs across disconnect");
            }
            ci.cancel();
        }
    }
}
