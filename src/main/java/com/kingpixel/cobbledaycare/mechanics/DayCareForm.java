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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class DayCareForm extends Mechanics {

  public static final String TAG = "form";
  private static final Logger LOGGER = LogManager.getLogger(CobbleDaycare.MOD_ID);

  private final Map<String, String> forms = new HashMap<>();
  private final List<EggForm> eggForms = new ArrayList<>();
  private final List<String> blacklistForm = new ArrayList<>();
  private final List<String> blacklistFeatures = new ArrayList<>();

  public DayCareForm() {
    validateData();

    eggForms.addAll(List.of(
      new EggForm("galarian", List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
      new EggForm("paldean", List.of("clodsire")),
      new EggForm("hisuian", List.of("overqwil", "sneasler"))
    ));

    blacklistForm.addAll(List.of("halloween", "disguised"));
    blacklistFeatures.addAll(List.of("netherite_coating", "disguised"));
  }

  /* ------------------------------------------------------------ */
  /* DEBUG                                                        */
  /* ------------------------------------------------------------ */

  private void debug(String msg, Object... args) {
    if (CobbleDaycare.config.isDebug()) {
      LOGGER.info(msg, args);
    }
  }

  /* ------------------------------------------------------------ */
  /* APPLY EGG                                                    */
  /* ------------------------------------------------------------ */

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon female = builder.getFemale();
    Pokemon male = builder.getMale();
    Pokemon egg = builder.getEgg();
    Pokemon evo = builder.getFirstEvolution();

    Pokemon source = male.heldItem().getItem().equals(CobblemonItems.EVERSTONE) ? male : female;
    debug("[DayCareForm] applyEgg source={}", source.showdownId());

    LinkedHashSet<String> parts = new LinkedHashSet<>();

    add(parts, getRegionalForm(source), "Regional", source);
    add(parts, processAspects(female), "Aspects", female);

    for (String feature : processFeatures(female)) {
      add(parts, feature, "Feature", female);
    }

    String configForm = getConfigForm(source);
    if (configForm != null) {
      add(parts, configForm, "ConfigForm", source);
    }

    finalizeForm(parts, egg, evo);
  }

  /* ------------------------------------------------------------ */
  /* CREATE EGG                                                   */
  /* ------------------------------------------------------------ */

  @Override
  public void createEgg(ServerPlayerEntity player, Pokemon female, Pokemon egg) {
    Pokemon evo = female;
    debug("[DayCareForm] createEgg pokemon={}", female.showdownId());

    String configForm = getConfigForm(female);
    if (configForm != null) {
      if (!isBlacklisted(configForm)) {
        debug("[DayCareForm][DIRECT APPLY] '{}'", configForm);
        applyForm(egg, configForm, evo);
      } else {
        debug("[DayCareForm][REMOVE] ConfigForm '{}' Pokémon={}", configForm, female.showdownId());
        applyForm(egg, "", evo);
      }
      return;
    }

    LinkedHashSet<String> parts = new LinkedHashSet<>();

    add(parts, getRegionalForm(female), "Regional", female);
    add(parts, processAspects(female), "Aspects", female);

    for (String feature : processFeatures(female)) {
      add(parts, feature, "Feature", female);
    }

    finalizeForm(parts, egg, evo);
  }

  /* ------------------------------------------------------------ */
  /* FINALIZE                                                     */
  /* ------------------------------------------------------------ */

  private void finalizeForm(Set<String> parts, Pokemon egg, Pokemon evo) {
    String form = String.join(" ", parts);
    debug("[DayCareForm] FINAL FORM='{}'", form);
    applyForm(egg, form, evo);
  }

  /* ------------------------------------------------------------ */
  /* ADD HELPER (DEDUP + BLACKLIST)                                */
  /* ------------------------------------------------------------ */

  private void add(Set<String> set, String value, String source, Pokemon pokemon) {
    if (value == null || value.isBlank()) return;

    String v = value.trim();

    if (isBlacklisted(v)) {
      debug("[DayCareForm][REMOVE] {} '{}' Pokémon={}", source, v, pokemon.showdownId());
      return;
    }

    if (!set.add(v)) {
      debug("[DayCareForm][SKIP] DUPLICATE {} '{}' Pokémon={}", source, v, pokemon.showdownId());
      return;
    }

    debug("[DayCareForm][ADD] {} '{}' Pokémon={}", source, v, pokemon.showdownId());
  }

  /* ------------------------------------------------------------ */
  /* SOURCES                                                      */
  /* ------------------------------------------------------------ */

  private String getConfigForm(Pokemon pokemon) {
    for (EggForm eggForm : eggForms) {
      if (eggForm.getPokemons().contains(pokemon.showdownId())) {
        return eggForm.getForm();
      }
    }
    return forms.get(pokemon.getForm().formOnlyShowdownId());
  }

  private String getRegionalForm(Pokemon pokemon) {
    return switch (pokemon.getSpecies().showdownId()) {
      case "perrserker", "sirfetchd", "mrrime", "cursola", "obstagoon", "runerigus" -> "galarian";
      case "clodsire" -> "paldean";
      case "overqwil", "sneasler" -> "hisuian";
      default -> "";
    };
  }

  private String processAspects(Pokemon pokemon) {
    if (pokemon.getForm().getAspects().isEmpty()) return "";

    List<String> parts = new ArrayList<>();

    for (String s : pokemon.getForm().getAspects()) {
      if (s.contains("male") || s.contains("female")) continue;
      parts.add(s.replace("-", "_").replace("_", "="));
    }

    for (String label : pokemon.getForm().getLabels()) {
      if (label.contains("regional") || label.contains("gen8a")) {
        parts.add("region_bias=" + pokemon.getForm().formOnlyShowdownId());
      }
    }

    return String.join(" ", parts);
  }

  private List<String> processFeatures(Pokemon pokemon) {
    List<String> features = new ArrayList<>();

    for (SpeciesFeatureProvider<?> provider : SpeciesFeatures.INSTANCE.getFeaturesFor(pokemon.getSpecies())) {
      if (provider instanceof ChoiceSpeciesFeatureProvider choice) {
        var feature = choice.get(pokemon);
        if (feature == null) continue;

        String entry = feature.getName() + "=" + feature.getValue();
        if (isBlacklisted(feature.getName(), feature.getValue())) {
          debug("[DayCareForm][REMOVE] Feature '{}' Pokémon={}", entry, pokemon.showdownId());
          continue;
        }

        features.add(entry);
      }
    }
    return features;
  }

  /* ------------------------------------------------------------ */
  /* BLACKLIST                                                    */
  /* ------------------------------------------------------------ */

  private boolean isBlacklisted(String... values) {
    for (String v : values) {
      if (blacklistForm.contains(v) || blacklistFeatures.contains(v)) {
        return true;
      }
    }
    return false;
  }

  /* ------------------------------------------------------------ */
  /* APPLY / HATCH                                                */
  /* ------------------------------------------------------------ */

  private void applyForm(Pokemon egg, String form, Pokemon evo) {
    egg.getPersistentData().putString(TAG, form);
    PokemonProperties.Companion.parse(form).apply(evo);
  }

  @Override
  public void applyHatch(HatchBuilder builder) {
    String form = builder.getEgg().getPersistentData().getString(TAG);
    debug("[DayCareForm] applyHatch '{}'", form);

    PokemonProperties.Companion.parse(form).apply(builder.getPokemon());
    CobbleDaycare.fixBreedable(builder.getPokemon());
    builder.getEgg().getPersistentData().remove(TAG);
  }

  /* ------------------------------------------------------------ */

  @Override
  public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%form%", nbt.getString(TAG));
  }

  @Override
  public void validateData() {
    forms.putAll(Map.of(
      "galar", "galarian",
      "paldea", "paldean",
      "hisui", "hisuian",
      "alola", "alolan"
    ));
  }

  @Override public String fileName() {
    return "form";
  }

  @Override public String replace(String text, ServerPlayerEntity player) {
    return text;
  }
}
