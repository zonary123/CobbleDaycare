package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Date;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class WalkBreedingMixin {

  @Shadow public ServerPlayerEntity player;
  @Unique private Entity entity;
  @Unique private double oldX;
  @Unique private double oldZ;
  @Unique private boolean tp;
  @Unique private long oldTime;

  @Inject(method = "onTeleportConfirm", at = @At("HEAD"))
  public void breeding$handlePendingTeleport(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
    tp = true;
  }

  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  public void breeding$onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
    long newTime = System.currentTimeMillis();

    if (oldTime < newTime) {
      boolean isInPose = CobbleDaycare.config.isAllowElytra() || !player.isInPose(EntityPose.FALL_FLYING);
      boolean isInvulnerable = !player.isInvulnerable();
      boolean isFly = player.getAbilities().flying;
      boolean permittedVehicles = cobbleUtils$permittedVehicles(player);
      boolean result =
        isInPose && !isFly && isInvulnerable && permittedVehicles && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING));
      if (result) {
        var party = Cobblemon.INSTANCE.getStorage().getParty(player);

        entity = player.getVehicle() != null ? player.getVehicle() : player;
        if (entity == null) return;

        double deltaMovement = cobbleUtils$getDeltaMovement(packet, party, entity);

        oldX = entity.getX();
        oldZ = entity.getZ();
        if (deltaMovement <= 0 || tp) {
          tp = false;
          return;
        }
        UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
        for (Pokemon pokemon : party) {
          if (pokemon != null && pokemon.showdownId().equals("egg")) {
            cobbleUtils$updateEggSteps(pokemon, deltaMovement, userInformation);
          }
        }
        long timeMultiplierSteps = userInformation.getTimeMultiplierSteps();
        if (timeMultiplierSteps > 0) {
          timeMultiplierSteps -= CobbleDaycare.config.getTicksToWalking();
          userInformation.setTimeMultiplierSteps(timeMultiplierSteps);
          if (timeMultiplierSteps <= 0) {
            userInformation.setMultiplierSteps(CobbleDaycare.config.getMultiplierSteps());
            userInformation.setTimeMultiplierSteps(0);
            DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
          }
        }
        cobbleDaycare$sendMessageMultiplierSteps(player, userInformation);
      } else {
        tp = true;
      }

      oldTime = System.currentTimeMillis() + (CobbleDaycare.config.getTicksToWalking() * 50);
    }
  }

  @Unique
  private void cobbleDaycare$sendMessageMultiplierSteps(ServerPlayerEntity player, UserInformation userInformation) {
    boolean activeMultiplier = CobbleDaycare.config.isGlobalMultiplierSteps();
    boolean haveMultiplier = userInformation.getActualMultiplier() > CobbleDaycare.config.getMultiplierSteps();
    if (activeMultiplier || haveMultiplier) {
      long ticks = userInformation.getTimeMultiplierSteps();
      long cooldown = ticks * 50; // Convert ticks to milliseconds
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageActiveStepsMultiplier()
          .replace("%multiplier%", String.format("%.2f", userInformation.getActualMultiplier()))
          .replace("%cooldown%", PlayerUtils.getCooldown(new Date(System.currentTimeMillis() + cooldown))),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.ACTIONBAR
      );
    }
  }

  @Unique private boolean cobbleUtils$permittedVehicles(ServerPlayerEntity player) {
    String id = player.getVehicle() == null ? "" : player.getVehicle().getSavedEntityId();
    if (id == null) id = "";
    return CobbleDaycare.config.getPermittedVehicles().contains(id) || id.isEmpty();
  }

  @Unique
  private double cobbleUtils$getDeltaMovement(PlayerMoveC2SPacket packet, PlayerPartyStore party, Entity entity) {
    double newX = entity instanceof ServerPlayerEntity ? packet.getX(entity.getX()) : entity.getX();
    double newZ = entity instanceof ServerPlayerEntity ? packet.getZ(entity.getZ()) : entity.getZ();
    //double newX = MathHelper.clamp(valueX, -3.0E7D, 3.0E7D);
    //double newZ = MathHelper.clamp(valueZ, -3.0E7D, 3.0E7D);

    if (Double.isNaN(newX) || Double.isNaN(newZ)) return 0;

    double deltaX = newX - oldX;
    double deltaZ = newZ - oldZ;

    if (Double.isNaN(deltaX) || Double.isNaN(deltaZ)) return 0;


    double deltaMovement = Math.hypot(deltaX, deltaZ);

    if (!(entity instanceof ServerPlayerEntity)) {
      deltaMovement /= CobbleDaycare.config.getReduceEggStepsVehicle();
    }
    return cobbleUtils$hasStepAcceleratingPokemon(party) ? deltaMovement * CobbleDaycare.config.getMultiplierAbilityAcceleration() : deltaMovement / 2;
  }

  @Unique
  private boolean cobbleUtils$hasStepAcceleratingPokemon(PlayerPartyStore party) {
    for (Pokemon pokemon : party) {
      if (CobbleDaycare.config.getAbilityAcceleration().contains(pokemon.getAbility().getName())) return true;
    }
    return false;
  }

  @Unique
  private void cobbleUtils$updateEggSteps(Pokemon egg, double deltaMovement, UserInformation userInformation) {
    egg.setCurrentHealth(0);
    EggData eggData = EggData.from(egg);
    eggData.steps(player, egg, deltaMovement, userInformation);
  }
}