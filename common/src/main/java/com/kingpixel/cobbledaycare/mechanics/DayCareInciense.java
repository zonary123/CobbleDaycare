package com.kingpixel.cobbledaycare.mechanics;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.EggBuilder;
import com.kingpixel.cobbledaycare.models.Incense;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.api.ItemsMod;
import com.kingpixel.cobbleutils.util.Utils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Carlos Varas Alonso - 15/03/2025 20:23
 */
@EqualsAndHashCode(callSuper = true) @Data
public class DayCareInciense extends Mechanics {
  public static String TAG_INCENSE = "incense";
  public static List<Incense> incenses = new ArrayList<>();
  private static String path = CobbleDaycare.PATH_MODULES + "incense/";
  private static DayCareInciense instance;

  public DayCareInciense() {
    setActive(true);
  }

  public static DayCareInciense INSTANCE() {
    if (instance == null) {
      instance = new DayCareInciense();
    }
    return instance;
  }

  @Override public DayCareInciense getInstance() {
    instance = this.readFromFile(getClass());
    instance.init();
    for (Incense incense : DayCareInciense.incenses) {
      ItemsMod.addItem(CobbleDaycare.MOD_ID, incense.getId(), incense.getItemStackIncense(1));
    }
    return instance;
  }


  public Incense getIncense(String id) {
    for (Incense incense : DayCareInciense.incenses) {
      if (incense.getId().equals(id)) {
        return incense;
      }
    }
    return null;
  }

  public void init() {
    File folder = Utils.getAbsolutePath(path);
    if (!folder.exists()) {
      folder.mkdirs();
    }

    File[] files = folder.listFiles();
    if (files == null || files.length == 0) {
      createDefaultIncense(folder, Incense.defaultIncenses());
      files = folder.listFiles(); // Re-read the files after creating defaults
    }
    incenses.clear();
    for (File file : files) {
      if (file.getName().endsWith(".json")) {
        try {
          String data = Utils.readFileSync(file); // Read the file content
          Incense incense = Utils.newGson().fromJson(data, Incense.class);
          if (incense != null) {
            incense.setId(file.getName().replace(".json", ""));
            incenses.add(incense);
            Utils.writeFileAsync(file, Utils.newGson().toJson(incense));
          }
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }

  }

  private void createDefaultIncense(File folder, List<Incense> incenses) {
    for (Incense incense : incenses) {
      String data = Utils.newGson().toJson(incense);
      Utils.writeFileAsync(new File(folder.getAbsolutePath() + "/" + incense.getId() + ".json"), data).join();
    }
  }

  public Pokemon applyIncense(Pokemon pokemon) {
    if (!isActive()) return null;
    String species;
    for (Incense incense : incenses) {
      species = incense.getChild(pokemon);
      if (species != null) return PokemonProperties.Companion.parse(species).create();
    }
    return null;
  }

  @Override public void validateData() {
  }

  @Override public String fileName() {
    return "incense";
  }

  @Override public String replace(String text) {
    return text
      .replace("%incense%", isActive() ? CobbleUtils.language.getYes() : CobbleUtils.language.getNo());
  }

  @Override
  public void applyEgg(EggBuilder builder) {
  }

  @Override public void applyHatch(ServerPlayerEntity player, Pokemon egg) {
  }

  @Override public void createEgg(ServerPlayerEntity player, Pokemon pokemon, Pokemon egg) {
  }

  @Override public String getEggInfo(String s, NbtCompound nbt) {
    return s;
  }
}
