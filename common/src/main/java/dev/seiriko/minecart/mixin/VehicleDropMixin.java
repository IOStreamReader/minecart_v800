package dev.seiriko.minecart.mixin;

import dev.seiriko.minecart.BrakedMinecart;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.vehicle.VehicleEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(VehicleEntity.class)
public abstract class VehicleDropMixin {
    @ModifyArg(method = "destroy(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/Item;)V",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/vehicle/VehicleEntity;spawnAtLocation(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/entity/item/ItemEntity;"), index = 1)
    private ItemStack preserveBrokenBrake(ItemStack stack) {
        if ((Object)this instanceof BrakedMinecart cart && cart.minecartV800$broken() && stack.is(Items.MINECART)) {
            CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean("brake_broken", true));
            stack.remove(DataComponents.CUSTOM_NAME);
            stack.set(DataComponents.ITEM_NAME, Component.translatable("item.minecart_v800.brake_broken_minecart"));
        }
        return stack;
    }
}
