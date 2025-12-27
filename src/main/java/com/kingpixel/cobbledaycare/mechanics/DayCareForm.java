package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.feature.ChoiceSpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatureProvider;
import com.cobblemon.mod.common.api.pokemon.feature.SpeciesFeatures;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbledaycare.models.EggForm;
import com.kingpixel.cobbledaycare.models.HatchBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.Map;

/**
 * Refactorizado por claridad y reducción de duplicación.
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareForm extends Mechanics {
  public static final String TAG = "form";
  private Map<String, String> forms;
  private List<EggForm> eggForms;
  private List<String> blacklistForm;
  private List<String> blacklistFeatures;

  public DayCareForm() {
    this.forms = Map.of(
      "galar", "galarian",
      "paldea", "paldean",
      "hisui", "hisuian",
      "alola", "alolan"
    );
    this.eggForms = List.of(
      new EggForm("galarian",
        List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
      new EggForm("paldean", List.of("clodsire")),
      new EggForm("hisuian", List.of("overqwil", "sneasler"))
    );
    this.blacklistForm = List.of("halloween", "disguised");
    this.blacklistFeatures = List.of("netherite_coating", "disguised");
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon female = builder.getFemale();
    Pokemon male = builder.getMale();
    Pokemon egg = builder.getEgg();
    Pokemon pokemonFinal = male.heldItem().getItem().equals(CobblemonItems.EVERSTONE)
      ? male
      : female;
    Pokemon firstEvolution = builder.getFirstEvolution();


    String configForm = getConfigForm(pokemonFinal);
    if (configForm != null) {
      if (blacklistForm.contains(configForm)) configForm = "";
      applyForm(egg, configForm, firstEvolution);
      return;
    }

    StringBuilder form = new StringBuilder(getRegionalForm(female));
    form.append(processAspects(female));
    form.append(processFeatures(female));

    if (isBlacklisted(form.toString())) {
      form = new StringBuilder();
    }

    applyForm(egg, form.toString(), firstEvolution);
  }

  @Override
  public void createEgg(ServerPlayerEntity player, Pokemon female, Pokemon egg) {
    Pokemon firstEvolution = female;

    String configForm = getConfigForm(female);
    if (configForm != null) {
      if (blacklistForm.contains(configForm)) configForm = "";
      applyForm(egg, configForm, firstEvolution);
      return;

    }

    StringBuilder form = new StringBuilder(getRegionalForm(female));
    form.append(processAspects(female));
    form.append(processFeatures(female));

    if (isBlacklisted(form.toString())) {
      form = new StringBuilder();
    }

    applyForm(egg, form.toString(), firstEvolution);
  }

  private String getConfigForm(Pokemon pokemon) {
    String form = null;
    for (EggForm eggForm : eggForms) {
      if (eggForm.getPokemons().contains(pokemon.showdownId())) {
        form = eggForm.getForm();
        break;
      }
    }
    if (form == null)
      form = forms.getOrDefault(pokemon.getForm().formOnlyShowdownId(), form);
    return form;
  }

  private String getRegionalForm(Pokemon female) {
    return switch (female.getSpecies().showdownId()) {
      case "perrserker", "sirfetchd", "mrrime", "cursola", "obstagoon", "runerigus" -> "galarian ";
      case "clodsire" -> "paldean";
      case "overqwil", "sneasler" -> "hisuian";
      default -> "";
    };
  }

  private String processAspects(Pokemon female) {
    List<String> aspects = female.getForm().getAspects();
    StringBuilder form = new StringBuilder(aspects.isEmpty() ? "" : aspects.get(0));
    form = new StringBuilder(form.toString().replace("-", "_"));

    int lastUnderscoreIndex = form.lastIndexOf("_");
    if (lastUnderscoreIndex != -1) {
      form = new StringBuilder(form.substring(0, lastUnderscoreIndex) + "=" + form.substring(lastUnderscoreIndex + 1));
    }

    for (String label : female.getForm().getLabels()) {
      if (label.contains("regional") || label.contains("gen8a")) {
        form.append(" ").append("region_bias=").append(female.getForm().formOnlyShowdownId());
      }
    }
    return form.toString();
  }

  private String processFeatures(Pokemon female) {
    StringBuilder form = new StringBuilder();
    for (SpeciesFeatureProvider<?> speciesFeatureProvider : SpeciesFeatures.INSTANCE.getFeaturesFor(female.getSpecies())) {
      if (speciesFeatureProvider instanceof ChoiceSpeciesFeatureProvider choice) {
        var choiceForm = choice.get(female);
        if (choiceForm != null) {
          String name = choiceForm.getName();
          String value = choiceForm.getValue();
          if (isBlacklisted(name, value)) continue;
          form.append(" ").append(name).append("=").append(value);
        }
      }
    }
    return form.toString();
  }

  private boolean isBlacklisted(String... values) {
    for (String value : values) {
      if (blacklistFeatures.contains(value) || blacklistForm.contains(value)) {
        return true;
      }
    }
    return false;
  }

  private void applyForm(Pokemon egg, String form, Pokemon firstEvolution) {
    egg.getPersistentData().putString(TAG, form);
    PokemonProperties.Companion.parse(form).apply(firstEvolution);
  }

  @Override
  public void applyHatch(HatchBuilder builder) {
    Pokemon egg = builder.getEgg();
    String form = egg.getPersistentData().getString(TAG);
    PokemonProperties.Companion.parse(form).apply(builder.getPokemon());
    CobbleDaycare.fixBreedable(builder.getPokemon());
    egg.getPersistentData().remove(TAG);
  }

  @Override
  public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%form%", nbt.getString(TAG));
  }

  @Override
  public void validateData() {
  }

  @Override
  public String fileName() {
    return "form";
  }

  @Override
  public String replace(String text, ServerPlayerEntity player) {
    return text;
  }
}