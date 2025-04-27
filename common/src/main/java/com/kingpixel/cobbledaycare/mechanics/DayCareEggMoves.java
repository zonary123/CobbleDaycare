package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.moves.BenchedMove;
import com.cobblemon.mod.common.api.moves.Move;
import com.cobblemon.mod.common.api.moves.MoveTemplate;
import com.cobblemon.mod.common.api.moves.Moves;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 11/03/2025 9:09
 */
public class DayCareEggMoves extends Mechanics {
  public static final String TAG = "moves";

  private static List<String> getMoves(Pokemon pokemon) {
    List<String> s = new ArrayList<>();
    pokemon.getMoveSet().forEach(move -> s.add(move.getName()));
    pokemon.getBenchedMoves().forEach(move -> s.add(move.getMoveTemplate().getName()));
    return s;
  }

  private static List<String> extractMoveNamesFromJson(String json) {
    if (json == null || json.isEmpty()) return new ArrayList<>();
    List<String> moveNames = new ArrayList<>();

    try {
      JsonObject jsonObject = JsonParser.parseString(json).getAsJsonObject();
      JsonArray jsonArray = jsonObject.getAsJsonArray("moves");
      for (JsonElement element : jsonArray) {
        moveNames.add(element.getAsString());
      }
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error parsing move names from JSON: " + e.getMessage());
    }

    return moveNames;
  }

  @Override
  public void applyEgg(EggBuilder builder) {
    List<String> moves = new ArrayList<>(getMoves(builder.getMale()));
    moves.addAll(getMoves(builder.getFemale()));

    List<String> names = new ArrayList<>();
    for (MoveTemplate eggMove : builder.getFirstEvolution().getForm().getMoves().getEggMoves()) {
      if (moves.contains(eggMove.getName())) {
        names.add(eggMove.getName());
      }
    }

    if (!names.isEmpty()) {
      JsonArray jsonArray = new JsonArray();
      names.forEach(jsonArray::add);

      JsonObject jsonObject = new JsonObject();
      jsonObject.add("moves", jsonArray);

      builder.getEgg().getPersistentData().putString(TAG, jsonObject.toString());
    }
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String json = egg.getPersistentData().getString(TAG);
    var moves = extractMoveNamesFromJson(json);

    for (String s : moves) {
      MoveTemplate moveTemplate = Moves.INSTANCE.getByName(s);
      if (moveTemplate == null) continue;
      Move move = moveTemplate.create();
      JsonObject moveJson = move.saveToJSON(new JsonObject());
      BenchedMove benchedMove = BenchedMove.Companion.loadFromJSON(moveJson);
      egg.getBenchedMoves().add(benchedMove);
    }

    egg.getPersistentData().remove(TAG);
  }

  @Override
  public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
  }

  @Override
  public String getEggInfo(String s, NbtCompound nbt) {
    List<String> moveNames = extractMoveNamesFromJson(nbt.getString(TAG));
    StringBuilder movesString = new StringBuilder();
    for (String moveName : moveNames) {
      movesString.append("<lang:cobblemon.move.").append(moveName).append(">").append(", ");
    }
    if (!movesString.isEmpty()) {
      movesString.setLength(movesString.length() - 2); // Remove the last comma and space
      movesString.append(".");
    }
    return s.replace("%eggmoves%", movesString.isEmpty() ? CobbleUtils.language.getNone() : movesString.toString());
  }

  @Override
  public void validateData() {
  }

  @Override
  public String fileName() {
    return "egg_moves";
  }

  @Override
  public String replace(String text, ServerPlayerEntity player) {
    return text
      .replace("%eggmoves%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }
}