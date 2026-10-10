package dev.seiriko.minecart.client;

import dev.seiriko.minecart.BrakeState;
import dev.seiriko.minecart.BrakedMinecart;
import dev.seiriko.minecart.MinecartMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = MinecartMod.MOD_ID, value = Dist.CLIENT)
public final class BrakeHud {
    private static final Identifier BACKGROUND = Identifier.withDefaultNamespace("hud/jump_bar_background");
    private static final Identifier PROGRESS = Identifier.withDefaultNamespace("hud/jump_bar_progress");

    @SubscribeEvent
    public static void render(RenderGuiLayerEvent.Pre event) {
        var minecraft = Minecraft.getInstance();
        if (minecraft.player == null || HudVisibility.hidden(minecraft) || minecraft.player.isSpectator()
                || !(minecraft.player.getVehicle() instanceof Minecart cart)) return;
        var brake = (BrakedMinecart)cart;
        if (brake.minecartV800$heat() == 0 && !minecraft.options.keyDown.isDown()) return;
        if (event.getName().equals(VanillaGuiLayers.EXPERIENCE_LEVEL)
                || event.getName().equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR)) {
            event.setCanceled(true);
        } else if (event.getName().equals(VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND)) {
            event.setCanceled(true);
            var graphics = event.getGuiGraphics();
            int x = (graphics.guiWidth() - 182) / 2;
            int y = graphics.guiHeight() - 29;
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, BACKGROUND, x, y, 182, 5);
            int width = Math.min(182, brake.minecartV800$heat() * 182 / BrakeState.MAX_HEAT);
            if (width > 0) graphics.blitSprite(RenderPipelines.GUI_TEXTURED, PROGRESS, 182, 5, 0, 0, x, y, width, 5);
        }
    }
}
