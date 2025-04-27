package com.kingpixel.cobbledaycare.config;

import com.cobblemon.mod.common.api.pokemon.stats.Stats;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.DaycareIvs;
import com.kingpixel.cobbledaycare.ui.PlotMenu;
import com.kingpixel.cobbledaycare.ui.PrincipalMenu;
import com.kingpixel.cobbledaycare.ui.ProfileMenu;
import com.kingpixel.cobbledaycare.ui.SelectPokemonMenu;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Language {
  private String prefix;
  private String eggName;
  private String eggInfo;
  private String infoAbility;
  private String messageReload;
  private String messageCooldownBreed;
  private String messageCooldownHatch;
  private String messageActiveStepsMultiplier;
  private String messageEggCreated;
  private String messageLimitEggs;
  private String messageBanPokemon;
  private String messageCooldownOpenMenu;
  private String messageItNotEgg;
  private String messageBreedable;
  private String messageRemovedFemale;
  private String messageRemovedMale;
  private String messageCannotBreed;
  private PrincipalMenu principalMenu;
  private PlotMenu plotMenu;
  private SelectPokemonMenu selectPokemonMenu;
  private ProfileMenu profileMenu;

  public Language() {

    this.prefix = "&7[&6CobbleDaycare&7] ";
    StringBuilder eggInfoBuilder = new StringBuilder();
    eggInfoBuilder.append("&7[<#de896f>Egginfo&7] &6Pokemon: &f%pokemon% %gender%%shiny% &f(&b%form%&f)\n")
      .append("<#b0eb59>Steps: &f%steps%&7/&f%cycles%\n");
    eggInfoBuilder.append("<#83a7de>IVs:\n ");
    int ivs = 0;
    for (Stats stat : DaycareIvs.stats) {
      String color;
      switch (stat) {
        case HP -> color = "<#ee8339>";
        case ATTACK -> color = "<#e84b48>";
        case DEFENCE -> color = "<#5d79e1>";
        case SPECIAL_ATTACK -> color = "<#40b5cd>";
        case SPECIAL_DEFENCE -> color = "<#f59bc2>";
        case SPEED -> color = "<#69cd65>";
        default -> color = "<#f7b2a1>";
      }
      eggInfoBuilder.append(color).append(stat.getShowdownId().toUpperCase()).append(": &f%iv_")
        .append(stat.getShowdownId()).append("%");
      ivs++;
      if (ivs % 3 == 0) {
        eggInfoBuilder.append("\n ");
      } else {
        eggInfoBuilder.append(" &7- ");
      }
    }
    eggInfoBuilder.deleteCharAt(eggInfoBuilder.length() - 1);
    eggInfoBuilder.append("<#6fa7de>Ability: &f%ability% %ha%\n")
      .append("<#9be8c2>Nature: &f%nature%\n")
      .append("<#98eb59>Egg Moves: &f%eggmoves%\n");

    this.eggInfo = eggInfoBuilder.toString();
    this.messageCannotBreed = "%prefix% &cYou cannot breed this pokemons";
    this.eggName = "%steps%/%cycles% %pokemon%";
    this.messageReload = "%prefix% &aReloaded";
    this.messageCooldownBreed = "%prefix% &7Cooldown to breed %cooldown%";
    this.messageCooldownHatch = "%prefix% &7Cooldown to hatch %cooldown%";
    this.messageActiveStepsMultiplier = "&7Active Steps multiplier x%multiplier% - &6%cooldown%";
    this.messageEggCreated = "%prefix% &6%pokemon1% %form1% &7and &6%pokemon2% %form2% &7have created &6%pokemon3% %form3%";
    this.messageLimitEggs = "%prefix% &aYou have reached the limit of eggs in the plot %plot%";
    this.messageBanPokemon = "%prefix% &cThis pokemon %pokemon% %form% %item% is banned in the plot %plot%";
    this.messageCooldownOpenMenu = "%prefix% &7You must wait %cooldown% to open the menu again";
    this.messageItNotEgg = "%prefix% &cThis is not an egg";
    this.messageBreedable = "%prefix% &7This pokemon %pokemon% %form% is now breedable: %breedable%";
    this.messageRemovedFemale = "%prefix% &7Removed the female pokemon in the plot %plot%";
    this.messageRemovedMale = "%prefix% &7Removed the male pokemon in the plot %plot%";
    this.infoAbility = "&b%male% &e%maleHA% &7| &d%female% &e%femaleHA%";
    this.principalMenu = new PrincipalMenu();
    this.plotMenu = new PlotMenu();
    this.selectPokemonMenu = new SelectPokemonMenu();
    this.profileMenu = new ProfileMenu();
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", call -> {
        CobbleDaycare.language = Utils.newGson().fromJson(call, Language.class);

        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", Utils.newGson().toJson(CobbleDaycare.language)
        );
        if (!futureWrite.join()) {
          CobbleUtils.LOGGER.error("Error creating language file");
        }
      }
    );

    if (!futureRead.join()) {
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", Utils.newGson().toJson(CobbleDaycare.language)
      );
      if (!futureWrite.join()) {
        CobbleUtils.LOGGER.error("Error creating language file");
      }
    }
  }
}
