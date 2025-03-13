package com.kingpixel.cobbledaycare.migrate;

import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.util.Utils;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Author: Carlos Varas Alonso - 13/03/2025 0:46
 */
public class Migrate {
  private static final String PATH = "/config/cobbleutils/breed/data/";

  public static void migrate() {
    try {
      File folder = Utils.getAbsolutePath(PATH);
      File backupFolder = Utils.getAbsolutePath(CobbleDaycare.PATH_OLD_DATA);

      // Create backup folder if it doesn't exist
      if (!backupFolder.exists()) {
        backupFolder.mkdirs();
      }

      if (folder.exists()) {
        File[] files = folder.listFiles();
        if (files != null) {
          for (File file : files) {
            String fileName = file.getName().replace(".json", "");
            if (file.getName().endsWith(".json")) {
              try (FileReader reader = new FileReader(file)) {
                List<OldPlot> oldPlots = Utils.newWithoutSpacingGson().fromJson(reader, new TypeToken<List<OldPlot>>() {
                }.getType());
                UserInformation userInformation = new UserInformation();
                userInformation.setPlayerUUID(UUID.fromString(fileName));
                for (OldPlot oldPlot : oldPlots) {
                  userInformation.getPlots().add(oldPlot.toNewPlot());
                }
                String data = Utils.newWithoutSpacingGson().toJson(userInformation);
                Utils.writeFileAsync(Utils.getAbsolutePath(CobbleDaycare.PATH_DATA + fileName + ".json"), data);
              }

              // Move the old file to the backup folder
              Files.move(file.toPath(), new File(backupFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
          }
        }
        folder.delete();
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}