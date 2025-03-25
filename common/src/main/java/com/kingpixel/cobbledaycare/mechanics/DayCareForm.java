package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareForm extends Mechanics {
  public static final String TAG = "form";
  private List<EggForm> eggForms;
  private List<String> blacklistForm;
  private List<String> blacklistFeatures;

  public DayCareForm() {
    this.eggForms = List.of(
      new EggForm("galarian",
        List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
      new EggForm("paldean", List.of("clodsire")),
      new EggForm("hisuian", List.of("overqwil", "sneasler"))
    );
    this.blacklistForm = List.of("halloween");
    this.blacklistFeatures = List.of(
      "netherite_coating"
    );
  }


  @Override
  public void applyEgg(EggBuilder builder) {
    StringBuilder form = new StringBuilder();
    Pokemon female = builder.getFemale();
    Pokemon egg = builder.getEgg();
    switch (female.getSpecies().showdownId()) {
      case "perrserker":
      case "sirfetchd":
      case "mrrime":
      case "cursola":
      case "obstagoon":
      case "runerigus":
        form.append("galarian ");
        break;
      case "clodsire":
        form.append("paldean");
        break;
      case "overqwil":
      case "sneasler":
        form.append("hisuian");
        break;
    }


    String configForm = null;

    for (EggForm eggForm : eggForms) {
      if (eggForm.getPokemons().contains(female.showdownId())) {
        configForm = eggForm.getForm();
        break;
      }
    }


    if (configForm != null) {
      if (getBlacklistForm().contains(configForm)) configForm = "";
      applyForm(egg, configForm);
      return;
    }

    List<String> aspects = female.getForm().getAspects();

    if (aspects.isEmpty()) {
      form = new StringBuilder();
    } else {
      form.append(aspects.getFirst());
    }


    form = new StringBuilder(form.toString().replace("-", "_"));

    int lastUnderscoreIndex = form.lastIndexOf("_");

    if (lastUnderscoreIndex != -1) {
      form = new StringBuilder(form.substring(0, lastUnderscoreIndex) + "=" + form.substring(lastUnderscoreIndex + 1));
    }


    // Form Regional
    for (String label : female.getForm().getLabels()) {
      if (label.contains("regional") || label.contains("gen8a")) {
        form.append(" ").append("region_bias=").append(female.getForm().formOnlyShowdownId());
      }
    }

    for (SpeciesFeatureProvider<?> speciesFeatureProvider : SpeciesFeatures.INSTANCE.getFeaturesFor(female.getSpecies())) {
      if (speciesFeatureProvider instanceof ChoiceSpeciesFeatureProvider choice) {
        var choiceForm = choice.get(female);
        if (choiceForm != null) {
          String name = choiceForm.getName();
          String value = choiceForm.getValue();
          if (blacklistFeatures.contains(name)
            || blacklistFeatures.contains(value)
            || blacklistForm.contains(name)
            || blacklistForm.contains(value)) continue;
          if (CobbleDaycare.config.isDebug()) {
            CobbleUtils.LOGGER.info("Feature -> Name: " + name + " Value: " + value);
          }
          form.append(" ").append(name).append("=").append(value);
        }
      }
    }

    if (blacklistForm.contains(form.toString()))
      form = new StringBuilder();

    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("Form: " + form);
    }

    applyForm(egg, form.toString());
  }

  private void applyForm(Pokemon egg, String form) {
    egg.getPersistentData().putString(TAG, form);
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String form = egg.getPersistentData().getString(TAG);
    PokemonProperties.Companion.parse(form).apply(egg);
    egg.getPersistentData().remove(TAG);
    CobbleDaycare.fixBreedable(egg);
  }

  @Override public void commandCreateEgg(ServerPlayerEntity player, Pokemon pokemon) {

  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "form";
  }

  @Override public String replace(String text) {
    return text;
  }
}
