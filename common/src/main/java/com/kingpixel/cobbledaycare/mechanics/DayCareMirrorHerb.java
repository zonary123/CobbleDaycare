package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
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
    List<Move> targetMoves = target.getMoveSet().getMoves();
    int size = targetMoves.size();
    if (size < 4) {
      List<String> sourceMoves = source.getMoveSet().getMoves().stream().map(Move::getName).toList();
      List<String> eggMoves = target.getForm().getMoves().getEggMoves().stream().map(MoveTemplate::getName).toList();
      for (String move : sourceMoves) {
        if (size >= 4) break;
        if (eggMoves.contains(move)) {
          MoveTemplate moveTemplate = Moves.INSTANCE.getByName(move);
          if (moveTemplate == null) {
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

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {

  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s;
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "mirror_herb";
  }
}
