package com.kingpixel.cobbleutils.features.breeding.events;

import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Getter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 28/06/2024 8:43
 */
@Getter
public class HatchEggEvent {
  private List<HatchEggListener> hatchEggListeners = new ArrayList<>();
  public static final HatchEggEvent HATCH_EGG_EVENT = new HatchEggEvent();

  public void register(HatchEggListener listener) {
    hatchEggListeners.add(listener);
  }

  public void unregister(HatchEggListener listener) {
    hatchEggListeners.remove(listener);
  }

  public void emit(ServerPlayerEntity player, Pokemon pokemon) {
    notifyHatchEgg(player, pokemon);
  }

  private void notifyHatchEgg(ServerPlayerEntity player, Pokemon pokemon) {
    for (HatchEggListener listener : hatchEggListeners) {
      listener.HatchEgg(player, pokemon);
    }
  }

  public void clear() {
    hatchEggListeners.clear();
  }
}
