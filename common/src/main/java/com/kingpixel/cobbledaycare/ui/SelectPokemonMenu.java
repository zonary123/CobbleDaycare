package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.button.linked.LinkType;
import ca.landonjw.gooeylibs2.api.button.linked.LinkedPageButton;
import ca.landonjw.gooeylibs2.api.helpers.PaginationHelper;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.page.LinkedPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.item.PokemonItem;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.SelectGender;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 5:56
 */
@Data
public class SelectPokemonMenu {
  private int rows;
  private String title;
  private Rectangle rectangle;
  private ItemModel previous;
  private ItemModel close;
  private ItemModel next;
  private List<PanelsConfig> panels;

  public SelectPokemonMenu() {
    this.rows = 6;
    this.title = "&6Select Pokemon";
    this.rectangle = new Rectangle(rows);
    this.previous = new ItemModel(45, "minecraft:arrow", "&6Previous", List.of(""), 1);
    this.close = new ItemModel(49, "minecraft:barrier", "&cClose", List.of(""), 1);
    this.next = new ItemModel(53, "minecraft:arrow", "&6Next", List.of(""), 1);
    this.panels = new ArrayList<>();
    panels.add(new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows));
  }

  public void open(ServerPlayerEntity player, Plot plot, UserInformation userInformation, SelectGender gender) {
    ChestTemplate template = ChestTemplate.builder(6)
      .build();

    List<Button> buttons = getButtons(plot, player, gender, userInformation);

    PanelsConfig.applyConfig(template, panels);
    rectangle.apply(template);


    LinkedPage.Builder builder = LinkedPage.builder()
      .title(AdventureTranslator.toNative(title));


    template.set(close.getSlot(), close.getButton(action -> {
      CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
    }));

    LinkedPageButton previousButton = LinkedPageButton.builder()
      .display(previous.getItemStack())
      .linkType(LinkType.Previous)
      .build();
    template.set(previous.getSlot(), previousButton);

    LinkedPageButton nextButton = LinkedPageButton.builder()
      .display(next.getItemStack())
      .linkType(LinkType.Next)
      .build();
    template.set(next.getSlot(), nextButton);

    GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, builder);

    UIManager.openUIForcefully(player, page);
  }

  private List<Button> getButtons(Plot plot, ServerPlayerEntity player, SelectGender gender, UserInformation userInformation) {
    List<Button> buttons = new ArrayList<>();
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      addPokemon(pokemon, plot, player, gender, userInformation, buttons);
    }
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getPC(player)) {
      addPokemon(pokemon, plot, player, gender, userInformation, buttons);
    }
    return buttons;
  }

  private void addPokemon(Pokemon pokemon, Plot plot, ServerPlayerEntity player, SelectGender gender,
                          UserInformation userInformation, List<Button> buttons) {
    if (plot.canBreed(pokemon, gender)) {
      GooeyButton button = GooeyButton
        .builder()
        .display(PokemonItem.from(pokemon))
        .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.getTranslatedName(pokemon)))
        .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(PokemonUtils.replaceLore(pokemon))))
        .onClick(action -> {
          plot.addPokemon(player, pokemon, gender, userInformation);
          CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
        }).build();
      buttons.add(button);
    }
  }


}
