package com.kingpixel.cobblestop.neoforge;

import com.kingpixel.cobblestop.CobbleStop;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CobbleStop.MOD_ID)
public class CobbleStopNeoForge {

    public CobbleStopNeoForge(IEventBus modBus) {
        CobbleStop.init();
    }
}
