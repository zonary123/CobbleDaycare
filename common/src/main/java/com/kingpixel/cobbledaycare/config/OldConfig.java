package com.kingpixel.cobbledaycare.config;

import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbledaycare.models.PokemonRareMecanic;
import com.kingpixel.cobbleutils.Model.FilterPokemons;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.Sound;
import lombok.Data;
import lombok.Getter;
import lombok.ToString;

import java.util.List;
import java.util.Map;


/**
 * @author Carlos Varas Alonso - 29/04/2024 0:14
 */
@Getter
@ToString
@Data
public class OldConfig {
  private boolean active;
  private String prefix;
  private boolean showIvs;
  private boolean changeuipasture;
  private boolean shifttoopen;
  private boolean obtainAspect;
  private boolean methodmasuda;
  private boolean aspectEggByType;
  private boolean ditto;
  private boolean doubleditto;
  private boolean spawnEggWorld;
  private boolean extraInfo;
  private long ticksToWalking;
  private int raritySpawnEgg;
  private boolean obtainPokeBallFromMother;
  private List<String> eggcommand;
  private double multiplierAbilityAcceleration;
  private List<String> abilityAcceleration;
  private double reduceEggStepsVehicle;
  private List<String> permittedVehicles;
  private String titleselectplot;
  private String titleplot;
  private String titleemptyplot;
  private String titleselectpokemon;
  private String nameAbandonedEgg;
  private String nameEgg;
  private String nameRandomEgg;
  private String permissionAutoClaim;
  private int defaultNumIvsToTransfer;
  private int maxIvsRandom;
  private int numberIvsDestinyKnot;
  private SuccessItems successItems;
  private float multipliermasuda;
  private float multiplierShiny;
  private int checkEggToBreedInSeconds;
  private int tickstocheck;
  private int cooldown;
  private Map<String, Integer> cooldowns;
  private int defaultNumberPlots;
  private int maxeggperplot;
  private int rowmenuselectplot;
  private int rowmenuplot;
  private int rowmenuselectpokemon;

  private int steps;
  private int cooldowninstaBreedInSeconds;
  private int cooldowninstaHatchInSeconds;

  private Sound soundCreateEgg;
  private String createEgg;
  private String notcancreateEgg;
  private String notbreedable;
  private String notdoubleditto;
  private String notditto;
  private String notCompatible;
  private String blacklisted;

  private List<Integer> plotSlots;
  private List<String> whitelist;
  private List<String> blacklist;
  private List<String> blacklistForm;
  private List<String> blacklistLabels;
  private List<String> blacklistFeatures;

  private ItemModel closeItem;
  private ItemModel plotItem;
  private ItemModel plotThereAreEggs;
  private ItemModel maleSelectItem;
  private ItemModel femaleSelectItem;
  private ItemModel infoItem;
  private ItemModel emptySlots;

  private List<Integer> maleSlots;
  private List<Integer> eggSlots;
  private List<Integer> femaleSlots;

  private List<EggForm> eggForms;
  private List<PokemonRareMecanic> pokemonRareMechanics;
  private FilterPokemons pokemonsForDoubleDitto;


  public OldConfig() {
    this.active = true;
    this.prefix = "&7[<#82d448>Breeding&7] &8» &a";
    this.showIvs = false;
    this.eggcommand = List.of("daycare", "pokebreed", "breed");
    this.multiplierAbilityAcceleration = 1.0;
    this.abilityAcceleration = List.of("magmaarmor",
      "flamebody",
      "steamengine");
    this.reduceEggStepsVehicle = 2f;
    this.permittedVehicles = List.of("minecraft:boat", "minecraft:horse", "cobblmeon:pokemon");
    this.titleselectplot = "<#82d448>Select Plot";
    this.titleplot = "<#82d448>Plot";
    this.titleemptyplot = "<#82d448>Plot";
    this.titleselectpokemon = "<#82d448>Select Pokemon";
    this.extraInfo = true;
    this.obtainAspect = false;
    this.changeuipasture = true;
    this.methodmasuda = true;
    this.ditto = true;
    this.doubleditto = true;
    this.spawnEggWorld = true;
    this.shifttoopen = true;
    this.aspectEggByType = true;
    this.obtainPokeBallFromMother = true;
    this.numberIvsDestinyKnot = 5;
    this.tickstocheck = 20;
    this.multipliermasuda = 1.5f;
    this.multiplierShiny = 1.5f;

    this.permissionAutoClaim = "cobbleutils.breeding.autoclaim";
    this.cooldown = 30;
    this.cooldowns = Map.of(
      "group.vip", 15,
      "group.legendary", 10,
      "group.master", 5
    );
    this.ticksToWalking = 20;
    this.defaultNumberPlots = 1;
    this.maxeggperplot = 3;
    this.steps = 128;
    this.checkEggToBreedInSeconds = 15;
    this.rowmenuselectplot = 3;
    this.rowmenuplot = 3;
    this.rowmenuselectpokemon = 6;
    this.raritySpawnEgg = 2048;
    this.cooldowninstaBreedInSeconds = 60;
    this.cooldowninstaHatchInSeconds = 60;
    this.defaultNumIvsToTransfer = 3;
    this.maxIvsRandom = 31;
    this.successItems = new SuccessItems();
    this.plotItem = new ItemModel(0, "minecraft:turtle_egg", "<#82d448>Plot", List.of(
      "&9male: &6%pokemon1% &f(&b%form1%&f) &f(&b%item1%&f)",
      "&dfemale: &6%pokemon2% &f(&b%form2%&f) &f(&b%item2%&f)",
      "&7Eggs: &6%eggs%",
      "&7Cooldown: &6%cooldown%"
    ), 0);
    this.infoItem = new ItemModel(4, "minecraft:book", "<#82d448>Info", List.of(
      "<#82d448>--- Info ---",
      "&7Transmit Ah: &6%ah%",
      "<#82d448>---- Items ----",
      "&7Destiny Knot: &6%destinyknot%",
      "&7Power Item: &6%poweritem%",
      "&7Ever Stone: &6%everstone%",
      "<#82d448>---- Shiny ----",
      "&7Masuda: &6%masuda% &7Multiplier: &6%multipliermasuda%",
      "&7Shiny Multiplier: &6%multipliershiny%",
      "&7ShinyRate: &6%shinyrate%",
      "<#82d448>---- Egg ----",
      "&7Egg Moves: &6%eggmoves%",
      "",
      "&7Max Ivs Random: &6%maxivs%",
      "&7Cooldown: &6%cooldown%"
    ), 0);
    this.plotSlots = List.of(
      10,
      12,
      14,
      16
    );
    this.plotThereAreEggs = new ItemModel(0, "minecraft:lime_wool", "", List.of(), 0);
    this.maleSlots = List.of();
    this.femaleSlots = List.of();
    this.eggSlots = List.of();
    this.emptySlots = new ItemModel(0, "minecraft:paper", "", List.of(""), 0);
    this.soundCreateEgg = new Sound("minecraft:entity.player.levelup");
    this.createEgg = "%prefix% <#ecca18>%pokemon1% %shiny1% &f(%form1%&f) <#64de7c>and <#ecca18>%pokemon2% %shiny2% &f(%form2%&f) <#64de7c>have created an egg <#ecca18>%egg%<#64de7c>!";
    this.notcancreateEgg = "%prefix% <#ecca18>%pokemon1% %shiny1% &f(%form1%&f) <#d65549>and <#ecca18>%pokemon2% %shiny2% &f(%form2%&f) <#d65549>can't create an egg!";
    this.notdoubleditto = "%prefix% <#d65549>you can't use two dittos!";
    this.notditto = "%prefix% <#d65549>you can't use one ditto!";
    this.blacklisted = "%prefix% <#ecca18>%pokemon% <#d65549>is blacklisted!";
    this.notbreedable = "%prefix% <#ecca18>%pokemon% <#d65549>is not breedable!";
    this.blacklist = List.of("pokestop", "egg", "manaphy");
    this.whitelist = List.of("manaphy");
    this.nameEgg = "%pokemon% Egg";
    this.nameRandomEgg = "Random Egg";
    this.nameAbandonedEgg = "Abandoned Egg";
    this.notCompatible = "%prefix% <#d65549>%pokemon1% and %pokemon2% is not compatible!";
    this.maleSelectItem = new ItemModel(10, "minecraft:light_blue_wool", "Male", List.of(""), 0);
    this.femaleSelectItem = new ItemModel(16, "minecraft:pink_wool", "Female", List.of(""), 0);
    this.blacklistForm = List.of("halloween");


    pokemonsForDoubleDitto = new FilterPokemons();
    pokemonsForDoubleDitto.setLegendarys(false);
    closeItem = null;

    blacklistLabels = List.of(
      "legendary",
      "mythical",
      "ultra_beast",
      "gmax"
    );
    blacklistFeatures = List.of(
      "netherite_coating"
    );

  }


  @Data
  public static class SuccessItems {
    private double percentageTransmitAH;
    private double percentageDestinyKnot;
    private double percentagePowerItem;
    private double percentageEverStone;
    private double percentageEggMoves;

    public SuccessItems() {
      this.percentageTransmitAH = 70.0;
      this.percentageDestinyKnot = 100.0;
      this.percentagePowerItem = 100.0;
      this.percentageEverStone = 100.0;
      this.percentageEggMoves = 100.0;
    }

  }
}