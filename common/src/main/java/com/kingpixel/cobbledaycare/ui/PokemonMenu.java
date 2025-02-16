package com.kingpixel.cobbledaycare.ui;

import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.features.shops.Shop;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

/**
 * @author Carlos Varas Alonso - 31/01/2025 2:44
 */
@Getter
@Setter
public class PokemonMenu {
  private int rows;
  private String title;
  private Shop.Rectangle rectangle;
  private ItemModel previous;
  private ItemModel close;
  private ItemModel next;
  private List<PanelsConfig> panels;

  public PokemonMenu() {
    this.rows = 6;
    this.title = "Pokemon Menu";
    this.rectangle = new Shop.Rectangle(6);
    this.previous = CobbleUtils.language.getItemPrevious();
    this.close = CobbleUtils.language.getItemClose();
    this.next = CobbleUtils.language.getItemNext();
    this.panels = List.of(
      new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows)
    );
  }

  public void open(ServerPlayerEntity player) {

  }
}
