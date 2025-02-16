package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.UIUtils;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 1:49
 */
public class PlotMenu {
  private int rows;
  private String title;
  private ItemModel maleItem;
  private ItemModel eggItem;
  private ItemModel femaleItem;
  private List<PanelsConfig> panels;

  public PlotMenu() {
    this.rows = 6;
    this.title = "&6Plot";
    this.maleItem = new ItemModel("minecraft:minecart", "&7Male",
      List.of(
        "&7First Parent: %pokemon1% %gender1%"
      ));
    this.eggItem = new ItemModel("minecraft:egg", "&7Egg",
      List.of(
        "&7Click to get the eggs"
      ));
    this.femaleItem = new ItemModel("minecraft:minecart", "&7Female",
      List.of(
        "&7Second Parent: %pokemon2% %gender2%"
      ));
    this.panels = List.of(
      new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows)
    );

  }

  public void open(ServerPlayerEntity player, Plot plot, UserInformation userInformation) {
    ChestTemplate template = ChestTemplate
      .builder(rows)
      .build();

    PanelsConfig.applyConfig(template, panels);

    if (UIUtils.isInside(maleItem, rows)) {
      Pokemon male = plot.getMale();
      GooeyButton button = getButton(male, maleItem)
        .onClick(action -> {
          plot.openMale(player, userInformation);
        })
        .build();
      template.set(maleItem.getSlot(), button);
    }

    if (UIUtils.isInside(eggItem, rows)) {
      template.set(eggItem.getSlot(), eggItem.getButton(action -> {
        if (plot.giveEggs(player)) {
          DatabaseClientFactory.databaseClient.updateUserInformation(player, userInformation);
        }
      }));
    }

    if (UIUtils.isInside(femaleItem, rows)) {
      Pokemon female = plot.getFemale();
      GooeyButton button = getButton(female, femaleItem)
        .onClick(action -> {
        }).build();
      template.set(femaleItem.getSlot(), button);
    }


    GooeyPage page = GooeyPage
      .builder()
      .template(template)
      .title(title)
      .build();

    UIManager.openUIForcefully(player, page);
  }

  private GooeyButton.Builder getButton(Pokemon pokemon, ItemModel itemModel) {
    ItemStack itemStack = pokemon == null ? itemModel.getItemStack() : PokemonItem.from(pokemon);
    String name = getName(pokemon, itemModel.getDisplayname());
    List<String> lore = getLore(pokemon, itemModel.getLore());

    return GooeyButton.builder()
      .display(itemStack)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(name))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)));
  }

  private String getName(Pokemon pokemon, String message) {
    return PokemonUtils.replace(message, pokemon);
  }


  private List<String> getLore(Pokemon pokemon, List<String> lore) {
    List<String> cloneLore = new ArrayList<>(lore);
    cloneLore.replaceAll(s -> PokemonUtils.replace(s, pokemon));
    return cloneLore;
  }
}
