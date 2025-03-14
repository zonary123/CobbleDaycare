package com.kingpixel.cobbledaycare.config;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.ui.PlotMenu;
import com.kingpixel.cobbledaycare.ui.PrincipalMenu;
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
  private String messageActiveStepsMultiplier;
  private String messageEggCreated;
  private String messageLimitEggs;
  private String messageBanPokemon;
  private PrincipalMenu principalMenu;
  private PlotMenu plotMenu;
  private SelectPokemonMenu selectPokemonMenu;

  public Language() {
    this.prefix = "&7[&6CobbleDaycare&7] ";
    this.messageActiveStepsMultiplier = "&7Active Steps multiplier x%multiplier% - &6%time%";
    this.messageEggCreated = "%prefix% &6%pokemon1% %form1% &7and &6%pokemon2% %form2% &7have created %pokemon3% %form3%";
    this.messageLimitEggs = "%prefix% &aYou have reached the limit of eggs in the plot";
    this.messageBanPokemon = "%prefix% &cThis pokemon %pokemon% %form% %item% is banned in the plot %plot%";
    this.principalMenu = new PrincipalMenu();
    this.plotMenu = new PlotMenu();
    this.selectPokemonMenu = new SelectPokemonMenu();
  }

  public void init() {
    CompletableFuture<Boolean> futureRead = Utils.readFileAsync(
      CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", call -> {
        CobbleDaycare.language = Utils.newGson().fromJson(call, Language.class);

        CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
          CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", Utils.newGson().toJson(CobbleDaycare.language)
        );
        if (futureWrite.join()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Language file created");
        } else {
          CobbleUtils.LOGGER.error("Error creating language file");
        }
      }
    );

    if (futureRead.join()) {
      CobbleUtils.LOGGER.info("Language file loaded");
    } else {
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH_LANGUAGE, CobbleDaycare.config.getLang() + ".json", Utils.newGson().toJson(CobbleDaycare.language)
      );
      if (futureWrite.join()) {
        CobbleUtils.LOGGER.info("Language file created");
      } else {
        CobbleUtils.LOGGER.error("Error creating language file");
      }
    }
  }
}
