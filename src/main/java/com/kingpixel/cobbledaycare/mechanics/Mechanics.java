package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbledaycare.models.HatchBuilder;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Author: Carlos Varas Alonso - 31/01/2025 0:25
 */
@Data
public abstract class Mechanics {
  private boolean active = true;

  /*
   * This method is used to get the instance of the class that extends Mechanics.
   * It reads the JSON file and returns the instance.
   */
  public Mechanics getInstance() {
    return readFromFile(this.getClass());
  }

  /*
   * This method is used to validate the data of the class that extends Mechanics.
   * It checks if the data is valid and throws an exception if it is not.
   */
  public abstract void validateData();

  /*
   * This method is used to get the file name of the class that extends Mechanics.
   * It returns the file name without the .json extension.
   */
  public abstract String fileName();

  /*
   * This method is used to get the name of the class that extends Mechanics.
   * It returns the class name without the package name.
   */
  public abstract String replace(String text, ServerPlayerEntity player);

  /*
   * This method is used to apply the egg mechanics to the egg builder.
   * It takes an EggBuilder object as a parameter and applies the mechanics to it.
   */
  public abstract void applyEgg(EggBuilder eggBuilder);

  /*
   * This method is used to apply the hatch mechanics to the egg.
   * It takes a ServerPlayerEntity object and a Pokemon object as parameters and applies the mechanics to it.
   */
  public abstract void applyHatch(HatchBuilder builder);

  /*
   * This method is used to create an egg for the player.
   * It takes a ServerPlayerEntity object, a Pokemon object, and an egg Pokemon object as parameters and creates the egg.
   */
  public abstract void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg);

  /*
   * This method is used to get the egg info.
   * It takes a String and an NbtCompound object as parameters and returns the egg info as a String.
   */
  public abstract String getEggInfo(String s, NbtCompound nbt);

  public <T> T readFromFile(Class<T> clazz) {
    try {
      Gson gson = Utils.newGson();
      String filePath = Utils.getAbsolutePath(CobbleDaycare.PATH_MODULES + fileName() + ".json").getAbsolutePath();

      File file = new File(filePath);


      if (!file.exists()) {
        file.getParentFile().mkdirs();
        T instance = clazz.getDeclaredConstructor().newInstance();
        ((Mechanics) instance).validateData();
        writeToFile(instance, filePath);

        return instance;
      }

      try (FileReader reader = new FileReader(filePath)) {
        T instance = gson.fromJson(reader, clazz);
        ((Mechanics) instance).validateData();
        writeToFile(instance, filePath);
        return instance;
      } catch (IOException e) {
        e.printStackTrace();
        return null;
      }
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public <T> void writeToFile(T object, String filePath) {
    Gson gson = Utils.newGson();
    try {
      File file = new File(filePath);
      File parentDir = file.getParentFile();
      if (parentDir != null && !parentDir.exists()) {
        parentDir.mkdirs();
      }
      try (FileWriter writer = new FileWriter(file)) {
        gson.toJson(object, writer);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }


}