package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareAbility extends Mechanics {
  public static final String TAG = "ability";
  private double percentageTransmitHA;
  private boolean dittoTransmitHA;

  public DayCareAbility() {
    this.percentageTransmitHA = 60;
    this.dittoTransmitHA = false;
  }

  private static boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  @Override public String replace(String text) {
    return text
      .replace("%ability%", String.format("%.2f", percentageTransmitHA))
      .replace("%activeAbility%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon male = builder.getMale();
    Pokemon female = builder.getFemale();
    boolean maleHA = PokemonUtils.isAH(male);
    boolean femaleHA = PokemonUtils.isAH(female);
    boolean maleIsDitto = isDitto(male);
    boolean femaleIsDitto = isDitto(female);

    boolean hasHA = maleHA || femaleHA;
    boolean getHA = Utils.RANDOM.nextDouble(100) <= percentageTransmitHA;
    boolean giveHA = false;

    if (getHA && hasHA) {
      if (maleIsDitto ^ femaleIsDitto) {
        boolean dittoHA = maleIsDitto ? maleHA : femaleHA;
        boolean nonDittoHA = maleIsDitto ? femaleHA : maleHA;
        giveHA = this.dittoTransmitHA ? dittoHA : nonDittoHA;
      } else if (male.getSpecies().showdownId().equals(female.getSpecies().showdownId())) {
        giveHA = true;
      } else {
        giveHA = femaleHA;
      }
    }

    builder.getEgg().getPersistentData().putBoolean(TAG, giveHA);
  }


  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    boolean isHA = egg.getPersistentData().getBoolean(TAG);
    Ability ability;
    if (isHA)
      ability = getHa(egg);
    else
      ability = PokemonUtils.getRandomAbility(egg);

    String s = "";
    if (ability != null) {
      s = "ability=" + ability.getName();
    }
    PokemonProperties.Companion.parse(s).apply(egg);
    egg.getPersistentData().remove(TAG);
  }

  private Ability getHa(Pokemon pokemon) {
    if (pokemon == null) return null;
    for (PotentialAbility ability : pokemon.getForm().getAbilities()) {
      if (ability.getType() instanceof HiddenAbilityType) {
        return ability.getTemplate().create(false, Priority.NORMAL);
      }
    }
    return null;
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    boolean isHA = PokemonUtils.isAH(pokemon);
    egg.getPersistentData().putBoolean(TAG, isHA);
  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s.replace("%ability%",
      nbt.getBoolean(TAG) ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "ability";
  }
}
