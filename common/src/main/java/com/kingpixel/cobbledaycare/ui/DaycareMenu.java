package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 0:53
 */
public class DaycareMenu {
  private int rows;
  private String title;
  private ItemModel plotItemEmpty;
  private ItemModel plotItemCreatingEgg;
  private ItemModel plotItemWithEggs;
  private List<Integer> slotsPlot;
  private List<PanelsConfig> panels;

  public DaycareMenu() {
    this.rows = 6;
    this.title = "&6Daycare";
    this.plotItemEmpty = new ItemModel("minecraft:minecart", "&7Empty",
      List.of(
        "&7Click to Open Plot",
        "",
        "&7First Parent: %pokemon1% %gender1%",
        "&7Second Parent: %pokemon2% %gender2%"
      ));
    this.plotItemCreatingEgg = new ItemModel("minecraft:furnace_minecart");
    this.plotItemWithEggs = new ItemModel("minecraft:chest_minecart");
    this.slotsPlot = List.of(10, 12, 14, 16);
    this.panels = List.of(
      new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows)
    );
  }

  public void open(ServerPlayerEntity player) {
    ChestTemplate template = ChestTemplate
      .builder(rows)
      .build();

    PanelsConfig.applyConfig(template, panels);


    UserInformation userInformation = DatabaseClientFactory.databaseClient.getUserInformation(player);

    int numPlots = 0;

    for (Integer i : slotsPlot) {
      if (PermissionApi.hasPermission(player, "daycare.plot." + i + 1, 2)) {
        if (i > numPlots)
          numPlots = i;
      }
    }

    boolean check = false;
    for (int i = 0; i < numPlots; i++) {
      if (i >= slotsPlot.size()) break;
      if (userInformation.check(i + 1)) {
        check = true;
      }
      Plot plot = userInformation.getPlots().get(i);
      List<Pokemon> parents = plot.getParents();
      GooeyButton plotButton = getPlotButton(player, parents, plot, userInformation);
      template.getSlot(slotsPlot.get(i)).setButton(plotButton);
    }
    if (check) {
      DatabaseClientFactory.databaseClient.updateUserInformation(player, userInformation);
    }


    GooeyPage page = GooeyPage
      .builder()
      .title(AdventureTranslator.toNative(title))
      .template(template)
      .build();

    UIManager.openUIForcefully(player, page);

  }

  private GooeyButton getPlotButton(ServerPlayerEntity player, List<Pokemon> parents, Plot plot, UserInformation userInformation) {
    ItemModel itemModel = plotItemEmpty;
    String name = itemModel.getDisplayname();
    PokemonUtils.replace(name, parents);
    List<String> lore = new ArrayList<>(itemModel.getLore());
    lore.replaceAll(s -> PokemonUtils.replace(s, parents));
    if (hasEggs()) {
      itemModel = plotItemWithEggs;
    } else if (hasParents()) {
      itemModel = plotItemCreatingEgg;
    }
    return itemModel.getButton(1, name, lore, action -> {
      CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
    });
  }

  private boolean hasParents() {
    return false;
  }

  private boolean hasEggs() {
    return false;
  }
}
