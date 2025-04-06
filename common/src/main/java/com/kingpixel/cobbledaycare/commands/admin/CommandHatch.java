package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Date;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:07
 */
public class CommandHatch {
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
              hatch(context, player);
              return 1;
            }).then(
              CommandManager.argument("player", EntityArgumentType.players())
                .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.other",
                  "cobbledaycare.admin"), 4))
                .executes(context -> {
                  var players = EntityArgumentType.getPlayers(context, "player");
                  for (ServerPlayerEntity player : players) {
                    hatch(context, player);
                  }
                  return 1;
                })
            )
        )
    );
  }

  private static void hatch(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
    var userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInfo.hasCooldownHatch(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageCooldownHatch()
          .replace("%cooldown%", PlayerUtils.getCooldown(new Date(userInfo.getCooldownHatch()))),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      return;
    }
    Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
    if (pokemon.getSpecies().showdownId().equals("egg")) {
      EggData.from(pokemon).hatch(player, pokemon);
      userInfo.setCooldownHatch(player);
      DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInfo);
    }
  }
}
