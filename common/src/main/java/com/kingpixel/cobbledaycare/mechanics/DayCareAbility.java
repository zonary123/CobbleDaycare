package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Pokemon;
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
    boolean dittoTransmitHA = this.dittoTransmitHA && ((maleIsDitto && maleHA) || (femaleIsDitto && femaleHA));
    boolean hasAh = maleHA || femaleHA;
    boolean getAh = Utils.RANDOM.nextDouble(100) <= percentageTransmitHA;
    boolean notDitto = isDitto(male) || isDitto(female);
    boolean giveHA = false;
    if (getAh && hasAh) {
      if (dittoTransmitHA || !notDitto) giveHA = true;
    }
    builder.getEgg().getPersistentData().putBoolean(TAG, giveHA);
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    boolean isHA = egg.getPersistentData().getBoolean(TAG);
    if (isHA) {
      PokemonProperties.Companion.parse("hiddenability=yes").apply(egg);
    } else {
      PokemonProperties.Companion.parse("hiddenability=no").apply(egg);
    }
    egg.getPersistentData().remove(TAG);
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
    String ability = PokemonUtils.getAH(pokemon).getName();
    egg.getPersistentData().putString(TAG, ability);
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
