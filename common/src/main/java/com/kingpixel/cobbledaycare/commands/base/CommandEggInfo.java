package com.kingpixel.cobbledaycare.commands.base;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:07
 */
public class CommandEggInfo {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {
              if (context.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = context.getSource().getPlayer();
                Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                if (pokemon.getSpecies().showdownId().equals("egg")) {
                  EggData.from(pokemon).sendEggInfo(player);
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
            })
        )
    );
  }
}
