package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbledaycare.models.Position;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Mixin(value = ServerPlayNetworkHandler.class, priority = 9999)
public abstract class WalkBreedingMixin {
  @Unique private static final long TICKS_TO_MILLISECONDS = 50;
  @Unique private static final int MAX_TELEPORT = 2;
  // Mapa para guardar la posición inicial de cada jugador
  @Unique
  private static final Map<ServerPlayerEntity, Position> cobbleDaycare$playerPositions = new ConcurrentHashMap<>();
  @Unique
  private static final Map<ServerPlayerEntity, Integer> cobbleDaycare$playerTeleport = new ConcurrentHashMap<>();
  @Unique
  private static final ScheduledExecutorService cobbleDaycare$scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder()
    .setNameFormat("CobbleDaycare-walk-breeding-%d")
    .build());

  static {
    cobbleDaycare$scheduler.scheduleAtFixedRate(() -> {
      try {
        cobbleDaycare$processPlayers();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }, 5, 1, TimeUnit.SECONDS);
  }

  @Unique
  private ServerPlayerEntity cobbleDaycare$player;

  @Unique
  private static void cobbleDaycare$processPlayers() {
    if (CobbleDaycare.server == null) return;
    long currentTime = System.currentTimeMillis();
    if (CobbleDaycare.server.getPlayerManager().getPlayerList().isEmpty()) return;
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("[CobbleDaycare] Processing players for egg step updates...");
    }
    var players = new ArrayList<>(CobbleDaycare.server.getPlayerManager().getPlayerList());
    int size = players.size();
    for (int i = 0; i < size; i++) {
      try {
        ServerPlayerEntity player = players.get(i);
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

  @Unique private static boolean cobbleDaycare$isPlayerEligibleForStepUpdate(ServerPlayerEntity player) {
    return (CobbleDaycare.config.isAllowElytra() || !player.isInPose(EntityPose.FALL_FLYING))
      && !player.getAbilities().flying
      && !player.isInvulnerable()
      && cobbleDaycare$isVehiclePermitted(player)
      && (!player.isTouchingWater() || player.isInPose(EntityPose.SWIMMING));
  }

  // -------------------- Mismos métodos existentes pero sin CompletableFuture --------------------

  @Unique private static Entity cobbleDaycare$getEffectiveEntity(ServerPlayerEntity player) {
    return player.getVehicle() != null ? player.getVehicle() : player;
  }

  @Unique
  private static void cobbleDaycare$updateEggSteps(PlayerPartyStore party, double deltaMovement, ServerPlayerEntity player) {
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInformation == null) return;
    for (Pokemon pokemon : party) {
      if (pokemon != null && "egg".equals(pokemon.showdownId())) {
        EggData.steps(player, pokemon, deltaMovement, userInformation);
      }
    }
  }

  @Unique private static void cobbleDaycare$sendMessage(ServerPlayerEntity player) {
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

  @Unique
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

  @Inject(method = "onTeleportConfirm", at = @At("HEAD"))
  public void onTeleportConfirm(TeleportConfirmC2SPacket packet, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }

  @Inject(method = "requestTeleport(DDDFF)V", at = @At("HEAD"))
  public void requestTeleport(double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }

  @Inject(method = "requestTeleport(DDDFFLjava/util/Set;)V", at = @At("HEAD"))
  public void requestTeleportWithSet(double x, double y, double z, float yaw, float pitch, java.util.Set<?> set, CallbackInfo ci) {
    if (cobbleDaycare$player == null) {
      cobbleDaycare$player = ((ServerPlayNetworkHandler) (Object) this).player;
    }
    cobbleDaycare$playerTeleport.put(cobbleDaycare$player, MAX_TELEPORT);
  }
}
