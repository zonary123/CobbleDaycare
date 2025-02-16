package com.kingpixel.cobbledaycare.fabric;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import net.fabricmc.api.ModInitializer;

public class CobbleDaycareFabric implements ModInitializer {
  @Override
  public void onInitialize() {
    CobbleDaycare.init();
  }
}
