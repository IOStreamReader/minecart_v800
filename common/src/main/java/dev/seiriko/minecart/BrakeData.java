package dev.seiriko.minecart;

import java.util.function.Supplier;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Sync-only HUD snapshot; the entity mixin owns the explicit save-data keys. */
public final class BrakeData {
    public static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, MinecartMod.MOD_ID);
    public static final Supplier<AttachmentType<Integer>> STATUS = TYPES.register("brake_status",
        () -> AttachmentType.builder(() -> 0).sync(ByteBufCodecs.VAR_INT).build());
}
