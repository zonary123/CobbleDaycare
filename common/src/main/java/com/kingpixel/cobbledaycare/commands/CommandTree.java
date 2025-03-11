package com.kingpixel.cobbledaycare.commands;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:12
 */
public class CommandTree {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registry) {
    for (String command : CobbleDaycare.config.getCommands()) {
      var base = CommandManager.literal(command)
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.user", "cobbledaycare.admin"),
          4));

      dispatcher.register(base
        .executes(context -> {
          if (context.getSource().isExecutedByPlayer()) {
            ServerPlayerEntity player = context.getSource().getPlayer();
            CobbleDaycare.language.getPrincipalMenu().open(player);
          }
          return 1;
        }).then(
          CommandManager.literal("other")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare" +
              ".other"), 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.player())

                .executes(context -> {
                  ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                  CobbleDaycare.language.getPrincipalMenu().open(player);
                  return 1;
                })
            )
        ).then(
          CommandManager.literal("reload")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare" +
              ".reload"), 4))
            .executes(context -> {
              CobbleDaycare.init();
              return 1;
            })
        )
      );
    }

    if (!CobbleDaycare.config.getCommandEggInfo().isEmpty()) {
      dispatcher.register(
        CommandManager.literal(CobbleDaycare.config.getCommandEggInfo())
          .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.user", "cobbledaycare.admin"),
            4))
          .then(
            CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
              .executes(context -> {
                if (context.getSource().isExecutedByPlayer()) {
                  ServerPlayerEntity player = context.getSource().getPlayer();
                  Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                  if (pokemon.getSpecies().showdownId().equals("egg")) {
                    EggData.from(pokemon).sendEggInfo(player);
                  } else {

                  }
                }
                return 1;
              })
          )
      );
    }
  }
}
