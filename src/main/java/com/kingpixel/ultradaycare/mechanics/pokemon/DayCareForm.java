package com.kingpixel.ultradaycare.mechanics.pokemon;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.mechanics.Mechanics;
import com.kingpixel.ultradaycare.models.EggBuilder;
import com.kingpixel.ultradaycare.models.HatchBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@EqualsAndHashCode(callSuper = true)
@Data
public class DayCareForm extends Mechanics {

  public static final String TAG = "form";

  private final Set<String> blacklistForm = new HashSet<>();
  private final Set<String> blacklistFeatures = new HashSet<>();

  public DayCareForm() {
    blacklistForm.addAll(List.of("halloween", "disguised"));
    blacklistFeatures.addAll(List.of("netherite_coating", "disguised"));
  }

  /* ------------------------------------------------------------ */
  /* DEBUG                                                        */
  /* ------------------------------------------------------------ */

  private void debug(String msg, Object... args) {
    if (UltraDaycare.config.isDebug()) {
      UltraDaycare.LOGGER.info(msg, args);
    }
  }

  /* ------------------------------------------------------------ */
  /* APPLY EGG                                                    */
  /* ------------------------------------------------------------ */

  @Override
  public void applyEgg(EggBuilder builder) {
    if (builder == null || builder.getEgg() == null) return;
    Pokemon female = builder.getFemale();
    Pokemon male = builder.getMale();
    Pokemon egg = builder.getEgg();
    Pokemon evo = builder.getFirstEvolution();

    if (female == null && male == null) return;
    Pokemon source = female;
    if (male != null && male.heldItem().getItem().equals(CobblemonItems.EVERSTONE)) {
      source = male;
    } else if (female != null && female.heldItem().getItem().equals(CobblemonItems.EVERSTONE)) {
      source = female;
    } else if (female == null) {
      source = male;
    }

    if (source == null) return;
    debug("[DayCareForm] applyEgg source={}", source.showdownId());
    processSourceForm(source, egg, evo);
  }

  @Override
  public void createEgg(ServerPlayerEntity player, Pokemon female, Pokemon egg) {
    if (female == null || egg == null) return;
    Pokemon evo = female;
    debug("[DayCareForm] createEgg pokemon={}", female.showdownId());
    processSourceForm(female, egg, evo);
  }

  private void processSourceForm(Pokemon source, Pokemon egg, Pokemon evo) {
    FormData childForm = DayCarePokemon.determineChildForm(source.getForm());
    String targetFormId = (childForm != null) ? childForm.formOnlyShowdownId() : source.getForm().formOnlyShowdownId();

    if (isBlacklisted(targetFormId)) {
      targetFormId = "";
    }

    var props = source.createPokemonProperties(
      PokemonPropertyExtractor.ASPECTS
    );

    Set<String> aspects = new HashSet<>(props.getAspects());
    aspects.removeIf(aspect -> {
      if (aspect.startsWith("gender=") || aspect.startsWith("shiny=")) return true;
      for (String blacklisted : blacklistFeatures) {
        if (aspect.contains(blacklisted)) return true;
      }
      return false;
    });

    StringBuilder formPropBuilder = new StringBuilder();
    if (!targetFormId.isEmpty()) {
      formPropBuilder.append("form=").append(targetFormId);
    }
    for (String aspect : aspects) {
      if (formPropBuilder.length() > 0) formPropBuilder.append(" ");
      formPropBuilder.append(aspect);
    }

    String formStr = formPropBuilder.toString();
    applyForm(egg, formStr, evo);
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
    if (egg == null || form == null || form.isEmpty() || evo == null) return;

    egg.getPersistentData().putString(TAG, form);
    applyFormToPokemon(evo, form);
  }

  private void applyFormToPokemon(Pokemon pokemon, String form) {
    if (pokemon == null || form == null || form.trim().isEmpty()) return;

    try {
      PokemonProperties.Companion.parse(form).apply(pokemon);
      debug("[DayCareForm] Applied properties '{}' to {}", form, pokemon.showdownId());
    } catch (Exception e) {
      debug("[DayCareForm] Error applying properties '{}' to {}: {}", form, pokemon.showdownId(), e.getMessage());
    }
  }

  @Override
  public void applyHatch(HatchBuilder builder) {
    if (builder == null || builder.getEgg() == null || builder.getPokemon() == null) return;
    String form = builder.getEgg().getPersistentData().getString(TAG);
    debug("[DayCareForm] applyHatch '{}'", form);

    if (form != null && !form.isEmpty()) {
      applyFormToPokemon(builder.getPokemon(), form);
    }

    UltraDaycare.fixBreedable(builder.getPokemon());
    builder.getEgg().getPersistentData().remove(TAG);
  }

  /* ------------------------------------------------------------ */

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
