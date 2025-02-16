package com.kingpixel.cobbledaycare.config;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbledaycare.models.EggSpecialForm;
import com.kingpixel.cobbledaycare.models.mecanics.OptionsMecanics;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.DataBaseConfig;
import com.kingpixel.cobbleutils.Model.PokemonBlackList;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 30/01/2025 23:47
 */
@Getter
@Setter
public class Config {
  private boolean debug;
  private String lang;
  private boolean showIvs;
  private DataBaseConfig dataBase;
  private List<String> commands;
  private OptionsMecanics optionsMecanics;
  private PokemonBlackList blackList;
  private List<EggForm> eggForms;
  private List<EggSpecialForm> eggSpecialForms;

  public Config() {
    this.debug = false;
    this.lang = "en";
    this.showIvs = false;
    this.dataBase = new DataBaseConfig();
    dataBase.setDatabase("cobbledaycare");
    this.commands = List.of(
      "daycare",
      "breed",
      "pokebreed"
    );
    this.optionsMecanics = new OptionsMecanics();
    this.blackList = new PokemonBlackList();
    this.eggForms = List.of(
      new EggForm("normal", List.of("bulbasaur", "charmander", "squirtle")),
      new EggForm("alolan", List.of("rattata", "sandshrew", "vulpix"))
    );
    this.eggSpecialForms = List.of(
      new EggSpecialForm("galar",
        List.of(
          ""
        ))
    );
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
      CobbleUtils.LOGGER.info("Config file loaded");
    } else {
      CobbleDaycare.config = this;
      CompletableFuture<Boolean> futureWrite = Utils.writeFileAsync(
        CobbleDaycare.PATH, "config.json", Utils.newGson().toJson(this)
      );
      if (futureWrite.join()) {
        CobbleUtils.LOGGER.info("Config file created");
      } else {
        CobbleUtils.LOGGER.error("Error creating config file");
      }
    }
  }
}
