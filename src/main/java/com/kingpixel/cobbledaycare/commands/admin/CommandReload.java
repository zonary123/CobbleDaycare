package com.kingpixel.cobbledaycare.commands.admin;

import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:03
 */
public class CommandReload {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base
        .then(
          CommandManager.literal("reload")
            .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare_datapack.admin", "cobbledaycare_datapack" +
              ".reload"), 4))
            .executes(context -> {
              context.getSource().sendMessage(
                AdventureTranslator.toNative(CobbleDaycare.language.getMessageReload(), CobbleDaycare.language.getPrefix())
              );
              CobbleDaycare.load();
              return 1;
            })
        )
    );

  }
}
