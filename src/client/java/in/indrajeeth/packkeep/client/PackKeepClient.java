package in.indrajeeth.packkeep.client;

import com.mojang.blaze3d.platform.InputConstants;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackKeepClient implements ClientModInitializer {
    public static final String MOD_ID = "packkeep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        PackKeepConfig.load();

        KeyMapping.Category category =
                KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        KeyMapping openConfig =
                KeyMappingHelper.registerKeyMapping(
                        new KeyMapping(
                                "key.packkeep.open_config",
                                InputConstants.Type.KEYSYM,
                                GLFW.GLFW_KEY_UNKNOWN,
                                category));

        ClientTickEvents.END_CLIENT_TICK.register(
                client -> {
                    while (openConfig.consumeClick()) {
                        client.setScreenAndShow(PackKeepConfigScreen.create(null));
                    }
                });

        LOGGER.info(
                "[PackKeep] client init — resident server-pack cache armed (enabled={})",
                PackKeepConfig.get().enabled);
    }
}
