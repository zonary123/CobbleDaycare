package com.kingpixel.cobbledaycare.neoforge;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CobbleDaycare.MOD_ID)
public class CobbleDaycareNeoForge {

    public CobbleDaycareNeoForge(IEventBus modBus) {
        CobbleDaycare.init();
    }
}
