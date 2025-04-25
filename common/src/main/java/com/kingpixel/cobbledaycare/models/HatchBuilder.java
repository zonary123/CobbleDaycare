package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Builder;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 23/04/2025 7:15
 */
@Data
@Builder
public class HatchBuilder {
  private ServerPlayerEntity player;
  private Pokemon egg;


}
