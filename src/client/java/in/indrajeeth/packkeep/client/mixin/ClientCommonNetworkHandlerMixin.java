package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepClient;
import in.indrajeeth.packkeep.client.PackKeepConfig;
import in.indrajeeth.packkeep.client.PackKeepState;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(ClientCommonNetworkHandler.class)
public abstract class ClientCommonNetworkHandlerMixin {
    @Shadow @Final protected MinecraftClient client;

    @Inject(method = "onResourcePackSend", at = @At("HEAD"), cancellable = true)
    private void packkeep$skipIfResident(ResourcePackSendS2CPacket packet, CallbackInfo ci) {
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled || !cfg.skipReloadOnHashMatch) {
            return;
        }
        String hash = packet.hash();
        UUID id = packet.id();
        if (PackKeepState.isResident(hash)) {
            ClientCommonNetworkHandler self = (ClientCommonNetworkHandler) (Object) this;
            self.sendPacket(
                    new ResourcePackStatusC2SPacket(
                            id, ResourcePackStatusC2SPacket.Status.ACCEPTED));
            self.sendPacket(
                    new ResourcePackStatusC2SPacket(
                            id, ResourcePackStatusC2SPacket.Status.DOWNLOADED));
            self.sendPacket(
                    new ResourcePackStatusC2SPacket(
                            id, ResourcePackStatusC2SPacket.Status.SUCCESSFULLY_LOADED));
            PackKeepState.setRetained(false);
            if (cfg.verboseLog) {
                PackKeepClient.LOGGER.info(
                        "[PackKeep] resident pack {} hash={} — acked, no reload", id, hash);
            }
            ci.cancel();
        } else {
            if (PackKeepState.isRetained()) {
                this.client.getServerResourcePackProvider().removeAll();
                PackKeepState.clearResident();
                PackKeepState.setRetained(false);
            }
            PackKeepState.markPending(hash);
        }
    }

    @Inject(method = "onDisconnected", at = @At("HEAD"), require = 0)
    private void packkeep$clearPendingOnDisconnect(CallbackInfo ci) {
        PackKeepState.clearPending();
    }
}
