package com.kingpixel.cobbledaycare.tasks;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbledaycare.models.Position;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Data;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Unique;

import java.util.Collections;
import java.util.concurrent.*;

/**
 * @author Carlos Varas Alonso - 24/11/2025 8:05
 */
@Data
public class TaskDayCare implements Runnable {
  public static final ConcurrentMap<ServerPlayerEntity, Integer> cobbleDaycare$playerTeleport = new ConcurrentHashMap<>();
  private static final long TICKS_TO_MILLISECONDS = 50;
  private static final ConcurrentMap<ServerPlayerEntity, Position> cobbleDaycare$playerPositions =
    new ConcurrentHashMap<>();
  private static final ScheduledExecutorService cobbleDaycare$scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
    .setNameFormat("CobbleDaycare-walk-breeding-%d")
    .build());

  public TaskDayCare() {
    cobbleDaycare$scheduler.scheduleAtFixedRate(this, 5, 1, TimeUnit.SECONDS);
  }

  private static void cobbleDaycare$sendMessageMultiplierSteps(UserInformation userInformation, ServerPlayerEntity player) {
    boolean activeMultiplier = CobbleDaycare.config.isGlobalMultiplierSteps();
    float multiplier = userInformation.getActualMultiplier(player);
    boolean hasMultiplier = multiplier >= CobbleDaycare.config.getMultiplierSteps();

    if (activeMultiplier || hasMultiplier) {
      long cooldown = userInformation.getTimeMultiplierSteps() * TICKS_TO_MILLISECONDS;
      String cooldownMessage = PlayerUtils.getCooldown(System.currentTimeMillis() + cooldown);

      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageActiveStepsMultiplier()
          .replace("%multiplier%", String.format("%.2f", multiplier))
          .replace("%cooldown%", cooldownMessage)
          .replace("%time%", cooldownMessage),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.ACTIONBAR
      );
    }
  }

  @Unique
  private static boolean cobbleDaycare$isVehiclePermitted(ServerPlayerEntity player) {
    String vehicleId = player.getVehicle() == null ? "" : player.getVehicle().getSavedEntityId();
    if (vehicleId == null) vehicleId = "";
    return CobbleDaycare.config.getPermittedVehicles().contains(vehicleId) || vehicleId.isEmpty();
  }

  // -------------------- Mismos métodos existentes pero sin CompletableFuture --------------------

  private boolean cobbleDaycare$isPlayerEligibleForStepUpdate(ServerPlayerEntity player) {
    return (CobbleDaycare.config.isAllowElytra() || !player.isInPose(EntityPose.FALL_FLYING))
      && !player.getAbilities().flying
      && cobbleDaycare$isVehiclePermitted(player)
      && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING));
  }

  private Entity cobbleDaycare$getEffectiveEntity(ServerPlayerEntity player) {
    return player.getVehicle() != null ? player.getVehicle() : player;
  }

  private void cobbleDaycare$updateEggSteps(PlayerPartyStore party, double deltaMovement, ServerPlayerEntity player) {
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInformation == null) return;
    for (Pokemon pokemon : party) {
      if (pokemon != null && "egg".equals(pokemon.showdownId())) {
        EggData.steps(player, pokemon, deltaMovement, userInformation);
      }
    }
  }

  private void cobbleDaycare$sendMessage(ServerPlayerEntity player) {
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInformation == null) return;
    long timeMultiplierSteps = userInformation.getTimeMultiplierSteps();

    if (timeMultiplierSteps > 0) {
      timeMultiplierSteps -= CobbleDaycare.config.getTicksToWalking();
      userInformation.setTimeMultiplierSteps(timeMultiplierSteps);

      if (timeMultiplierSteps <= 0) {
        userInformation.setMultiplierSteps(CobbleDaycare.config.getMultiplierSteps());
        userInformation.setTimeMultiplierSteps(0);
      }

      if (userInformation.isActionBar()) {
        cobbleDaycare$sendMessageMultiplierSteps(userInformation, player);
      }
    }
  }

  private void cobbleDaycare$processPlayers() {
    if (CobbleDaycare.server == null) return;
    long currentTime = System.currentTimeMillis();
    if (CobbleDaycare.server.getPlayerManager().getPlayerList().isEmpty()) return;
    var players = Collections.synchronizedList(CobbleDaycare.server.getPlayerManager().getPlayerList());
    for (ServerPlayerEntity serverPlayerEntity : players) {
      try {
        ServerPlayerEntity player = serverPlayerEntity;
        UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
        if (userInformation == null) continue;
        if (player == null || !player.isAlive() || player.isRemoved()) continue;
        if (!cobbleDaycare$isPlayerEligibleForStepUpdate(player)) continue;

        Entity entity = cobbleDaycare$getEffectiveEntity(player);
        if (entity == null) continue;

        // Obtiene posición inicial o la crea si no existe
        Position pos = cobbleDaycare$playerPositions.computeIfAbsent(player, p ->
          new Position(entity.getX(), entity.getZ(), currentTime)
        );

        double deltaX = entity.getX() - pos.getX();
        double deltaZ = entity.getZ() - pos.getZ();
        double deltaMovement = Math.hypot(deltaX, deltaZ);

        // Manejo de teletransportes
        int teleportCount = cobbleDaycare$playerTeleport.getOrDefault(player, 0);
        if (deltaMovement <= 0 || teleportCount > 0) {
          if (teleportCount > 0) teleportCount--;
          cobbleDaycare$playerTeleport.put(player, teleportCount);
          pos.setX(entity.getX());
          pos.setZ(entity.getZ());
          pos.setLastUpdate(currentTime);
          continue;
        }

        // Actualiza pasos de huevos y info de usuario
        PlayerPartyStore party = Cobblemon.INSTANCE.getStorage().getParty(player);
        cobbleDaycare$updateEggSteps(party, deltaMovement, player);
        cobbleDaycare$sendMessage(player);

        // Actualiza posición inicial para el siguiente intervalo
        pos.setX(entity.getX());
        pos.setZ(entity.getZ());
        pos.setLastUpdate(currentTime);
        cobbleDaycare$playerPositions.put(player, pos);

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override public void run() {
    try {
      cobbleDaycare$processPlayers();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
