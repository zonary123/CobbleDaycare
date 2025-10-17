package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.SelectGender;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.CustomModelDataComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 11/03/2025 5:09
 */
@Data
public class PlotMenu {
  private int rows;
  private String title;
  private ItemModel male;

  private ItemModel female;
  private ItemModel egg;
  private ItemModel close;

  public PlotMenu() {
    this.rows = 3;
    this.title = "&6Plot Menu";
    this.male = new ItemModel(10, "minecraft:light_blue_wool", "&6Male", List.of(""), 1);
    this.female = new ItemModel(16, "minecraft:pink_wool", "&6Female", List.of(""), 1);
    this.egg = new ItemModel(13, "minecraft:dragon_egg", "&6Egg", List.of(""), 1);
    this.close = new ItemModel(22, "minecraft:barrier", "&cClose", List.of(""), 1);
  }

  public void open(ServerPlayerEntity player, Plot plot, UserInformation userInformation) {
    CompletableFuture.runAsync(() -> {
        ChestTemplate template = ChestTemplate.builder(rows)
          .build();

        if (plot.checkEgg(player, userInformation))
          DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);

        GooeyButton maleButton = GooeyButton
          .builder()
          .display(plot.getMale() != null ? PokemonItem.from(plot.getMale()) : male.getItemStack())
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(plot.getMale()))))
          .with(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) egg.getCustomModelData()))
          .onClick(action -> {
            CompletableFuture.runAsync(() -> {
                if (CobbleDaycare.config.hasOpenCooldown(action.getPlayer())) return;
                if (plot.getMale() != null) {
                  Cobblemon.INSTANCE.getStorage().getParty(player).add(plot.getMale());
                  plot.setMale(null);
                  DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
                  open(player, plot, userInformation);
                } else {
                  CobbleDaycare.language.getSelectPokemonMenu().open(player, plot, userInformation, SelectGender.MALE, 0);
                }
              }, CobbleDaycare.DAYCARE_EXECUTOR)
              .exceptionally(e -> {
                e.printStackTrace();
                return null;
              });
          })
          .build();
        male.applyTemplate(template, maleButton);

        ItemStack displayEgg = plot.getEggs().isEmpty() ? egg.getItemStack() : PokemonItem.from(plot.getEggs().getFirst());
        GooeyButton eggButton = GooeyButton.builder()
          .display(displayEgg)
          .with(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) egg.getCustomModelData()))
          .onClick(action -> CompletableFuture.runAsync(() -> {
              if (plot.giveEggs(player)) {
                if (plot.limitEggs(player) >= plot.getEggs().size()) plot.setTime(player);
                DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
                CobbleDaycare.language.getPrincipalMenu().open(player);
              }
            }, CobbleDaycare.DAYCARE_EXECUTOR)
            .exceptionally(e -> {
              e.printStackTrace();
              return null;
            }))
          .build();
        eggButton.setDisplay(displayEgg);
        egg.applyTemplate(template, eggButton);

        GooeyButton femaleButton = GooeyButton
          .builder()
          .display(plot.getFemale() != null ? PokemonItem.from(plot.getFemale()) : female.getItemStack())
          .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(plot.getFemale()))))
          .with(DataComponentTypes.CUSTOM_MODEL_DATA, new CustomModelDataComponent((int) female.getCustomModelData()))
          .onClick(action -> CompletableFuture.runAsync(() -> {
              if (CobbleDaycare.config.hasOpenCooldown(action.getPlayer())) return;
              if (plot.getFemale() != null) {
                Cobblemon.INSTANCE.getStorage().getParty(player).add(plot.getFemale());
                plot.setFemale(null);
                DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
                open(player, plot, userInformation);
              } else {
                CobbleDaycare.language.getSelectPokemonMenu().open(player, plot, userInformation, SelectGender.FEMALE, 0);
              }
            }, CobbleDaycare.DAYCARE_EXECUTOR)
            .exceptionally(e -> {
              e.printStackTrace();
              return null;
            }))
          .build();
        female.applyTemplate(template, femaleButton);

        template.set(close.getSlot(), close.getButton(action -> {
          CobbleDaycare.language.getPrincipalMenu().open(player);
        }));

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .title(AdventureTranslator.toNative(title))
          .build();

        UIManager.openUIForcefully(player, page);
      }, CobbleDaycare.DAYCARE_EXECUTOR)
      .exceptionally(e -> {
        e.printStackTrace();
        return null;
      });
  }

}
