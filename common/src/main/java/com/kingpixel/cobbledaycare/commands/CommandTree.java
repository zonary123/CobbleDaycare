package com.kingpixel.cobbledaycare.commands;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.command.argument.PartySlotArgumentType;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.EggData;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.SelectGender;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
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
        ).then(
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

                  }
                }
                return 1;
              })
          )
      );
    }

    dispatcher.register(
      CommandManager.literal("hatch")
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.hatch", "cobbledaycare.admin")
          , 4))
        .then(
          CommandManager.argument("slot", PartySlotArgumentType.Companion.partySlot())
            .executes(context -> {
              if (context.getSource().isExecutedByPlayer()) {
                ServerPlayerEntity player = context.getSource().getPlayer();
                Pokemon pokemon = PartySlotArgumentType.Companion.getPokemon(context, "slot");
                if (pokemon.getSpecies().showdownId().equals("egg")) {
                  EggData.from(pokemon).hatch(player, pokemon);
                }
              }
              return 1;
            })
        )
    );

    dispatcher.register(
      CommandManager.literal("breed")
        .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.breed", "cobbledaycare.admin")
          , 4))
        .then(
          CommandManager.argument("male", PartySlotArgumentType.Companion.partySlot())
            .then(
              CommandManager.argument("female", PartySlotArgumentType.Companion.partySlot())
                .executes(context -> {
                    if (context.getSource().isExecutedByPlayer()) {
                      ServerPlayerEntity player = context.getSource().getPlayer();
                      if (player == null) return 0;
                      
                      Plot plot = new Plot();
                      Pokemon male = PartySlotArgumentType.Companion.getPokemon(context, "male");
                      boolean maleCanBreed = plot.canBreed(male, SelectGender.MALE);
                      plot.setMale(male);
                      Pokemon female = PartySlotArgumentType.Companion.getPokemon(context, "female");
                      boolean femaleCanBreed = plot.canBreed(female, SelectGender.FEMALE);
                      plot.setFemale(female);
                      CobbleUtils.LOGGER.info("maleCanBreed: " + maleCanBreed);
                      CobbleUtils.LOGGER.info("femaleCanBreed: " + femaleCanBreed);
                      if (maleCanBreed && femaleCanBreed) {
                        Cobblemon.INSTANCE.getStorage().getParty(player).add(plot.createEgg(player));
                      }
                    }
                    return 1;
                  }
                )
            )
        )
    );
  }
}
