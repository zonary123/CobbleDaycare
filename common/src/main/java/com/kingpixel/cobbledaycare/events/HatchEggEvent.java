package com.kingpixel.cobbledaycare.events;

import com.cobblemon.mod.common.pokemon.Pokemon;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 28/06/2024 8:43
 */
@Data
public class HatchEggEvent {
  public static final HatchEggEvent HATCH_EGG_EVENT = new HatchEggEvent();
  private final List<HatchEggListener> hatchEggListeners = new ArrayList<>();

  public void subscribe(HatchEggListener listener) {
    hatchEggListeners.add(listener);
  }

  public void unsubscribe(HatchEggListener listener) {
    hatchEggListeners.remove(listener);
  }

  public void emit(ServerPlayerEntity player, Pokemon pokemon) {
    for (HatchEggListener listener : hatchEggListeners) {
      listener.onHatchEgg(player, pokemon);
    }
  }


  public void clear() {
    hatchEggListeners.clear();
  }
}
