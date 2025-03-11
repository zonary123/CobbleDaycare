package com.kingpixel.cobbledaycare.models.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:25
 */
public abstract class Mechanics {

  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Not Implemented -> " + this.getClass().getName());
  }

  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Not Implemented -> " + this.getClass().getName());
  }
}
