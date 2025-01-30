package com.kingpixel.cobbleutils.features.breeding.events;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 28/06/2024 8:45
 */
public interface HatchEggListener {
  void HatchEgg(ServerPlayerEntity player, Pokemon pokemon);
}
