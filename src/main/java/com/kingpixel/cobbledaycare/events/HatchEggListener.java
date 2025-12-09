package com.kingpixel.cobbledaycare.events;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.server.network.ServerPlayerEntity;

public interface HatchEggListener {
  void onHatchEgg(ServerPlayerEntity player, Pokemon pokemon);
}
