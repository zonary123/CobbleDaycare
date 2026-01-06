package com.kingpixel.cobbledaycare.commands.admin;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.SelectGender;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:09
 */
public class CommandBreed {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                              LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed.base", "cobbledaycare" +
            ".admin"),
          4))
        .then(
          CommandManager.argument("male", PartySlotArgumentType.Companion.partySlot())
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed.base", "cobbledaycare" +
                ".admin"),
              4))
            .then(
              CommandManager.argument("female", PartySlotArgumentType.Companion.partySlot())
                .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed.base", "cobbledaycare" +
                    ".admin"),
                  4))
                .executes(context -> {
                    var player = context.getSource().getPlayer();
                    return breed(context, player);
                  }
                ).then(
                  CommandManager.argument("player", EntityArgumentType.players())
                    .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed.other",
                      "cobbledaycare.admin"), 4))
                    .executes(context -> {
                      var players = EntityArgumentType.getPlayers(context, "player");
                      for (ServerPlayerEntity player : players) {
                        breed(context, player);
                      }
                      return 1;
                    })
                )
            )
        )
    );
  }

  private static int breed(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
    if (player == null) return 0;
    var userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInfo.hasCooldownBreed(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageCooldownBreed()
          .replace("%cooldown%", PlayerUtils.getCooldown(userInfo.getCooldownBreed())),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      return 0;
    }
    Plot plot = new Plot();
    Pokemon male = PartySlotArgumentType.Companion.getPokemon(context, "male");
    CobbleDaycare.fixBreedable(male);
    boolean maleCanBreed = plot.canBreed(male, SelectGender.MALE);
    plot.setMale(male);
    Pokemon female = PartySlotArgumentType.Companion.getPokemon(context, "female");
    CobbleDaycare.fixBreedable(female);
    boolean femaleCanBreed = plot.canBreed(female, SelectGender.FEMALE);
    plot.setFemale(female);

    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("maleCanBreed: " + maleCanBreed);
      CobbleUtils.LOGGER.info("femaleCanBreed: " + femaleCanBreed);
    }
    if (maleCanBreed && femaleCanBreed) {
      Cobblemon.INSTANCE.getStorage().getParty(player).add(plot.createEgg(player));
      userInfo.setCooldownBreed(player);
      DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInfo);
    } else {
      PlayerUtils.sendMessage(
        player,
        PokemonUtils.replace(CobbleDaycare.language.getMessageCannotBreed(), List.of(male, female)),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
    }
    return 1;
  }
}
