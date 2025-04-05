package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:06
 */
public class CommandBreedable {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("breedable")
            .requires(
              source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare.breedable"), 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .then(
                  CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
                    .then(
                      CommandManager.argument("breedable", BoolArgumentType.bool())
                        .executes(context -> {
                          var players = EntityArgumentType.getPlayers(context, "player");
                          boolean breedable = BoolArgumentType.getBool(context, "breedable");
                          for (ServerPlayerEntity player : players) {
                            Pokemon pokemon = PartySlotArgumentType.Companion.getPokemonOf(context, "slot", player);
                            if (pokemon == null) continue;
                            CobbleDaycare.setBreedable(pokemon, breedable);
                            pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !breedable);
                          }
                          return 1;
                        })
                    )
                )
            )
        )
    );
  }
}
