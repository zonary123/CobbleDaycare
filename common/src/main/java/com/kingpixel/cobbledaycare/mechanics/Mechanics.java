package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.Gson;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
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

  public Mechanics getInstance() {
    return readFromFile(this.getClass());
  }


  public abstract void validateData();

  public abstract String fileName();

  public abstract String replace(String text);

  public abstract void applyEgg(EggBuilder eggBuilder);

  public abstract void applyHatch(ServerPlayerEntity player, Pokemon egg);

  public abstract void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg);

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