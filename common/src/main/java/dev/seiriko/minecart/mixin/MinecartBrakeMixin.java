package dev.seiriko.minecart.mixin;

import dev.seiriko.minecart.BrakeState;
import dev.seiriko.minecart.BrakeData;
import dev.seiriko.minecart.BrakedMinecart;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.minecart.AbstractMinecart;
import net.minecraft.world.entity.vehicle.minecart.Minecart;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AbstractMinecart.class)
public abstract class MinecartBrakeMixin implements BrakedMinecart {
    @Unique private static final ResourceKey<DamageType> BRAKE_FAILURE = ResourceKey.create(Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath("minecart_v800", "brake_failure"));
    @Unique private final BrakeState minecartV800$state = new BrakeState();
    @Unique private double minecartV800$speedLimit = -1;
    @Unique private AbstractMinecart minecartV800$self() { return (AbstractMinecart)(Object)this; }

    @Override public int minecartV800$heat() { return minecartV800$self().getData(BrakeData.STATUS) & 255; }
    @Override public boolean minecartV800$broken() { return (minecartV800$self().getData(BrakeData.STATUS) & 256) != 0; }
    @Override public void minecartV800$restoreBroken() {
        minecartV800$state.restore(true, 0);
        minecartV800$sync();
    }
    @Unique private void minecartV800$sync() {
        int status = minecartV800$state.heat() | (minecartV800$state.broken() ? 256 : 0);
        if (minecartV800$self().getData(BrakeData.STATUS) != status) minecartV800$self().setData(BrakeData.STATUS, status);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void tickBrake(CallbackInfo ci) {
        var cart = minecartV800$self();
        minecartV800$speedLimit = -1;
        if (!(cart instanceof Minecart) || !(cart.level() instanceof ServerLevel level)) return;
        boolean pressed = cart.getFirstPassenger() instanceof ServerPlayer player && player.getLastClientInput().backward();
        boolean wasBroken = minecartV800$state.broken();
        if (pressed && !wasBroken) {
            minecartV800$speedLimit = BrakeState.speedAfterBraking(cart.getDeltaMovement().horizontalDistance(), cart.getBehavior().getMaxSpeed(level));
            cart.setDeltaMovement(minecartV800$clamp(cart.getDeltaMovement()));
        }
        var result = minecartV800$state.tick(pressed);
        minecartV800$sync();
        if (pressed && minecartV800$state.broken() && result != BrakeState.Result.DESTROY) {
            level.sendParticles(ParticleTypes.LARGE_SMOKE, cart.getX(), cart.getY() + 0.3, cart.getZ(), 2, 0.3, 0.2, 0.3, 0.02);
        }
        if (result != BrakeState.Result.NONE) {
            if (result == BrakeState.Result.BREAK) {
                level.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.ITEM_BREAK, SoundSource.NEUTRAL, 1.0F, 1.0F);
                cart.spawnAtLocation(level, new ItemStack(Items.IRON_INGOT));
            } else {
                var passenger = cart.getFirstPassenger();
                level.playSound(null, cart.getX(), cart.getY(), cart.getZ(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 1.0F, 1.0F);
                level.sendParticles(ParticleTypes.EXPLOSION, cart.getX(), cart.getY() + 0.3, cart.getZ(), 1, 0, 0, 0, 0);
                cart.ejectPassengers();
                cart.discard();
                if (passenger instanceof ServerPlayer player) {
                    player.hurtServer(level, new DamageSource(level.registryAccess().getOrThrow(BRAKE_FAILURE)), 25.0F);
                }
                ci.cancel();
            }
        }
    }

    @Unique private Vec3 minecartV800$clamp(Vec3 movement) {
        double speed = movement.horizontalDistance();
        if (minecartV800$speedLimit < 0 || speed <= minecartV800$speedLimit || speed == 0) return movement;
        double scale = minecartV800$speedLimit / speed;
        return new Vec3(movement.x * scale, movement.y, movement.z * scale);
    }

    @ModifyVariable(method = "move", at = @At("HEAD"), argsOnly = true)
    private Vec3 limitBrakeMovement(Vec3 movement) { return minecartV800$clamp(movement); }

    @Inject(method = "getMaxSpeed", at = @At("RETURN"), cancellable = true)
    private void limitRailSpeed(ServerLevel level, CallbackInfoReturnable<Double> cir) {
        if (minecartV800$speedLimit >= 0) cir.setReturnValue(Math.min(cir.getReturnValue(), minecartV800$speedLimit));
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void finishBrakeTick(CallbackInfo ci) {
        var cart = minecartV800$self();
        cart.setDeltaMovement(minecartV800$clamp(cart.getDeltaMovement()));
    }

    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void saveBrake(ValueOutput output, CallbackInfo ci) {
        output.putBoolean("brake_broken", minecartV800$state.broken());
        output.putInt("brake_heat", minecartV800$state.heat());
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void loadBrake(ValueInput input, CallbackInfo ci) {
        minecartV800$state.restore(input.getBooleanOr("brake_broken", false), input.getIntOr("brake_heat", 0));
        minecartV800$sync();
    }

    @Inject(method = "createMinecart", at = @At("RETURN"))
    private static <T extends AbstractMinecart> void restoreItemBrake(Level level, double x, double y, double z,
            EntityType<T> type, EntitySpawnReason reason, ItemStack stack, Player player, CallbackInfoReturnable<T> cir) {
        if (cir.getReturnValue() instanceof Minecart cart && stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getBooleanOr("brake_broken", false)) {
            ((BrakedMinecart)cart).minecartV800$restoreBroken();
        }
    }
}
