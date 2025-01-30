package com.kingpixel.cobblestop.fabric;

import com.kingpixel.cobblestop.CobbleStop;
import net.fabricmc.api.ModInitializer;

public class CobbleStopFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleStop.init();
  }
}
