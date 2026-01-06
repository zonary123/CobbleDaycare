package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:07
 */
public class CommandHatch {
  private static final String HATCH_ALL_TYPE = "all";
  private static final String HATCH_SLOT_TYPE = "slot";

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.base", "cobbledaycare" +
            ".admin"),
          4))
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {
              var player = context.getSource().getPlayer();
              hatch(HATCH_SLOT_TYPE, context, player, false);
              return 1;
            }).then(
              CommandManager.argument("player", EntityArgumentType.players())
                .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.other",
                  "cobbledaycare.admin"), 4))
                .executes(context -> {
                  var players = EntityArgumentType.getPlayers(context, "player");
                  for (ServerPlayerEntity player : players) {
                    hatch(HATCH_SLOT_TYPE, context, player, false);
                  }
                  return 1;
                })
            )
        ).then(
          CommandManager.literal("all")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.all",
              "cobbledaycare.admin"), 4))
            .executes(context -> {
              var player = context.getSource().getPlayer();
              if (player == null) return 1;
              hatch(HATCH_ALL_TYPE, context, player, false);
              return 1;
            })
            .then(
              CommandManager.argument("player", EntityArgumentType.players())
                .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.other",
                  "cobbledaycare.admin"), 4))
                .executes(context -> {
                  var players = EntityArgumentType.getPlayers(context, "player");
                  for (ServerPlayerEntity player : players) {
                    if (player == null) continue;
                    hatch(HATCH_ALL_TYPE, context, player, false);
                  }
                  return 1;
                }).then(
                  CommandManager.argument("bypassCooldown", StringArgumentType.string())
                    .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.bypasscooldown",
                      "cobbledaycare.admin"), 4))
                    .suggests((context, builder) -> {
                      builder.suggest("true");
                      builder.suggest("false");
                      return builder.buildFuture();
                    })
                    .executes(context -> {
                      var players = EntityArgumentType.getPlayers(context, "player");
                      var bypassCooldown = StringArgumentType.getString(context, "bypassCooldown")
                        .equalsIgnoreCase("true");
                      for (ServerPlayerEntity player : players) {
                        if (player == null) continue;
                        hatch(HATCH_ALL_TYPE, context, player, bypassCooldown);
                      }
                      return 1;
                    })
                )
            )
        )
    );
  }

  private static void hatch(String TYPE_HATCH, CommandContext<ServerCommandSource> context, ServerPlayerEntity player, boolean byPassCooldown) {
    List<Pokemon> pokemons = new ArrayList<>();
    if (TYPE_HATCH.equals(HATCH_ALL_TYPE)) {
      for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
        pokemons.add(pokemon);
      }
      if (pokemons.isEmpty()) return;
    } else {
      var pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
      pokemons.add(pokemon);
    }
    boolean hasEgg = pokemons
      .stream()
      .anyMatch(pokemon -> pokemon.getSpecies().showdownId().equals("egg"));
    if (!hasEgg) return;
    var userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (!byPassCooldown && userInfo.hasCooldownHatch(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageCooldownHatch()
          .replace("%cooldown%", PlayerUtils.getCooldown(userInfo.getCooldownHatch())),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }

    for (Pokemon pokemon : pokemons) {
      if (!pokemon.getSpecies().showdownId().equals("egg")) continue;
      EggData.hatch(player, pokemon);
      userInfo.setCooldownHatch(player);
      CobbleDaycare.runAsync(() -> DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInfo));
    }

  }
}
