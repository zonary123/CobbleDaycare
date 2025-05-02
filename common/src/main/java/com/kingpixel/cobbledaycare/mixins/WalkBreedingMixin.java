package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
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

@Mixin(ServerPlayNetworkHandler.class)
public abstract class WalkBreedingMixin {

  @Unique private static final long TICKS_TO_MILLISECONDS = 50;
  @Unique private static final int MAX_TELEPORT = 3;
  @Shadow public ServerPlayerEntity player;
  @Unique private double cobbleDaycare$previousX = 0;
  @Unique private double cobbleDaycare$previousZ = 0;
  @Unique private int cobbleDaycare$teleport = 0;
  @Unique private long cobbleDaycare$lastUpdateTime = 0;

  @Inject(method = "onTeleportConfirm", at = @At("HEAD"))
  public void onTeleportConfirm(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
    cobbleDaycare$logDebug("TeleportConfirm: " + packet.getTeleportId());
    cobbleDaycare$teleport = MAX_TELEPORT;
  }

  @Inject(method = "requestTeleport(DDDFF)V", at = @At("HEAD"))
  public void requestTeleport(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
    cobbleDaycare$logDebug("requestTeleport: " + x + ", " + y + ", " + z);
    cobbleDaycare$teleport = MAX_TELEPORT;
  }

  @Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
  public void requestTeleportWithSet(double x, double y, double z, float yaw, float pitch, java.util.Set<?> set, CallbackInfo ci) {
    cobbleDaycare$logDebug("requestTeleport with Set: " + x + ", " + y + ", " + z);
    cobbleDaycare$teleport = MAX_TELEPORT;
  }

  @Inject(method = "onPlayerMove", at = @At("HEAD"))
  public void onPlayerMove(PlayerMoveC2SPacket packet, CallbackInfo ci) {
    long currentTime = System.currentTimeMillis();

    if (currentTime > cobbleDaycare$lastUpdateTime) {
      try {
        if (cobbleDaycare$isPlayerEligibleForStepUpdate()) {
          var party = Cobblemon.INSTANCE.getStorage().getParty(player);
          Entity entity = cobbleDaycare$getEffectiveEntity();

          if (entity == null) return;

          if (cobbleDaycare$previousX == 0 && cobbleDaycare$previousZ == 0) {
            cobbleDaycare$teleport = MAX_TELEPORT;
            cobbleDaycare$previousX = entity.getX();
            cobbleDaycare$previousZ = entity.getZ();
          }
          double deltaMovement = cobbleDaycare$calculateDeltaMovement(packet, party, entity);
          if (deltaMovement <= 0 || cobbleDaycare$teleport > 0) {
            if (cobbleDaycare$teleport > 0) cobbleDaycare$teleport--;
            cobbleDaycare$previousX = entity.getX();
            cobbleDaycare$previousZ = entity.getZ();
            cobbleDaycare$lastUpdateTime = currentTime + (CobbleDaycare.config.getTicksToWalking() * TICKS_TO_MILLISECONDS);
            return;
          }

          cobbleDaycare$updateEggSteps(party, deltaMovement);
          cobbleDaycare$updateUserInformation();

          cobbleDaycare$lastUpdateTime = currentTime + (CobbleDaycare.config.getTicksToWalking() * TICKS_TO_MILLISECONDS);
        }
      } catch (Exception e) {
        e.printStackTrace();
        cobbleDaycare$lastUpdateTime = currentTime + (CobbleDaycare.config.getTicksToWalking() * TICKS_TO_MILLISECONDS);
      }
    }

  }

  @Unique private boolean cobbleDaycare$isPlayerEligibleForStepUpdate() {
    return (CobbleDaycare.config.isAllowElytra() || !player.isInPose(EntityPose.FALL_FLYING))
      && !player.getAbilities().flying
      && !player.isInvulnerable()
      && cobbleDaycare$isVehiclePermitted()
      && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING));
  }

  @Unique private Entity cobbleDaycare$getEffectiveEntity() {
    return player.getVehicle() != null ? player.getVehicle() : player;
  }

  @Unique
  private double cobbleDaycare$calculateDeltaMovement(PlayerMoveC2SPacket packet, PlayerPartyStore party, Entity entity) {
    double newX = entity instanceof ServerPlayerEntity ? packet.getX(entity.getX()) : entity.getX();
    double newZ = entity instanceof ServerPlayerEntity ? packet.getZ(entity.getZ()) : entity.getZ();

    if (Double.isNaN(newX) || Double.isNaN(newZ)) return 0;

    double deltaX = newX - cobbleDaycare$previousX;
    double deltaZ = newZ - cobbleDaycare$previousZ;

    cobbleDaycare$previousX = newX;
    cobbleDaycare$previousZ = newZ;

    if (Double.isNaN(deltaX) || Double.isNaN(deltaZ)) return 0;

    double deltaMovement = Math.hypot(deltaX, deltaZ);

    if (!(entity instanceof ServerPlayerEntity)) {
      deltaMovement /= CobbleDaycare.config.getReduceEggStepsVehicle();
    }

    return cobbleDaycare$hasStepAcceleratingPokemon(party)
      ? deltaMovement * CobbleDaycare.config.getMultiplierAbilityAcceleration()
      : deltaMovement / 2;
  }

  @Unique private boolean cobbleDaycare$hasStepAcceleratingPokemon(PlayerPartyStore party) {
    for (Pokemon pokemon : party) {
      if (pokemon != null) {
        String abilityName = pokemon.getAbility().getName();
        if (CobbleDaycare.config.getAbilityAcceleration().contains(abilityName)) return true;
      }
    }
    return false;
  }

  @Unique private void cobbleDaycare$updateEggSteps(PlayerPartyStore party, double deltaMovement) {
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);

    for (Pokemon pokemon : party) {
      if (pokemon != null && "egg".equals(pokemon.showdownId())) {
        EggData eggData = EggData.from(pokemon);
        if (eggData != null) {
          eggData.steps(player, pokemon, deltaMovement, userInformation);
        }
      }
    }
  }

  @Unique private void cobbleDaycare$updateUserInformation() {
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
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

    if (userInformation.isActionBar()) {
      cobbleDaycare$sendMessageMultiplierSteps(userInformation);
    }
  }

  @Unique private void cobbleDaycare$sendMessageMultiplierSteps(UserInformation userInformation) {
    boolean activeMultiplier = CobbleDaycare.config.isGlobalMultiplierSteps();
    float actualSteps = userInformation.getActualMultiplier(player);
    boolean hasMultiplier = actualSteps > CobbleDaycare.config.getMultiplierSteps();

    if (activeMultiplier || hasMultiplier) {
      long cooldown = userInformation.getTimeMultiplierSteps() * TICKS_TO_MILLISECONDS;
      String cooldownMessage = PlayerUtils.getCooldown(System.currentTimeMillis() + cooldown);

      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageActiveStepsMultiplier()
          .replace("%multiplier%", String.format("%.2f", actualSteps))
          .replace("%cooldown%", cooldownMessage)
          .replace("%time%", cooldownMessage),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.ACTIONBAR
      );
    }
  }

  @Unique
  private boolean cobbleDaycare$isVehiclePermitted() {
    String vehicleId = player.getVehicle() == null ? "" : player.getVehicle().getSavedEntityId();
    if (vehicleId == null) vehicleId = "";
    return CobbleDaycare.config.getPermittedVehicles().contains(vehicleId) || vehicleId.isEmpty();
  }

  @Unique private void cobbleDaycare$logDebug(String message) {
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, message);
    }
  }

  public int getTeleport() {
    return cobbleDaycare$teleport;
  }

  public void setTeleport(int teleport) {
    this.cobbleDaycare$teleport = teleport;
  }
}