package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.ItemUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import lombok.Data;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 11/03/2025 4:11
 */
@Data
public class PrincipalMenu {
  private int rows;
  private String title;
  private ItemModel info;
  private List<String> lore;
  private ItemModel plotWithEgg;
  private ItemModel plotWithOutEgg;
  private ItemModel plotWithOutParents;
  private ItemModel close;

  public PrincipalMenu() {
    this.rows = 3;
    this.title = "&6Plots Menu";
    this.info = new ItemModel(4, "minecraft:book", "<#82d448>ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ", List.of(
      "<#82d448>--- ᴀʙɪʟɪᴛʏ ---",
      "&7Transmit Ah: &6%ability% %activeAbility%",
      "<#82d448>---- ɪᴠꜱ ----",
      "&7Max Ivs Random: &6%maxivs%",
      "&7Destiny Knot: &6%destinyknot%",
      "&7Power Item: &6%poweritem%",
      "<#82d448>---- ɴᴀᴛᴜʀᴇ ----",
      "&7Ever Stone: &6%everstone%",
      "<#82d448>---- ꜱʜɪɴʏ ----",
      "&7Masuda: &6%masuda% &7Multiplier: &6%multipliermasuda%",
      "&7Shiny: &6%parentsShiny% &7Multiplier: &6%multipliershiny%",
      "&7ShinyRate: &6%shinyrate%",
      "<#82d448>---- ᴍᴏᴠᴇꜱ ----",
      "&7Egg Moves: &6%eggmoves%",
      "&7Mirror Herb: &6%mirrorherb%",
      "<#82d448>---- ᴄᴏᴏʟᴅᴏᴡɴ ----",
      "&7Cooldown: &6%cooldown%"
    ), 0);
    this.lore = List.of(
      "&7Male: %male% (%malehelditem%)",
      "&7Female: %female% (%femalehelditem%)",
      "&7Eggs: %eggs%/%limiteggs%",
      "&7Time to hatch: %time%"
    );
    this.plotWithEgg = new ItemModel("item:1:minecraft:turtle_egg", "&6Plot with egg", List.of(
    ), 0);
    this.plotWithOutEgg = new ItemModel("item:1:cobblemon:pasture", "&6Plot without egg", List.of(

    ), 0);
    this.plotWithOutParents = new ItemModel("item:1:minecraft:gunpowder", "&6Plot without parents", List.of(

    ), 0);
    this.close = new ItemModel(22, "item:1:minecraft:barrier", "&cClose", List.of(
      "&7Click to close the menu"
    ), 0);
  }

  public void open(ServerPlayerEntity player) {
    ChestTemplate template = ChestTemplate.builder(rows).build();
    UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    int numPlots = 0;
    int size = CobbleDaycare.config.getSlotPlots().size();
    for (int i = 0; i < size; i++) {
      if (PermissionApi.hasPermission(player, "cobbledaycare.plot." + (i + 1), 4)) {
        numPlots = i + 1;
      }
    }

    for (int i = 0; i < numPlots; i++) {
      int slot = CobbleDaycare.config.getSlotPlots().get(i);
      Plot plot = userInformation.getPlots().get(i);
      ItemModel itemModel;
      if (plot.hasEggs()) {
        itemModel = plotWithEgg;
      } else if (plot.notParents()) {
        itemModel = plotWithOutParents;
      } else {
        itemModel = plotWithOutEgg;
      }

      template.set(slot, itemModel.getButton(plot.getEggs().size(), null, replacePlotLore(plot, player), action -> {
        CobbleDaycare.language.getPlotMenu().open(player, plot, userInformation);
      }));

    }

    List<String> loreInfo = new ArrayList<>(info.getLore());
    long cooldown = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(PlayerUtils.getCooldown(CobbleDaycare.config.getCooldowns(), CobbleDaycare.config.getCooldown()
      , player));
    loreInfo.replaceAll(s -> {
      for (Mechanics mechanic : CobbleDaycare.mechanics) {
        s = mechanic.replace(s);
      }
      s = s.replace("%cooldown%", PlayerUtils.getCooldown(new Date(cooldown)));
      return s;
    });

    template.set(info.getSlot(), info.getButton(1, null, loreInfo, action -> {

    }));

    template.set(close.getSlot(), close.getButton(action -> {
      UIManager.closeUI(player);
    }));

    GooeyPage page = GooeyPage.builder()
      .template(template)
      .title(AdventureTranslator.toNative(title))
      .build();

    UIManager.openUIForcefully(player, page);
  }

  private List<String> replacePlotLore(Plot plot, ServerPlayerEntity player) {
    List<String> newLore = new ArrayList<>(lore);

    String cooldown = PlayerUtils.getCooldown(new Date(plot.getTimeToHatch()));
    newLore.replaceAll(s -> s
      .replace("%male%", plot.getMale() != null ? plot.getMale().getSpecies().getName() : CobbleUtils.language.getUnknown())
      .replace("%malehelditem%", plot.getMale() != null ? ItemUtils.getNameItem(plot.getMale().getHeldItem$common()) : CobbleUtils.language.getUnknown())
      .replace("%female%", plot.getFemale() != null ? plot.getFemale().getSpecies().getName() :
        CobbleUtils.language.getUnknown())
      .replace("%femalehelditem%", plot.getFemale() != null ?
        ItemUtils.getNameItem(plot.getFemale().getHeldItem$common()) : CobbleUtils.language.getUnknown())
      .replace("%eggs%", String.valueOf(plot.getEggs().size()))
      .replace("%limiteggs%", String.valueOf(plot.limitEggs(player)))
      .replace("%time%", cooldown)
      .replace("%cooldown%", cooldown)
    );
    return newLore;
  }

  private List<String> replaceInfoLore(ServerPlayerEntity player) {
    return info.getLore();
  }


}
