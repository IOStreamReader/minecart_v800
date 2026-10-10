package dev.seiriko.minecart.client;

import net.minecraft.client.Minecraft;

final class HudVisibility {
    static boolean hidden(Minecraft minecraft) { return minecraft.options.hideGui; }
}
