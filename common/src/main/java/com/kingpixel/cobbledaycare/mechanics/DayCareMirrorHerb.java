package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.List;

import static com.cobblemon.mod.common.CobblemonItems.MIRROR_HERB;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareMirrorHerb extends Mechanics {

  public DayCareMirrorHerb() {
  }

  private static void mirrorHerb(Pokemon target, Pokemon source) {
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Mirror Herb: " + target.getSpecies().showdownId());
    }
    List<Move> targetMoves = target.getMoveSet().getMoves();
    int size = targetMoves.size();
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Target Moves: " + size);
    }
    if (size < 4) {
      List<String> sourceMoves = source.getMoveSet().getMoves().stream().map(Move::getName).toList();
      List<String> eggMoves = target.getForm().getMoves().getEggMoves().stream().map(MoveTemplate::getName).toList();
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Source Moves: " + sourceMoves);
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Egg Moves: " + eggMoves);
      }
      for (String move : sourceMoves) {
        if (size >= 4) break;
        if (eggMoves.contains(move)) {
          if (CobbleDaycare.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Mirror Herb Move: " + move);
          }
          MoveTemplate moveTemplate = Moves.INSTANCE.getByName(move);
          if (moveTemplate == null) {
            if (CobbleDaycare.config.isDebug()) {
              CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID, "Move not found: " + move);
            }
            continue;
          }
          if (target.getMoveSet().add(moveTemplate.create())) {
            target.removeHeldItem();
            size++;
          }
        }
      }
    }
  }

  @Override public String replace(String text) {
    String s = isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo();
    return text
      .replace("%mirrorHerb%", s)
      .replace("%mirrorherb%", s);
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    Pokemon male = builder.getMale();
    Pokemon female = builder.getFemale();
    boolean hasMaleMirrorHerb = male.heldItem().getItem() == MIRROR_HERB;
    boolean hasFemaleMirrorHerb = female.heldItem().getItem() == MIRROR_HERB;

    if (hasMaleMirrorHerb) mirrorHerb(male, female);
    if (hasFemaleMirrorHerb) mirrorHerb(female, male);
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {

  }

  @Override public void commandCreateEgg(ServerPlayerEntity player, Pokemon pokemon) {

  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "mirror_herb";
  }
}
