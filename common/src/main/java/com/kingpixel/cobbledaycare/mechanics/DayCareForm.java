package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCareForm extends Mechanics {
  public static final String TAG = "form";
  private boolean active;

  public DayCareForm() {
    this.active = true;
  }


  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    StringBuilder form = new StringBuilder();


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

    for (EggForm eggForm : CobbleDaycare.config.getEggForms()) {
      if (eggForm.getPokemons().contains(female.showdownId())) {
        configForm = eggForm.getForm();
        break;
      }
    }


    if (configForm != null) {
      if (CobbleUtils.breedconfig.getBlacklistForm().contains(configForm)) configForm = "";
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
          if (CobbleUtils.breedconfig.getBlacklistFeatures().contains(name)
            || CobbleUtils.breedconfig.getBlacklistFeatures().contains(value)
            || CobbleUtils.breedconfig.getBlacklistForm().contains(name)
            || CobbleUtils.breedconfig.getBlacklistForm().contains(value)) continue;
          if (CobbleDaycare.config.isDebug()) {
            CobbleUtils.LOGGER.info("Feature -> Name: " + name + " Value: " + value);
          }
          form.append(" ").append(name).append("=").append(value);
        }
      }
    }

    if (CobbleUtils.breedconfig.getBlacklistForm().contains(form.toString()))
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
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "form";
  }
}
