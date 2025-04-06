package com.kingpixel.cobbledaycare.migrate;

import com.google.gson.reflect.TypeToken;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.Utils;

import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
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
      File oldData = Utils.getAbsolutePath(PATH);
      File backupFolder = Utils.getAbsolutePath(CobbleDaycare.PATH_OLD_DATA);
      File newFolder = Utils.getAbsolutePath(CobbleDaycare.PATH_DATA);

      if (!newFolder.exists()) {
        newFolder.mkdirs();
      }
      // Crear carpeta de respaldo si no existe
      if (!backupFolder.exists()) {
        backupFolder.mkdirs();
      }

      // Migrar datos desde oldData
      migrateData(oldData, backupFolder);

      // Migrar datos desde backupFolder
      migrateData(backupFolder, backupFolder);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private static void migrateData(File sourceFolder, File backupFolder) {
    if (sourceFolder.exists()) {
      File[] files = sourceFolder.listFiles();
      if (files != null) {
        for (File file : files) {
          String fileName = file.getName().replace(".json", "");
          if (file.getName().endsWith(".json")) {
            File newFile = Utils.getAbsolutePath(CobbleDaycare.PATH_DATA + fileName + ".json");
            try {
              // Crear las carpetas necesarias para el nuevo archivo
              File parentDir = newFile.getParentFile();
              if (!parentDir.exists()) {
                parentDir.mkdirs();
              }

              if (!newFile.exists()) {
                try (FileReader reader = new FileReader(file, StandardCharsets.UTF_8)) {
                  List<OldPlot> oldPlots = Utils.newWithoutSpacingGson().fromJson(reader, new TypeToken<List<OldPlot>>() {
                  }.getType());
                  UserInformation userInformation = new UserInformation();
                  userInformation.setPlayerUUID(UUID.fromString(fileName));
                  for (OldPlot oldPlot : oldPlots) {
                    userInformation.getPlots().add(oldPlot.toNewPlot());
                  }
                  String data = Utils.newWithoutSpacingGson().toJson(userInformation);
                  if (Utils.writeFileSync(newFile, data)) {
                    CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Delete File: " + file.getAbsolutePath());
                    file.delete();
                  } else {
                    CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID, "Error writing file: " + newFile.getAbsolutePath());
                    Files.copy(file.toPath(), new File(backupFolder, file.getName()).toPath(), StandardCopyOption.REPLACE_EXISTING);
                  }
                }
              } else {
                // Borrar el archivo original si ya existe en la nueva carpeta
                file.delete();
              }
            } catch (Exception e) {
              e.printStackTrace();
            }
          }
        }
      }
    }
  }
}