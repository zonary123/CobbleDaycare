package com.kingpixel.cobbledaycare.commands;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.commands.admin.*;
import com.kingpixel.cobbledaycare.commands.base.CommandEggInfo;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
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
    try {
      for (String command : CobbleDaycare.config.getCommands()) {
        var base = CommandManager.literal(command)
          .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare.user",
              "cobbledaycare.admin"),
            4));
        if (!CobbleDaycare.config.getCommandEggInfo().isEmpty()) {
          CommandEggInfo.register(dispatcher, CommandManager.literal(CobbleDaycare.config.getCommandEggInfo()));
        }

        CommandHatch.register(dispatcher, CommandManager.literal("hatch"));
        CommandBreed.register(dispatcher, CommandManager.literal("breed"));

        CommandReload.register(dispatcher, base);
        CommandIncense.register(dispatcher, base);
        CommandBoosterSteps.register(dispatcher, base);
        CommandBreedable.register(dispatcher, base);
        CommandEgg.register(dispatcher, base);
        CommandSteps.register(dispatcher, base);

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
          )
        );
      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

}
