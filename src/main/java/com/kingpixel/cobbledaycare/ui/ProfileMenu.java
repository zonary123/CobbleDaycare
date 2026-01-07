package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.Model.PanelsConfig;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 14/03/2025 22:25
 */
@Data
public class ProfileMenu {
  private int rows;
  private String title;
  private ItemModel close;
  private ItemModel notifyCreateEgg;
  private ItemModel notifyBanPokemon;
  private ItemModel notifyActionBar;
  private List<PanelsConfig> panels;

  public ProfileMenu() {
    this.rows = 3;
    this.title = "&6Profile Menu";
    this.close = new ItemModel(22, "minecraft:barrier", "&cClose", List.of(""), 1);
    this.notifyActionBar = new ItemModel(4, "minecraft:book", "&6Notify Action Bar",
      List.of("&7Notify Action Bar: %active%")
      , 1);
    this.notifyCreateEgg = new ItemModel(11, "minecraft:book", "&6Notify Create Egg",
      List.of("&7Notify Create Egg: %active%")
      , 1);
    this.notifyBanPokemon = new ItemModel(15, "minecraft:book", "&6Notify Ban Pokemon",
      List.of("&7Notify Ban Pokemon: %active%")
      , 1);
    this.panels = new ArrayList<>();
    panels.add(new PanelsConfig(new ItemModel("minecraft:gray_stained_glass_pane"), rows));
  }

  public void open(ServerPlayerEntity player, UserInformation userInformation) {
    CobbleDaycare.runAsync(() -> {
      ChestTemplate template = ChestTemplate
        .builder(rows)
        .build();

      PanelsConfig.applyConfig(template, panels);

      notifyActionBar.applyTemplate(template, notifyActionBar.getButton(1, null, replaceLore(notifyActionBar.getLore(),
        userInformation.isActionBar()), action -> {
        userInformation.setActionBar(!userInformation.isActionBar());
        DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInformation);
        open(player, userInformation);
      }));

      notifyCreateEgg.applyTemplate(template, notifyCreateEgg.getButton(1, null, replaceLore(notifyCreateEgg.getLore(),
          userInformation.isNotifyCreateEgg()),
        action -> {
          userInformation.setNotifyCreateEgg(!userInformation.isNotifyCreateEgg());
          DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInformation);
          open(player, userInformation);
        }));

      notifyBanPokemon.applyTemplate(template, notifyBanPokemon.getButton(1, null,
        replaceLore(notifyBanPokemon.getLore(),
          userInformation.isNotifyBanPokemon()), action -> {
          userInformation.setNotifyBanPokemon(!userInformation.isNotifyBanPokemon());
          DatabaseClientFactory.INSTANCE.saveOrUpdateUserInformation(player, userInformation);
          open(player, userInformation);
        }));

      close.applyTemplate(template, close.getButton(action -> {
        CobbleDaycare.language.getPrincipalMenu().open(player);
      }));

      GooeyPage page = GooeyPage.builder()
        .template(template)
        .title(AdventureTranslator.toNative(title))
        .build();

      CobbleDaycare.server.execute(() -> UIManager.openUIForcefully(player, page));
    });
  }

  private List<String> replaceLore(List<String> lore, boolean value) {
    List<String> newLore = new ArrayList<>();
    for (String line : lore) {
      newLore.add(line.replace("%active%", value ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo()));
    }
    return newLore;
  }
}
