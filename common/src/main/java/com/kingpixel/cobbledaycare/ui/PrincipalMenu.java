package com.kingpixel.cobbledaycare.ui;

import ca.landonjw.gooeylibs2.api.UIManager;
import ca.landonjw.gooeylibs2.api.button.GooeyButton;
import ca.landonjw.gooeylibs2.api.page.GooeyPage;
import ca.landonjw.gooeylibs2.api.template.types.ChestTemplate;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.Model.ItemModel;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.AdventureTranslator;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import lombok.Data;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
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
  private ItemModel profileOptions;
  private ItemModel close;

  public PrincipalMenu() {
    this.rows = 3;
    this.title = "&6Plots Menu";
    this.info = new ItemModel(5, "minecraft:book", "<#82d448>ɪɴꜰᴏʀᴍᴀᴛɪᴏɴ", List.of(
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
      "&7Male: %pokemon1% %form1% (%item1%)",
      "&7Female: %pokemon2% %form2% (%item2%)",
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
    this.profileOptions = new ItemModel(3, "item:1:minecraft:player_head", "&6Profile Options", List.of(
      "&7Click to open the profile options"
    ), 0);
  }

  public void open(ServerPlayerEntity player) {
    CompletableFuture.runAsync(() -> {
      try {
        ChestTemplate template = ChestTemplate.builder(rows).build();
        UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
        int numPlots = 0;
        int size = CobbleDaycare.config.getSlotPlots().size();
        for (int i = 0; i < size; i++) {
          if (PermissionApi.hasPermission(player, "cobbledaycare.plot." + (i + 1), 4)) {
            numPlots = i + 1;
          }
        }
        if (numPlots == 0) numPlots = 1;
        for (int i = 0; i < numPlots; i++) {
          int slot = CobbleDaycare.config.getSlotPlots().get(i);
          Plot plot = userInformation.getPlots().get(i);
          if (plot == null) continue;
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

        GooeyButton profileButton = profileOptions.getButton(action -> {
          CobbleDaycare.language.getProfileMenu().open(player, userInformation);
        });

        if (profileOptions.getItem().contains("minecraft:player_head")) {
          ItemStack headItem = PlayerUtils.getHeadItem(player);
          headItem.set(DataComponentTypes.CUSTOM_NAME, AdventureTranslator.toNative(profileOptions.getDisplayname()));
          headItem.set(DataComponentTypes.LORE,
            new LoreComponent(AdventureTranslator.toNativeL(profileOptions.getLore())));
          profileButton.setDisplay(headItem);
        }

        template.set(profileOptions.getSlot(), profileButton);

        GooeyPage page = GooeyPage.builder()
          .template(template)
          .title(AdventureTranslator.toNative(title))
          .build();

        UIManager.openUIForcefully(player, page);
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }

  private List<String> replacePlotLore(Plot plot, ServerPlayerEntity player) {
    List<String> newLore = new ArrayList<>(lore);

    String cooldown = PlayerUtils.getCooldown(new Date(plot.getTimeToHatch()));
    Pokemon male = plot.getMale();
    Pokemon female = plot.getFemale();
    List<Pokemon> parents = new ArrayList<>();
    parents.add(male);
    parents.add(female);

    newLore.replaceAll(s -> {
      s = s
        .replace("%eggs%", String.valueOf(plot.getEggs().size()))
        .replace("%limiteggs%", String.valueOf(plot.limitEggs(player)))
        .replace("%time%", cooldown)
        .replace("%cooldown%", cooldown);
      return PokemonUtils.replace(s, parents);
    });

    return newLore;
  }


}
