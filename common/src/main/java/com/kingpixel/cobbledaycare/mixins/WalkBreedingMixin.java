package com.kingpixel.cobbledaycare.mixins;

import com.kingpixel.cobbledaycare.tasks.TaskDayCare;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = ServerPlayNetworkHandler.class, priority = 9999)
public abstract class WalkBreedingMixin {
  @Unique private static final int MAX_TELEPORT = 2;
  @Unique private ServerPlayerEntity cobbleDaycare$player;

  @Inject(method = "onTeleportConfirm", at = @At("HEAD"))
  public void onTeleportConfirm(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    TaskDayCare.cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }

  @Inject(method = "requestTeleport(DDDFF)V", at = @At("HEAD"))
  public void requestTeleport(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    TaskDayCare.cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }

  @Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
  public void requestTeleportWithSet(double x, double y, double z, float yaw, float pitch, java.util.Set<?> set, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    TaskDayCare.cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }
}
