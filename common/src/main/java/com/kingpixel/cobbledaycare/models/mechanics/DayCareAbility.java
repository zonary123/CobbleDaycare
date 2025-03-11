package com.kingpixel.cobbledaycare.models.mechanics;

import com.cobblemon.mod.common.api.abilities.Abilities;
import com.cobblemon.mod.common.api.abilities.AbilityTemplate;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCareAbility extends Mechanics {
  public static final String TAG = "ability";

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {

  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String ability = egg.getPersistentData().getString(TAG);
    AbilityTemplate abilityTemplate;
    AbilityTemplate randomAbility =
      PokemonUtils.getRandomAbility(egg).getTemplate();
    if (ability.isEmpty()) {
      abilityTemplate = randomAbility;
    } else {
      abilityTemplate = Abilities.INSTANCE.get(ability);
    }
    if (abilityTemplate == null) abilityTemplate = randomAbility;

    PokemonProperties.Companion.parse(abilityTemplate.getName()).apply(egg);
    egg.getPersistentData().remove(TAG);
  }
}
