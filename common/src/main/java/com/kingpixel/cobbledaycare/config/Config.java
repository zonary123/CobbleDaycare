package com.kingpixel.cobbledaycare.config;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Config {
  private static Map<UUID, Long> cooldownsOpenMenus = new HashMap<>();
  private boolean debug;
  private String lang;
  private DataBaseConfig dataBase;
  private String commandEggInfo;
  private List<String> commands;
  private boolean fixIlegalAbilities;
  private boolean canUseNativeGUI;
  private boolean showIvs;
  private boolean spawnEggWorld;
  private boolean dobbleDitto;
  private boolean allowElytra;
  private int raritySpawnEgg;
  private long ticksToWalking;
  private boolean globalMultiplierSteps;
  private float multiplierSteps;
  private double defaultSteps;
  private Map<EggGroup, Double> steps;
  private int cooldown;
  private Map<String, Integer> cooldowns;
  private int defaultCooldownBreed;
  private Map<String, Integer> cooldownsBreed;
  private int defaultCooldownHatch;
  private Map<String, Integer> cooldownsHatch;
  private int cooldownToOpenMenus;
  private List<Integer> slotPlots;
  private double multiplierAbilityAcceleration;
  private List<String> abilityAcceleration;
  private double reduceEggStepsVehicle;
  private List<String> permittedVehicles;
  private List<String> whitelist;
  private Map<String, Integer> limitEggs;
  private PokemonBlackList blackList;
  private FilterPokemons dobbleDittoFilter;

  public Config() {
    this.debug = false;
    this.fixIlegalAbilities = false;
    this.canUseNativeGUI = false;
    this.lang = "en";
    this.dataBase = new DataBaseConfig();
    this.showIvs = false;
    this.dobbleDitto = false;
    this.spawnEggWorld = true;
    this.allowElytra = true;
    this.commands = List.of("daycare", "pokebreed", "breed");
    this.commandEggInfo = "egginfo";
    this.globalMultiplierSteps = false;
    this.multiplierAbilityAcceleration = 1.0;
    this.dataBase.setDatabase("cobbledaycare");
    this.defaultSteps = 256D;
    this.steps = new HashMap<>();
    for (@NotNull EggGroup value : EggGroup.values()) {
      this.steps.put(value, 256D);
    }
    this.blackList = new PokemonBlackList();
    this.blackList.getPokemons().add("egg");
    this.blackList.getLabels().add("basculegion");
    this.blackList.getLabels().add("legendary");
    this.limitEggs = new HashMap<>();
    this.limitEggs.put("", 1);
    this.limitEggs.put("group.vip", 2);
    this.abilityAcceleration = List.of("magmaarmor",
      "flamebody",
      "steamengine");
    this.reduceEggStepsVehicle = 2f;
    this.multiplierSteps = 1.0f;
    this.permittedVehicles = List.of("minecraft:boat", "minecraft:horse", "cobblemon:pokemon");
    this.cooldownToOpenMenus = 3;
    this.cooldown = 30;
    this.cooldowns = Map.of(
      "group.vip", 15,
      "group.legendary", 10,
      "group.master", 5
    );
    this.ticksToWalking = 20;
    this.slotPlots = new ArrayList<>();
    this.slotPlots.add(10);
    this.slotPlots.add(12);
    this.slotPlots.add(14);
    this.slotPlots.add(16);
    this.raritySpawnEgg = 2048;
    this.defaultCooldownBreed = 60;
    this.cooldownsBreed = new HashMap<>();
    this.cooldownsBreed.put("group.vip", 30);
    this.defaultCooldownHatch = 60;
    this.cooldownsHatch = new HashMap<>();
    this.cooldownsHatch.put("group.vip", 30);
    this.whitelist = new ArrayList<>();
    this.dobbleDittoFilter = new FilterPokemons();

  }

  public void check() {
    if (ticksToWalking < 20) ticksToWalking = 20;

  }

  public boolean hasOpenCooldown(ServerPlayerEntity player) {
    Long cooldown = cooldownsOpenMenus.get(player.getUuid());
    boolean b = cooldown != null && cooldown > System.currentTimeMillis();
    if (!b) {
      cooldownsOpenMenus.put(player.getUuid(), System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(cooldownToOpenMenus));
      return false;
    }
    PlayerUtils.sendMessage(
      player,
      CobbleDaycare.language.getMessageCooldownOpenMenu()
        .replace("%cooldown%", PlayerUtils.getCooldown(new Date(cooldown))),
      CobbleDaycare.language.getPrefix(),
      TypeMessage.CHAT
    );
    return true;
  }

  public double getSteps(Pokemon pokemon) {
    double d = this.defaultSteps;
    for (EggGroup eggGroup : pokemon.getForm().getEggGroups()) {
      if (this.steps.containsKey(eggGroup)) {
        if (this.steps.get(eggGroup) < d) {
          d = this.steps.get(eggGroup);
        }
      }
    }
    return d;
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleDaycare.PATH, "config.json", call -> {
        CobbleDaycare.config = Utils.newGson().fromJson(call, Config.class);
        CobbleDaycare.config.check();
        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleDaycare.PATH, "config.json", Utils.newGson().toJson(CobbleDaycare.config)
        );
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.error("Error creating config file");
        }
      }
    );

    if (!futureRead.join()) {
      CobbleDaycare.config = this;
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH, "config.json", Utils.newGson().toJson(CobbleDaycare.config)
      );
      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.error("Error creating config file");
      }
    }
  }
}
