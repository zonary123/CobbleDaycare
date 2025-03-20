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
import com.kingpixel.cobbleutils.CobbleUtils;
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

  @Override
  public void applyEgg(ServerPlayerEntity player, Pokemon male, Pokemon female, Pokemon egg, List<Pokemon> parents, Pokemon firstEvolution) {
    List<String> moves = new ArrayList<>(getMoves(male));
    moves.addAll(getMoves(female));

    List<String> names = new ArrayList<>();
    for (MoveTemplate eggMove : firstEvolution.getForm().getMoves().getEggMoves()) {
      if (moves.contains(eggMove.getName())) {
        names.add(eggMove.getName());
      }
    }

    if (!names.isEmpty()) {
      JsonArray jsonArray = new JsonArray();
      names.forEach(jsonArray::add);

      JsonObject jsonObject = new JsonObject();
      jsonObject.add("moves", jsonArray);

      egg.getPersistentData().putString(TAG, jsonObject.toString());
    }
  }

  @Override
  public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
    String moves = egg.getPersistentData().getString(TAG);
    if (moves != null && !moves.isEmpty()) {
      try {
        // Parsear el JSON string como un JsonObject
        JsonObject jsonObject = JsonParser.parseString(moves).getAsJsonObject();

        // Obtener el JsonArray bajo la clave "moves"
        JsonArray jsonArray = jsonObject.getAsJsonArray("moves");
        for (JsonElement element : jsonArray) {
          MoveTemplate moveTemplate = Moves.INSTANCE.getByName(element.getAsString());
          if (moveTemplate == null) continue;
          Move move = moveTemplate.create();
          JsonObject moveJson = move.saveToJSON(new JsonObject());
          BenchedMove benchedMove = BenchedMove.Companion.loadFromJSON(moveJson);
          egg.getBenchedMoves().add(benchedMove);
        }
      } catch (Exception e) {
        CobbleUtils.LOGGER.error("Error to process JSON ARRAY: " + e.getMessage());
      }
    }
    egg.getPersistentData().remove(TAG);
  }

  @Override public void commandCreateEgg(ServerPlayerEntity player, Pokemon pokemon) {

  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "egg_moves";
  }

  @Override public String replace(String text) {
    return text
      .replace("%eggmoves%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }
}
