package com.kingpixel.cobbledaycare.commands.admin;

import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:05
 */
public class CommandBoosterSteps {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
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
                            DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInformation);
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
