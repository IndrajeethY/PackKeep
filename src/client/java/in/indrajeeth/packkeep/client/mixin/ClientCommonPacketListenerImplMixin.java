package in.indrajeeth.packkeep.client.mixin;

import in.indrajeeth.packkeep.client.PackKeepConfig;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.protocol.common.ClientboundResourcePackPushPacket;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.URL;

@Mixin(ClientCommonPacketListenerImpl.class)
public abstract class ClientCommonPacketListenerImplMixin {
    @Shadow @Final protected Minecraft minecraft;

    @Invoker("parseResourcePackUrl")
    private static URL packkeep$parseUrl(String url) {
        throw new AssertionError();
    }

    @Inject(method = "handleResourcePackPush", at = @At("HEAD"), cancellable = true, require = 0)
    private void packkeep$autoAccept(ClientboundResourcePackPushPacket packet, CallbackInfo ci) {
        if (!this.minecraft.isSameThread()) {
            return;
        }
        PackKeepConfig cfg = PackKeepConfig.get();
        if (!cfg.enabled || !cfg.autoAcceptRequired || !packet.required()) {
            return;
        }
        URL url = packkeep$parseUrl(packet.url());
        if (url == null) {
            return;
        }
        this.minecraft.getDownloadedPackSource().pushPack(packet.id(), url, packet.hash());
        ci.cancel();
    }
}
