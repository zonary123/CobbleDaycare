package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.command.argument.PokemonPropertiesArgumentType;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:19
 */
public class CommandEgg {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("egg")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare" +
              ".egg"), 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .then(
                  CommandManager.argument("pokemon", PokemonPropertiesArgumentType.Companion.properties())
                    .executes(context -> {
                        var players = EntityArgumentType.getPlayers(context, "player");
                        var pokemon =
                          PokemonPropertiesArgumentType.Companion.getPokemonProperties(context, "pokemon").create();
                        var egg = PokemonProperties.Companion.parse("egg").create();

                        for (var player : players) {
                          for (Mechanics mechanic : CobbleDaycare.mechanics) {
                            mechanic.createEgg(player, pokemon, egg);
                          }
                          Cobblemon.INSTANCE.getStorage().getParty(player).add(egg);
                        }
                        return 1;
                      }
                    )
                )
            )
        )
    );

  }
}
