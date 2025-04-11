package com.kingpixel.cobbledaycare.commands.base;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:07
 */
public class CommandEggInfo {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.egginfo.base", "cobbledaycare" +
            ".admin"),
          4))
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {
              try {
                if (context.getSource().isExecutedByPlayer()) {
                  ServerPlayerEntity player = context.getSource().getPlayer();
                  Pokemon egg = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                  if (egg.getSpecies().showdownId().equals("egg")) {
                    String message = CobbleDaycare.language.getEggInfo();
                    var nbt = egg.getPersistentData();
                    for (Mechanics mechanic : CobbleDaycare.mechanics) {
                      message = mechanic.getEggInfo(message, nbt);
                    }
                    PlayerUtils.sendMessage(
                      player,
                      message,
                      CobbleDaycare.language.getPrefix(),
                      TypeMessage.CHAT
                    );
                  } else {
                    PlayerUtils.sendMessage(
                      player,
                      CobbleDaycare.language.getMessageItNotEgg(),
                      CobbleDaycare.language.getPrefix(),
                      TypeMessage.CHAT
                    );
                  }
                }
                return 1;
              } catch (Exception e) {
                e.printStackTrace();
                return 0;
              }
            })
        )
    );
  }
}
