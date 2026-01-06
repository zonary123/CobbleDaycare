package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:03
 */
public class CommandSteps {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("steps")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare_datapack.admin", "cobbledaycare_datapack" +
              ".reload"), 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .then(
                  CommandManager.argument("amount", IntegerArgumentType.integer(1))
                    .executes(context -> {
                      var players = EntityArgumentType.getPlayers(context, "player");
                      int amount = IntegerArgumentType.getInteger(context, "amount");
                      for (var player : players) {
                        var party = Cobblemon.INSTANCE.getStorage().getParty(player);
                        for (Pokemon pokemon : party) {
                          if (pokemon.showdownId().equals("egg")) {
                            EggData.steps(player, pokemon, amount,
                              DatabaseClientFactory.INSTANCE.getUserInformation(player));
                          }
                        }
                      }
                      return 1;
                    })
                )
            )
        )
    );

  }
}
