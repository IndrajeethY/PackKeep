package in.indrajeeth.packkeep.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class PackKeepConfigScreen extends Screen {
    private static final Component TITLE = Component.literal("PackKeep");
    private final Screen parent;

    private PackKeepConfigScreen(Screen parent) {
        super(Minecraft.getInstance(), Minecraft.getInstance().font, TITLE);
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new PackKeepConfigScreen(parent);
    }

    @Override
    protected void init() {
        PackKeepConfig cfg = PackKeepConfig.get();
        int w = 310, h = 20, x = this.width / 2 - w / 2, y = 50;

        addRenderableWidget(
                CycleButton.onOffBuilder(cfg.enabled)
                        .create(
                                x,
                                y,
                                w,
                                h,
                                Component.literal("Enabled"),
                                (b, v) -> cfg.enabled = v));
        y += 24;
        addRenderableWidget(
                CycleButton.onOffBuilder(cfg.keepResidentAcrossDisconnect)
                        .create(
                                x,
                                y,
                                w,
                                h,
                                Component.literal("Keep resident across disconnect"),
                                (b, v) -> cfg.keepResidentAcrossDisconnect = v));
        y += 24;
        addRenderableWidget(
                CycleButton.onOffBuilder(cfg.skipReloadOnHashMatch)
                        .create(
                                x,
                                y,
                                w,
                                h,
                                Component.literal("Skip reload on hash match"),
                                (b, v) -> cfg.skipReloadOnHashMatch = v));
        y += 24;
        addRenderableWidget(
                CycleButton.onOffBuilder(cfg.autoAcceptRequired)
                        .create(
                                x,
                                y,
                                w,
                                h,
                                Component.literal("Auto-accept required packs"),
                                (b, v) -> cfg.autoAcceptRequired = v));

        addRenderableWidget(
                Button.builder(Component.literal("Done"), b -> this.onClose())
                        .bounds(this.width / 2 - 100, this.height - 28, 200, 20)
                        .build());
    }

    @Override
    public void extractRenderState(
            GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.centeredText(this.font, TITLE, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void onClose() {
        PackKeepConfig.save();
        Minecraft.getInstance().setScreenAndShow(parent);
    }
}
