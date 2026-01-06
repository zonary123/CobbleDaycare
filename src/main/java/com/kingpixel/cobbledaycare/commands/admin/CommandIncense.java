package com.kingpixel.cobbledaycare.commands.admin;

import com.kingpixel.cobbledaycare.mechanics.DayCareInciense;
import com.kingpixel.cobbledaycare.models.Incense;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 05/04/2025 2:04
 */
public class CommandIncense {
  public static void register(CommandDispatcher<ServerCommandSource> dispatcher, LiteralArgumentBuilder<ServerCommandSource> base) {
    dispatcher.register(
      base.then(
        CommandManager.literal("incense")
          .requires(source -> PermissionApi.hasPermission(source, List.of("cobbledaycare_datapack.admin", "cobbledaycare_datapack.incense"), 4))
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
    );
  }
}
