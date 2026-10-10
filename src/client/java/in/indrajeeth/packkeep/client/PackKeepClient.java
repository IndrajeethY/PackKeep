package in.indrajeeth.packkeep.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
//? if >=1.21.9
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PackKeepClient implements ClientModInitializer {
    public static final String MOD_ID = "packkeep";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        PackKeepConfig.load();

        //? if >=1.21.9 {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        //?} else {
        /*String category = "key.category.packkeep.main";
        *///?}
        KeyBinding openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.packkeep.open_config",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                category));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openConfig.wasPressed()) {
                client.setScreen(PackKeepConfigScreen.create(client.currentScreen));
            }
        });

        LOGGER.info("[PackKeep] client init — resident server-pack cache armed (enabled={})",
                PackKeepConfig.get().enabled);
    }
}
