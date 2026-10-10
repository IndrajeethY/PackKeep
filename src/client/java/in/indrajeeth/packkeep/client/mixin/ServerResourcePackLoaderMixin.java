package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepState;

import net.minecraft.client.resource.server.ServerResourcePackLoader;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerResourcePackLoader.class)
public class ServerResourcePackLoaderMixin {

    @Inject(method = "onReloadSuccess", at = @At("TAIL"), require = 0)
    private void packkeep$promote(CallbackInfo ci) {
        PackKeepState.promotePending();
    }
}
