package com.kingpixel.cobbledaycare;

import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Automatically installs and enables the CobbleDaycare datapack using a recursive JAR copy.
 */
public final class DataPackDaycare {

  private DataPackDaycare() {
    // Utility class
  }

  /**
   * Installs the datapack into the world's datapacks folder if it is not already present,
   * and enables it on the server.
   *
   * @param server       Minecraft server instance
   * @param datapackName Name of the datapack folder inside resources (e.g., "cobbledaycare_datapack")
   */
  public static void installDatapack(MinecraftServer server, String datapackName) {
    Path worldDatapacks = server.getSavePath(WorldSavePath.DATAPACKS);
    Path target = worldDatapacks.resolve(datapackName);

    if (Files.exists(target)) return;

    try {
      Files.createDirectories(target);

      // Copy all datapack files from mod resources
      copyDatapackFromJar(datapackName, target, true);

      // Enable datapack in the server
      server.getDataPackManager().enable("file/" + datapackName);
      server.reloadResources(server.getDataPackManager().getEnabledIds());

      System.out.println("[CobbleDaycare] Datapack installed and enabled successfully");
    } catch (Exception e) {
      System.err.println("[CobbleDaycare] Failed to install datapack");
      e.printStackTrace();
    }
  }

  /**
   * Copies all files of a folder from inside the mod JAR to the target path.
   *
   * @param datapackName Folder name inside resources
   * @param targetPath   Destination path in the world's datapacks folder
   * @param exceptions   Whether to print exceptions
   */
  private static void copyDatapackFromJar(String datapackName, Path targetPath, boolean exceptions) {
    try {
      // Obtain the URL of the resource inside the mod
      URL url = CobbleDaycare.class.getResource("/" + datapackName);
      if (url == null) {
        System.err.println("[CobbleDaycare] Datapack resources not found inside JAR.");
        return;
      }

      URI uri = url.toURI();

      if ("jar".equals(uri.getScheme())) {
        // Open the JAR
        FileSystem fs;
        try {
          fs = FileSystems.getFileSystem(uri);
        } catch (FileSystemNotFoundException e) {
          fs = FileSystems.newFileSystem(uri, Map.of());
        }

        Path jarPath = fs.getPath("/" + datapackName);
        copyFolder(jarPath, targetPath, exceptions);

        if (fs.isOpen()) fs.close();

      } else {
        // Dev environment (IDE)
        Path path = Paths.get(uri);
        copyFolder(path, targetPath, exceptions);
      }

    } catch (Exception e) {
      if (exceptions) e.printStackTrace();
    }
  }

  /**
   * Recursively copies all files and directories from source to target.
   *
   * @param source     Source path
   * @param target     Destination path
   * @param exceptions Whether to print exceptions
   */
  private static void copyFolder(Path source, Path target, boolean exceptions) {
    try (Stream<Path> paths = Files.walk(source)) {
      paths.forEach(path -> {
        try {
          Path relative = source.relativize(path);
          Path destination = target.resolve(relative.toString());

          if (Files.isDirectory(path)) {
            Files.createDirectories(destination);
          } else {
            Files.copy(path, destination, StandardCopyOption.REPLACE_EXISTING);
          }
        } catch (Exception e) {
          if (exceptions) e.printStackTrace();
        }
      });
    } catch (Exception e) {
      if (exceptions) e.printStackTrace();
    }
  }

}
