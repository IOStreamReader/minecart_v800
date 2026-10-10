package dev.seiriko.minecart;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PoweredRailBlock;
import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;

/** Included only with -PbrakeGameTests; never shipped in release jars. */
@EventBusSubscriber(modid = MinecartMod.MOD_ID)
public final class BrakeGameTests {
    @SubscribeEvent
    public static void register(RegisterGameTestsEvent event) {
        var id = Identifier.fromNamespaceAndPath(MinecartMod.MOD_ID, "brake_lifecycle");
        var environment = event.registerEnvironment(id);
        event.registerTest(id, new GameTestInstance(new TestData<>(environment,
                Identifier.fromNamespaceAndPath(MinecartMod.MOD_ID, "empty"), 20, 0, true)) {
            @Override public void run(GameTestHelper helper) { lifecycle(helper); }
            @Override public MapCodec<? extends GameTestInstance> codec() { return MapCodec.unit(this); }
            @Override protected MutableComponent typeDescription() { return Component.literal("Minecart brake lifecycle"); }
        });
    }

    @SuppressWarnings("unchecked")
    private static EntityType<Minecart> minecartType() {
        return (EntityType<Minecart>) net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.withDefaultNamespace("minecart"));
    }

    private static void lifecycle(GameTestHelper helper) {
        var level = helper.getLevel();
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "brake-test-rider"), false);
        var player = new net.minecraft.server.level.ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(level.getServer(), connection, player, cookie);
        player.connection.markClientLoaded();
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        var origin = helper.absolutePos(new BlockPos(2, 2, 2));
        for (int x = 0; x < 8; x++) {
            helper.setBlock(new BlockPos(x, 1, 2), Blocks.REDSTONE_BLOCK);
            helper.setBlock(new BlockPos(x, 2, 2), Blocks.POWERED_RAIL.defaultBlockState()
                .setValue(PoweredRailBlock.SHAPE, RailShape.EAST_WEST).setValue(PoweredRailBlock.POWERED, true));
        }
        var cart = new Minecart(minecartType(), level);
        cart.setPos(Vec3.atBottomCenterOf(origin));
        level.addFreshEntity(cart);
        player.setPos(cart.position());
        player.startRiding(cart);
        player.setLastClientInput(new Input(false, true, false, false, false, false, false));
        cart.setDeltaMovement(0.4, 0, 0);
        double previous = 0.4;
        for (int i = 0; i < 31; i++) {
            cart.setPos(Vec3.atBottomCenterOf(origin));
            cart.tick();
            helper.assertTrue(cart.getDeltaMovement().horizontalDistance() <= Math.max(0, previous - 0.004) + 1e-8, "Powered rails must not overpower brakes: step=" + i + ", before=" + previous + ", after=" + cart.getDeltaMovement().horizontalDistance() + ", rider=" + cart.getFirstPassenger());
            previous = cart.getDeltaMovement().horizontalDistance();
        }
        helper.assertTrue(!((BrakedMinecart)cart).minecartV800$broken(), "Brake must survive 31 ticks");
        cart.tick();
        helper.assertTrue(((BrakedMinecart)cart).minecartV800$broken(), "Brake must break on tick 32");
        var iron = level.getEntitiesOfClass(ItemEntity.class, cart.getBoundingBox().inflate(4), e -> e.getItem().is(Items.IRON_INGOT));
        helper.assertTrue(iron.size() == 1 && iron.getFirst().getItem().getCount() == 1, "Must drop exactly one iron ingot");

        var restored = new Minecart(minecartType(), level);
        restored.restoreFrom(cart);
        helper.assertTrue(((BrakedMinecart)restored).minecartV800$broken(), "Save/load must retain brake_broken");
        helper.assertTrue(((BrakedMinecart)restored).minecartV800$heat() == 160, "Save/load must retain heat");
        player.stopRiding();
        cart.setCustomName(Component.literal("Old custom name"));
        cart.destroy(level, Items.MINECART);
        var drops = level.getEntitiesOfClass(ItemEntity.class, cart.getBoundingBox().inflate(4), e -> e.getItem().is(Items.MINECART));
        helper.assertTrue(drops.size() == 1, "Must drop one minecart");
        ItemStack stack = drops.getFirst().getItem();
        helper.assertTrue(stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("brake_broken", false), "Drop must retain brake_broken");
        helper.assertTrue(stack.get(DataComponents.ITEM_NAME) != null, "Drop must have translated broken name");
        helper.assertTrue(stack.get(DataComponents.CUSTOM_NAME) == null, "Broken name must be visible even for renamed carts");
        var placed = AbstractMinecart.createMinecart(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5,
                minecartType(), EntitySpawnReason.SPAWN_ITEM_USE, stack, player);
        helper.assertTrue(placed != null && ((BrakedMinecart)placed).minecartV800$broken(), "Placed item must remain broken");
        level.addFreshEntity(placed);
        player.setPos(placed.position());
        player.startRiding(placed);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);
        player.setHealth(40);
        player.invulnerableTime = 0;
        for (int i = 0; i < 59; i++) {
            placed.setPos(Vec3.atBottomCenterOf(origin));
            placed.tick();
        }
        helper.assertTrue(!placed.isRemoved(), "Broken cart must survive 59 forced ticks");
        placed.tick();
        helper.assertTrue(placed.isRemoved() && player.getVehicle() == null, "Forced braking must remove cart and dismount player");
        helper.assertTrue(player.getHealth() == 15.0F, "Destruction must deal exactly 25 physical damage, health=" + player.getHealth());
        var armoredCart = AbstractMinecart.createMinecart(level, origin.getX() + 0.5, origin.getY(), origin.getZ() + 0.5,
                minecartType(), EntitySpawnReason.SPAWN_ITEM_USE, stack, player);
        level.addFreshEntity(armoredCart);
        player.startRiding(armoredCart);
        player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(40);
        player.setHealth(40);
        player.invulnerableTime = 0;
        player.setItemSlot(net.minecraft.world.entity.EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.doTick(); // Apply vanilla equipment attribute updates before taking damage.
        for (int i = 0; i < 60; i++) {
            armoredCart.setPos(Vec3.atBottomCenterOf(origin));
            armoredCart.tick();
        }
        helper.assertTrue(armoredCart.isRemoved(), "Armored rider must not prevent cart destruction");
        helper.assertTrue(player.getHealth() > 15 && player.getHealth() < 40, "Physical damage must respect armor");
        player.setLastClientInput(Input.EMPTY);
        helper.succeed();
    }
}
