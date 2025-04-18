package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:06
 */
public class CommandBreedable {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    var literalArgumentBuilder = CommandManager.literal("breedable")
      .requires(
        source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin",
          "cobbledaycare.breedable.self", "cobbledaycare.breedable.other"), 2))
      .then(
        CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
          .executes(context -> {
            if (!context.getSource().isExecutedByPlayer()) return 0;
            var player = context.getSource().getPlayer();
            if (player == null) return 0;
            apply(null, List.of(player), false, context);
            return 1;
          })
          .then(
            CommandManager.argument("player", EntityArgumentType.players())
              .requires(
                source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare.breedable.other"), 2))
              .then(
                CommandManager.argument("breedable", BoolArgumentType.bool())
                  .executes(context -> {
                    var player = context.getSource().getPlayer();
                    var players = EntityArgumentType.getPlayers(context, "player");
                    boolean breedable = BoolArgumentType.getBool(context, "breedable");
                    apply(player, players, breedable, context);
                    return 1;
                  })
              )
          )
      );
    dispatcher.register(literalArgumentBuilder);
    dispatcher.register(base
      .then(
        literalArgumentBuilder
      )
    );
  }

  private static void apply(ServerPlayerEntity staff, Collection<ServerPlayerEntity> players, boolean breedable,
                            CommandContext<ServerCommandSource> context) {
    for (ServerPlayerEntity player : players) {
      Pokemon pokemon = PartySlotArgumentType.Companion.getPokemonOf(context, "slot", player);
      if (pokemon == null) continue;
      CobbleDaycare.setBreedable(pokemon, breedable);
      pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG, !breedable);
      PlayerUtils.sendMessage(
        player,
        PokemonUtils.replace(CobbleDaycare.language.getMessageBreedable(), pokemon),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      if (staff != null) {
        if (staff.equals(player)) continue;
        PlayerUtils.sendMessage(
          staff,
          PokemonUtils.replace(CobbleDaycare.language.getMessageBreedable(), pokemon),
          CobbleDaycare.language.getPrefix(),
          TypeMessage.CHAT
        );
      }
    }
  }

}
