package in.indrajeeth.packkeep.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.CyclingButtonWidget;
import net.minecraft.text.Text;

public final class PackKeepConfigScreen extends Screen {
    private static final Text TITLE = Text.literal("PackKeep");
    private final Screen parent;

    private PackKeepConfigScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    public static Screen create(Screen parent) {
        return new PackKeepConfigScreen(parent);
    }

    @Override
    protected void init() {
        PackKeepConfig cfg = PackKeepConfig.get();
        int w = 310, h = 20, x = this.width / 2 - w / 2, y = 50;

        addDrawableChild(
                CyclingButtonWidget.onOffBuilder(cfg.enabled)
                        .build(x, y, w, h, Text.literal("Enabled"), (b, v) -> cfg.enabled = v));
        y += 24;
        addDrawableChild(
                CyclingButtonWidget.onOffBuilder(cfg.keepResidentAcrossDisconnect)
                        .build(
                                x,
                                y,
                                w,
                                h,
                                Text.literal("Keep resident across disconnect"),
                                (b, v) -> cfg.keepResidentAcrossDisconnect = v));
        y += 24;
        addDrawableChild(
                CyclingButtonWidget.onOffBuilder(cfg.skipReloadOnHashMatch)
                        .build(
                                x,
                                y,
                                w,
                                h,
                                Text.literal("Skip reload on hash match"),
                                (b, v) -> cfg.skipReloadOnHashMatch = v));

        addDrawableChild(
                ButtonWidget.builder(Text.literal("Done"), b -> this.close())
                        .dimensions(this.width / 2 - 100, this.height - 28, 200, 20)
                        .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer, TITLE, this.width / 2, 20, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        PackKeepConfig.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
