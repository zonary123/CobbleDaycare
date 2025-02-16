package com.kingpixel.cobbledaycare.commands;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
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
            CobbleDaycare.language.getDaycareMenu().open(player);
          }
          return 1;
        })
      );
    }
  }
}
