package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.abilities.Ability;
import com.cobblemon.mod.common.api.abilities.PotentialAbility;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.abilities.HiddenAbilityType;
import com.kingpixel.cobbledaycare.CobbleDaycare;
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
  public static final String TAG_HA = "ha";
  private double percentageTransmitHAFemale;
  private double percentageTransmitHAMale;
  private boolean dittoTransmitHA;
  private boolean eggGroupTransmitHA;

  public DayCareAbility() {
    this.percentageTransmitHAFemale = 60;
    this.percentageTransmitHAMale = 40;
    this.dittoTransmitHA = false;
    this.eggGroupTransmitHA = false;
  }

  private static boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  @Override public String replace(String text, ServerPlayerEntity player) {
    String femaleHA = String.format("%.2f", percentageTransmitHAFemale);
    String maleHA = String.format("%.2f", percentageTransmitHAMale);
    String maleGender = PokemonUtils.getGenderTranslate(Gender.MALE);
    String femaleGender = PokemonUtils.getGenderTranslate(Gender.FEMALE);

    String ability = CobbleDaycare.language.getInfoAbility()
      .replace("%female%", femaleGender)
      .replace("%male%", maleGender)
      .replace("%femaleHA%", femaleHA)
      .replace("%maleHA%", maleHA);
    return text
      .replace("%ability%", ability)
      .replace("%activeAbility%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo())
      .replace("%activeDitto%", dittoTransmitHA ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
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
    boolean giveHA = false;

    if (hasHA) {
      if (maleIsDitto ^ femaleIsDitto) {
        boolean nonDittoHA = maleIsDitto ? femaleHA : maleHA;
        giveHA = this.dittoTransmitHA || nonDittoHA;
      } else {
        if (eggGroupTransmitHA) {
          giveHA = true;
        } else if (male.getSpecies().equals(female.getSpecies())) {
          if (CobbleDaycare.config.isDebug()) CobbleUtils.LOGGER.info("Same Species");
          giveHA = true;
        }
      }
    }

    boolean result = false;

    if (giveHA) {
      if (femaleHA) {
        result = Utils.RANDOM.nextDouble() < percentageTransmitHAFemale / 100;
      } else {
        result = Utils.RANDOM.nextDouble() < percentageTransmitHAMale / 100;
      }
    }

    Ability ability;
    if (result)
      ability = getHa(builder.getFirstEvolution());
    else
      ability = PokemonUtils.getRandomAbility(builder.getFirstEvolution());

    builder.getEgg().getPersistentData().putString(TAG, ability.getName());
    builder.getEgg().getPersistentData().putBoolean(TAG_HA, result);
  }


  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String s = egg.getPersistentData().getString(TAG);
    boolean isHA = egg.getPersistentData().getBoolean(TAG_HA);

    if (s.isEmpty()) {
      if (isHA) {
        s = getHa(egg).getName();
      } else {
        s = PokemonUtils.getRandomAbility(egg).getName();
      }
    }
    PokemonProperties.Companion.parse("ability=" + s).apply(egg);
    egg.getPersistentData().remove(TAG);
    egg.getPersistentData().remove(TAG_HA);
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
    egg.getPersistentData().putString(TAG, pokemon.getAbility().getName());
    egg.getPersistentData().putBoolean(TAG_HA, isHA);
  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s
      .replace("%ability%", "<lang:cobblemon.ability." + nbt.getString(TAG) + ">")
      .replace("%ha%", nbt.getBoolean(TAG_HA) ? CobbleUtils.language.getAH() : "");
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "ability";
  }
}
