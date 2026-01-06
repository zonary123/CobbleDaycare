package com.kingpixel.cobbledaycare.commands.base;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.CobbleUtils;
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
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare_datapack.egginfo.base", "cobbledaycare_datapack" +
            ".admin"),
          4))
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {

              if (context.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = context.getSource().getPlayer();
                Pokemon egg = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                if (egg.getSpecies().showdownId().equals("egg")) {
                  String message = CobbleDaycare.language.getEggInfo();
                  var nbt = egg.getPersistentData();
                  for (Mechanics mechanic : CobbleDaycare.mechanics) {
                    try {
                      message = mechanic.getEggInfo(message, nbt);
                    } catch (Exception e) {
                      CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID,
                        "Error in egg info: " + mechanic.getClass().getSimpleName());
                      e.printStackTrace();
                    }
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

            })
        )
    );
  }
}
