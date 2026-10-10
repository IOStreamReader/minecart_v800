package dev.seiriko.minecart;

import com.mojang.logging.LogUtils;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/** Entry point. Keep version-specific Minecraft integration in this project. */
@Mod(MinecartMod.MOD_ID)
public final class MinecartMod {
    public static final String MOD_ID = "minecart_v800";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MinecartMod(net.neoforged.bus.api.IEventBus modBus) {
        BrakeData.TYPES.register(modBus);
        LOGGER.info("Minecart V800 initialized");
    }
}
