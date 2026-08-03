package com.kingpixel.ultradaycare.mechanics.pokemon;

import com.cobblemon.mod.common.CobblemonItems;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.kingpixel.ultradaycare.UltraDaycare;
import com.kingpixel.ultradaycare.mechanics.Mechanics;
import com.kingpixel.ultradaycare.models.EggBuilder;
import com.kingpixel.ultradaycare.models.EggForm;
import com.kingpixel.ultradaycare.models.HatchBuilder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
public class DayCareForm extends Mechanics {

  public static final String TAG = "form";
  private static final String REGION_BIAS_PREFIX = "region_bias=";

  private final Map<String, String> forms = new HashMap<>();
  private final List<EggForm> eggForms = new ArrayList<>();
  private final Set<String> blacklistForm = new HashSet<>();
  private final Set<String> blacklistFeatures = new HashSet<>();

  public DayCareForm() {
    validateData();

    if (eggForms.isEmpty()) {
      eggForms.addAll(List.of(
        new EggForm("galarian", List.of("perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon")),
        new EggForm("paldean", List.of("clodsire")),
        new EggForm("hisuian", List.of("overqwil", "sneasler")),
        new EggForm("white-striped", List.of("basculegion"))
      ));
    }

    if (blacklistForm.isEmpty()) {
      blacklistForm.addAll(List.of("halloween", "disguised"));
    }
    if (blacklistFeatures.isEmpty()) {
      blacklistFeatures.addAll(List.of("netherite_coating", "disguised"));
    }
  }

  public FormData determineChildForm(FormData parentForm) {
    if (parentForm == null) return null;
    Species baseSpecies = parentForm.getSpecies();

    while (baseSpecies.getPreEvolution() != null) {
      Species pre = baseSpecies.getPreEvolution().getSpecies();
      if (pre.showdownId().equalsIgnoreCase(baseSpecies.showdownId())) {
        break;
      }
      baseSpecies = pre;
    }

    String pId = parentForm.getSpecies().showdownId().replace("'", "").replace("_", "").replace("-", "").toLowerCase();
    String pName = parentForm.getSpecies().getName().replace("'", "").replace("_", "").replace("-", "").toLowerCase();

    for (EggForm eggForm : eggForms) {
      boolean matched = false;
      for (String p : eggForm.getPokemons()) {
        String normP = p.replace("'", "").replace("_", "").replace("-", "").toLowerCase();
        if (normP.equalsIgnoreCase(pId) || normP.equalsIgnoreCase(pName)) {
          matched = true;
          break;
        }
      }

      if (matched) {
        String fTarget = eggForm.getForm().replace("-", "").replace("_", "").toLowerCase();
        for (FormData f : baseSpecies.getForms()) {
          String childNorm = f.formOnlyShowdownId().replace("-", "").replace("_", "").toLowerCase();
          if (childNorm.equals(fTarget) || childNorm.contains(fTarget)) {
            return f;
          }
        }
      }
    }

    String parentFormId = parentForm.formOnlyShowdownId();
    if (!parentFormId.isEmpty()) {
      String parentNorm = parentFormId.replace("-", "").replace("_", "").toLowerCase();
      for (FormData f : baseSpecies.getForms()) {
        String childNorm = f.formOnlyShowdownId().replace("-", "").replace("_", "").toLowerCase();
        if (!childNorm.isEmpty() && (childNorm.contains(parentNorm) || parentNorm.contains(childNorm))) {
          return f;
        }
      }
    }

    return baseSpecies.getStandardForm();
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

    boolean femaleIsDitto = female != null && female.getForm().getEggGroups().contains(EggGroup.DITTO);
    boolean maleIsDitto = male != null && male.getForm().getEggGroups().contains(EggGroup.DITTO);

    Pokemon source = female;
    if (male != null && !maleIsDitto && male.heldItem().getItem().equals(CobblemonItems.EVERSTONE)) {
      source = male;
    } else if (female != null && !femaleIsDitto && female.heldItem().getItem().equals(CobblemonItems.EVERSTONE)) {
      source = female;
    } else if (femaleIsDitto) {
      source = male;
    } else if (female == null) {
      source = male;
    }

    if (source == null || (femaleIsDitto && maleIsDitto)) return;
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
    String configForm = getConfigForm(source);
    if (configForm != null) {
      if (!isBlacklisted(configForm)) {
        applyForm(egg, configForm, evo);
      } else {
        applyForm(egg, "", evo);
      }
      return;
    }

    var props = source.createPokemonProperties(
      PokemonPropertyExtractor.FORM,
      PokemonPropertyExtractor.ASPECTS
    );

    if (props.getForm() != null && blacklistForm.contains(props.getForm())) {
      props.setForm("");
    }
    Set<String> aspects = new HashSet<>(props.getAspects());
    aspects.removeIf(aspect -> {
      if (aspect.startsWith("gender=") || aspect.startsWith("shiny=")) return true;
      for (String blacklisted : blacklistFeatures) {
        if (aspect.contains(blacklisted)) return true;
      }
      return false;
    });
    props.setAspects(aspects);

    String formStr = props.asString(" ");
    applyForm(egg, formStr, evo);
  }

  /* ------------------------------------------------------------ */
  /* SOURCES                                                      */
  /* ------------------------------------------------------------ */

  private String getConfigForm(Pokemon pokemon) {
    if (pokemon == null) return null;
    String pId = pokemon.showdownId().replace("'", "").replace("_", "").replace("-", "").toLowerCase();
    String pName = pokemon.getSpecies().getName().replace("'", "").replace("_", "").replace("-", "").toLowerCase();

    for (EggForm eggForm : eggForms) {
      for (String p : eggForm.getPokemons()) {
        String normP = p.replace("'", "").replace("_", "").replace("-", "").toLowerCase();
        if (normP.equalsIgnoreCase(pId) || normP.equalsIgnoreCase(pName)) {
          return eggForm.getForm();
        }
      }
    }
    String formId = pokemon.getForm().formOnlyShowdownId();
    if (formId.isEmpty()) return null;

    String mappedForm = forms.get(formId.toLowerCase());
    return mappedForm != null ? mappedForm : formId;
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

  private String resolveFormName(Pokemon pokemon, String form) {
    if (pokemon == null || form == null || form.trim().isEmpty()) {
      return null;
    }
    String target = form.trim().toLowerCase();
    if (target.contains("=") || target.contains(" ")) {
      return target;
    }

    for (FormData f : pokemon.getSpecies().getForms()) {
      String fName = f.getName().toLowerCase();
      if (fName.equalsIgnoreCase(target) || fName.equalsIgnoreCase(forms.getOrDefault(target, target))) {
        return f.getName();
      }
    }

    String mapped = forms.get(target);
    if (mapped != null) {
      for (FormData f : pokemon.getSpecies().getForms()) {
        if (f.getName().equalsIgnoreCase(mapped)) {
          return f.getName();
        }
      }
    }

    return null;
  }

  private void applyForm(Pokemon egg, String form, Pokemon evo) {
    if (egg == null || form == null || form.isEmpty() || evo == null) return;

    String cleanForm = resolveFormName(evo, form);
    if (cleanForm == null) {
      cleanForm = form;
    }

    egg.getPersistentData().putString(TAG, cleanForm);
    applyFormToPokemon(evo, form);
  }

  private String formToRegionBias(String form) {
    if (form == null) return null;
    String clean = form.toLowerCase().trim();

    if (clean.contains("alola") || clean.contains("alolan")) return "alola";
    if (clean.contains("hisui") || clean.contains("hisuian")) return "hisui";
    if (clean.contains("galar") || clean.contains("galarian")) return "galar";
    if (clean.contains("paldea") || clean.contains("paldean")) return "paldea";
    return null;
  }

  private void applyFormToPokemon(Pokemon pokemon, String form) {
    if (pokemon == null || form == null || form.trim().isEmpty()) return;

    String cleanForm = form.trim();
    debug("[DayCareForm] applyFormToPokemon processing '{}' on {}", cleanForm, pokemon.showdownId());

    // 1. ALWAYS apply region_bias aspect if a region bias is detected (alola, hisui, galar, paldea)
    String regionBias = formToRegionBias(cleanForm);
    if (regionBias != null) {
      try {
        PokemonProperties.Companion.parse(REGION_BIAS_PREFIX + regionBias).apply(pokemon);
        debug("[DayCareForm] Applied region_bias='{}' to {}", regionBias, pokemon.showdownId());
      } catch (Exception e) {
        debug("[DayCareForm] Error applying region_bias='{}' to {}: {}", regionBias, pokemon.showdownId(), e.getMessage());
      }
    }

    // 2. Explicit Basculin / Basculegion property parser for striped=white / striped=blue / striped=red
    if (pokemon.getSpecies().showdownId().equalsIgnoreCase("basculin") ||
        pokemon.getSpecies().showdownId().equalsIgnoreCase("basculegion")) {
      String stripColor = null;
      if (cleanForm.contains("white")) stripColor = "white";
      else if (cleanForm.contains("blue")) stripColor = "blue";
      else if (cleanForm.contains("red")) stripColor = "red";

      if (stripColor != null) {
        try {
          PokemonProperties.Companion.parse("striped=" + stripColor).apply(pokemon);
          debug("[DayCareForm] Applied explicit Basculin property 'striped={}' to {}", stripColor, pokemon.showdownId());
        } catch (Exception e) {
          debug("[DayCareForm] Error applying explicit Basculin 'striped={}': {}", stripColor, e.getMessage());
        }
      }
    }

    // 2. Try parsing the complete property string first
    try {
      PokemonProperties.Companion.parse(cleanForm).apply(pokemon);
      debug("[DayCareForm] Applied complete property string '{}' to {}", cleanForm, pokemon.showdownId());
    } catch (Exception ignored) {}

    // 3. Process every space-separated token option
    String[] parts = cleanForm.split(" ");
    for (String part : parts) {
      String token = part.trim();
      if (token.isEmpty()) continue;

      if (token.startsWith("form=")) {
        token = token.substring("form=".length());
      }

      String alias = forms.getOrDefault(token.toLowerCase(), token);

      String normTarget = token.replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
      String searchTarget = normTarget.startsWith("striped=") ? normTarget.substring("striped=".length()) : normTarget;

      // Try native setForm match on each form option
      for (FormData f : pokemon.getSpecies().getForms()) {
        String childNorm = f.formOnlyShowdownId().replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
        String nameNorm = f.getName().replace("-", "").replace("_", "").replace(" ", "").toLowerCase();
        List<String> formAspects = f.getAspects();

        if ((!childNorm.isEmpty() && (childNorm.equals(normTarget) || childNorm.contains(searchTarget))) ||
            (!nameNorm.isEmpty() && (nameNorm.equals(normTarget) || nameNorm.contains(searchTarget))) ||
            formAspects.contains(searchTarget) || formAspects.contains(searchTarget + "striped") ||
            formAspects.contains("white" + searchTarget) || formAspects.contains("blue" + searchTarget) ||
            childNorm.equalsIgnoreCase(alias.toLowerCase()) || nameNorm.equalsIgnoreCase(alias.toLowerCase())) {
          pokemon.setForm(f);
          for (String aspect : f.getAspects()) {
            try {
              PokemonProperties.Companion.parse(aspect).apply(pokemon);
            } catch (Exception ignored) {}
          }
          if (searchTarget.equalsIgnoreCase("white") || searchTarget.equalsIgnoreCase("blue") || searchTarget.equalsIgnoreCase("red")) {
            try {
              PokemonProperties.Companion.parse("striped=" + searchTarget).apply(pokemon);
            } catch (Exception ignored) {}
            try {
              PokemonProperties.Companion.parse(searchTarget + "striped").apply(pokemon);
            } catch (Exception ignored) {}
          }
          debug("[DayCareForm] Set native form '{}' and aspects on {}", f.formOnlyShowdownId(), pokemon.showdownId());
        }
      }

      // Try parsing token directly
      try {
        PokemonProperties.Companion.parse(token).apply(pokemon);
      } catch (Exception ignored) {}

      // Try parsing token with alias
      if (!alias.equalsIgnoreCase(token)) {
        try {
          PokemonProperties.Companion.parse(alias).apply(pokemon);
        } catch (Exception ignored) {}
      }
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

  private void ensureEggForm(String form, String... pokemons) {
    for (EggForm ef : eggForms) {
      if (ef.getForm() != null && ef.getForm().equalsIgnoreCase(form)) {
        if (ef.getPokemons() == null) {
          ef.setPokemons(new ArrayList<>());
        }
        for (String p : pokemons) {
          if (!ef.getPokemons().contains(p)) {
            ef.getPokemons().add(p);
          }
        }
        return;
      }
    }
    eggForms.add(new EggForm(form, new ArrayList<>(List.of(pokemons))));
  }

  @Override
  public void validateData() {
    forms.putIfAbsent("galar", "galarian");
    forms.putIfAbsent("paldea", "paldean");
    forms.putIfAbsent("hisui", "hisuian");
    forms.putIfAbsent("alola", "alolan");
    forms.putIfAbsent("white-striped", "striped=white");
    forms.putIfAbsent("blue-striped", "striped=blue");
    forms.putIfAbsent("red-striped", "striped=red");
    forms.putIfAbsent("striped=white", "white-striped");
    forms.putIfAbsent("striped=blue", "blue-striped");
    forms.putIfAbsent("striped=red", "red-striped");

    ensureEggForm("galarian", "perrserker", "sirfetchd", "mrrime", "cursola", "runerigus", "obstagoon");
    ensureEggForm("paldean", "clodsire");
    ensureEggForm("hisuian", "overqwil", "sneasler");
    ensureEggForm("white-striped", "basculegion");

    blacklistForm.add("halloween");
    blacklistForm.add("disguised");

    blacklistFeatures.add("netherite_coating");
    blacklistFeatures.add("disguised");
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
