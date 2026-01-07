package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.Button;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
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
import com.kingpixel.cobbleutils.Model.DurationValue;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.Model.Rectangle;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Author: Carlos Varas Alonso - 11/03/2025 5:56
 * Refactor optimizado para mejor rendimiento.
 */
@Data
public class SelectPokemonMenu {
  private static final int POKEMONS_PER_PAGE = 45;

  private final int rows;
  private final String title;
  private final Rectangle rectangle;
  private final ItemModel previous;
  private final ItemModel close;
  private final ItemModel next;
  private final List<PanelsConfig> panels;

  // Template base (solo se construye una vez)
  transient
  private final ChestTemplate baseTemplate;

  public SelectPokemonMenu() {
    this.rows = 6;
    this.title = "&6Select Pokemon";
    this.rectangle = new Rectangle(rows);
    this.previous = new ItemModel(45, "minecraft:arrow", "&6Previous", Collections.emptyList(), 1);
    this.close = new ItemModel(49, "minecraft:barrier", "&cClose", Collections.emptyList(), 1);
    this.next = new ItemModel(53, "minecraft:arrow", "&6Next", Collections.emptyList(), 1);
    this.panels = new ArrayList<>();
    panels.add(new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows));

    // Construcción del template base
    ChestTemplate template = ChestTemplate.builder(rows).build();
    PanelsConfig.applyConfig(template, panels);
    rectangle.apply(template);
    this.baseTemplate = template;
  }

  public void open(ServerPlayerEntity player, Plot plot, UserInformation userInformation, SelectGender gender, int position) {
    if (PlayerUtils.isCooldownMenu(player, "select_menu", DurationValue.parse("1s"))) return;
    CobbleDaycare.runAsync(() -> {
      // Clonar el template base
      ChestTemplate template = baseTemplate.clone();

      // Obtener botones para esta página
      List<Button> buttons = getButtons(plot, player, gender, userInformation, position);

      LinkedPage.Builder builder = LinkedPage.builder().title(AdventureTranslator.toNative(title));

      // Botón cerrar
      close.applyTemplate(template, close.getButton(action -> {
        CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
      }, 1, TimeUnit.SECONDS, 1));

      // Botón previo
      if (position > 0) {
        previous.applyTemplate(template, previous.getButton(action -> {
          if (CobbleDaycare.config.hasOpenCooldown(action.getPlayer())) return;
          open(player, plot, userInformation, gender, Math.max(0, position - POKEMONS_PER_PAGE));
        }, 1, TimeUnit.SECONDS, 1));
      }

      // Botón siguiente
      if (buttons.size() == POKEMONS_PER_PAGE) {
        next.applyTemplate(template, next.getButton(action -> {
          if (CobbleDaycare.config.hasOpenCooldown(action.getPlayer())) return;
          open(player, plot, userInformation, gender, position + POKEMONS_PER_PAGE);
        }, 1, TimeUnit.SECONDS, 1));
      }

      GooeyPage page = PaginationHelper.createPagesFromPlaceholders(template, buttons, builder);

      CobbleDaycare.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }

  private List<Button> getButtons(Plot plot, ServerPlayerEntity player, SelectGender gender,
                                  UserInformation userInformation, int position) {

    List<Button> buttons = new ArrayList<>(POKEMONS_PER_PAGE);

    int start = position;
    int end = position + POKEMONS_PER_PAGE;
    int index = 0;

    // Party
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      if (pokemon != null && plot.canBreed(pokemon, gender)) {
        if (index >= start && index < end) {
          addPokemon(pokemon, plot, player, gender, userInformation, buttons);
        }
        index++;
      }
    }

    // PC
    var pcIterator = Cobblemon.INSTANCE.getStorage().getPC(player).iterator();
    while (pcIterator.hasNext()) {
      Pokemon pokemon = pcIterator.next();
      if (pokemon != null && plot.canBreed(pokemon, gender)) {
        if (index >= start && index < end) {
          addPokemon(pokemon, plot, player, gender, userInformation, buttons);
        }
        index++;
      }
    }

    return buttons;
  }


  private void addPokemon(@NotNull Pokemon pokemon, Plot plot, ServerPlayerEntity player,
                          SelectGender gender, UserInformation userInformation, List<Button> buttons) {
    ItemStack display = PokemonItem.from(pokemon);
    List<String> lore = PokemonUtils.replaceLore(pokemon);

    GooeyButton button = GooeyButton.builder()
      .display(display)
      .with(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(PokemonUtils.getTranslatedName(pokemon)))
      .with(DataComponentTypes.LORE, new LoreComponent(AdventureTranslator.toNativeL(lore)))
      .onClick(action -> CobbleDaycare.runAsync(() -> {
        if (PlayerUtils.isCooldownMenu(player, "select_menu_add", DurationValue.parse("1s"))) return;
        CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
        plot.addPokemon(player, pokemon, gender, userInformation);
      }))
      .build();

    buttons.add(button);
  }
}
