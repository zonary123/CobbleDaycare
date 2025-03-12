package com.kingpixel.cobbledaycare.config;

import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.OptionsMecanics;
import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbledaycare.models.EggSpecialForm;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Config {
  private boolean debug;
  private String lang;
  private DataBaseConfig dataBase;
  private String commandEggInfo;
  private List<String> commands;
  private boolean showIvs;
  private boolean spawnEggWorld;
  private boolean dobbleDitto;
  private boolean PokeBallFromMother;
  private int raritySpawnEgg;
  private long ticksToWalking;
  private double defaultSteps;
  private Map<EggGroup, Double> steps;
  private int cooldowninstaBreedInSeconds;
  private int cooldowninstaHatchInSeconds;
  private int cooldownToOpenMenus;
  private List<Integer> slotPlots;
  private int cooldown;
  private double multiplierAbilityAcceleration;
  private List<String> abilityAcceleration;
  private double reduceEggStepsVehicle;
  private List<String> permittedVehicles;
  private Map<String, Integer> limitEggs;
  private Map<String, Integer> cooldowns;
  private OptionsMecanics optionsMecanics;
  private PokemonBlackList blackList;
  private List<EggForm> eggForms;
  private List<EggSpecialForm> eggSpecialForms;
  private FilterPokemons dobbleDittoFilter;

  public Config() {
    this.debug = false;
    this.lang = "en";
    this.showIvs = false;
    this.dobbleDitto = false;
    this.spawnEggWorld = true;
    this.PokeBallFromMother = true;
    this.commands = List.of("daycare", "pokebreed", "breed");
    this.commandEggInfo = "egginfo";
    this.multiplierAbilityAcceleration = 1.0;
    this.dataBase = new DataBaseConfig();
    this.dataBase.setDatabase("cobbledaycare");
    this.defaultSteps = 256D;
    this.steps = new HashMap<>();
    for (@NotNull EggGroup value : EggGroup.values()) {
      this.steps.put(value, 256D);
    }
    this.blackList = new PokemonBlackList();
    this.limitEggs = new HashMap<>();
    this.limitEggs.put("", 1);
    this.limitEggs.put("group.vip", 2);
    this.abilityAcceleration = List.of("magmaarmor",
      "flamebody",
      "steamengine");
    this.reduceEggStepsVehicle = 2f;
    this.permittedVehicles = List.of("minecraft:boat", "minecraft:horse", "cobblmeon:pokemon");

    this.cooldownToOpenMenus = 3;
    this.cooldown = 30;
    this.cooldowns = Map.of(
      "group.vip", 15,
      "group.legendary", 10,
      "group.master", 5
    );
    this.ticksToWalking = 20;
    this.slotPlots = new ArrayList<>();
    this.slotPlots.add(1);
    this.slotPlots.add(2);
    this.slotPlots.add(3);
    this.slotPlots.add(4);
    this.raritySpawnEgg = 2048;
    this.cooldowninstaBreedInSeconds = 60;
    this.cooldowninstaHatchInSeconds = 60;
    this.optionsMecanics = new OptionsMecanics();

    this.eggForms = List.of(
      new EggForm("galarian",
        List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
      new EggForm("paldean", List.of("clodsire")),
      new EggForm("hisuian", List.of("overqwil", "sneasler"))
    );
    this.dobbleDittoFilter = new FilterPokemons();

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

        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleDaycare.PATH, "config.json", Utils.newGson().toJson(CobbleDaycare.config)
        );
        if (futureWrite.join()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Config file created");
        } else {
          CobbleUtils.LOGGER.error("Error creating config file");
        }
      }
    );

    if (futureRead.join()) {
      CobbleUtils.LOGGER.info("CobbleDaycare Config file loaded");
    } else {
      CobbleDaycare.config = this;
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH, "config.json", Utils.newGson().toJson(CobbleDaycare.config)
      );
      if (futureWrite.join()) {
        CobbleUtils.LOGGER.info("Config file created");
      } else {
        CobbleUtils.LOGGER.error("Error creating config file");
      }
    }
  }
}
