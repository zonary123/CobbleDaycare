package com.kingpixel.cobbledaycare.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.DayCareInciense;
import com.kingpixel.cobbledaycare.models.*;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Date;
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
              context.getSource().sendMessage(
                AdventureTranslator.toNative(CobbleDaycare.language.getMessageReload(), CobbleDaycare.language.getPrefix())
              );
              CobbleDaycare.load();
              return 1;
            })
        ).then(
          CommandManager.literal("incense")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare.incense"), 4))
            .then(
              CommandManager.argument("player", EntityArgumentType.player())
                .then(
                  CommandManager.argument("incense", StringArgumentType.string())
                    .suggests((context, builder) -> {
                      for (Incense incense : DayCareInciense.incenses) {
                        builder.suggest(incense.getId());
                      }
                      return builder.buildFuture();
                    })
                    .executes(context -> {
                      ServerPlayerEntity player = EntityArgumentType.getPlayer(context, "player");
                      String s = StringArgumentType.getString(context, "incense");
                      var incense = DayCareInciense.INSTANCE().getIncense(s);
                      if (incense != null) {
                        ItemStack itemStack = incense.getItemStackIncense(1);
                        player.getInventory().insertStack(itemStack);
                      }
                      return 1;
                    })
                )
            )
        )
        .then(
          CommandManager.literal("multiplierSteps")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.admin", "cobbledaycare" +
              ".multipliersteps"), 4))
            .then(
              CommandManager.argument("seconds", IntegerArgumentType.integer(1))
                .then(
                  CommandManager.argument("multiplier", FloatArgumentType.floatArg(1.0f))
                    .then(
                      CommandManager.argument("player", EntityArgumentType.players())
                        .executes(context -> {
                          var players = EntityArgumentType.getPlayers(context, "player");
                          float multiplier = FloatArgumentType.getFloat(context, "multiplier");
                          int seconds = IntegerArgumentType.getInteger(context, "seconds");
                          for (ServerPlayerEntity player : players) {
                            UserInformation userInformation =
                              DatabaseClientFactory.INSTANCE.getUserInformation(player);
                            userInformation.setTimeMultiplierSteps(seconds * 20L);
                            userInformation.setMultiplierSteps(multiplier);
                            DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
                          }
                          return 1;
                        })
                    )
                )
            )
        )
      );
    }

    if (!CobbleDaycare.config.getCommandEggInfo().isEmpty()) {
      dispatcher.register(
        CommandManager.literal(CobbleDaycare.config.getCommandEggInfo())
          .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.egginfo", "cobbledaycare" +
              ".admin"),
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

    dispatcher.register(
      CommandManager.literal("hatch")
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch.base", "cobbledaycare" +
            ".admin")
          , 4))
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

    dispatcher.register(
      CommandManager.literal("breed")
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed.base", "cobbledaycare" +
            ".admin")
          , 4))
        .then(
          CommandManager.argument("male", PartySlotArgumentType.Companion.partySlot())
            .then(
              CommandManager.argument("female", PartySlotArgumentType.Companion.partySlot())
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

  private static int hatch(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
    var userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInfo.hasCooldownHatch(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageCooldownHatch()
          .replace("%cooldown%", PlayerUtils.getCooldown(new Date(userInfo.getCooldownHatch()))),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      return 0;
    }
    Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
    if (pokemon.getSpecies().showdownId().equals("egg")) {
      EggData.from(pokemon).hatch(player, pokemon);
      userInfo.setCooldownHatch(player);
      DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInfo);
    }
    return 1;
  }

  private static int breed(CommandContext<ServerCommandSource> context, ServerPlayerEntity player) {
    if (player == null) return 0;
    var userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    if (userInfo.hasCooldownBreed(player)) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageCooldownBreed()
          .replace("%cooldown%", PlayerUtils.getCooldown(new Date(userInfo.getCooldownBreed()))),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
      return 0;
    }
    Plot plot = new Plot();
    Pokemon male = PartySlotArgumentType.Companion.getPokemon(context, "male");
    boolean maleCanBreed = plot.canBreed(male, SelectGender.MALE);
    plot.setMale(male);
    Pokemon female = PartySlotArgumentType.Companion.getPokemon(context, "female");
    boolean femaleCanBreed = plot.canBreed(female, SelectGender.FEMALE);
    plot.setFemale(female);
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info("maleCanBreed: " + maleCanBreed);
      CobbleUtils.LOGGER.info("femaleCanBreed: " + femaleCanBreed);
    }
    if (maleCanBreed && femaleCanBreed) {
      Cobblemon.INSTANCE.getStorage().getParty(player).add(plot.createEgg(player));
      userInfo.setCooldownBreed(player);
      DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInfo);
    }
    return 1;
  }
}
