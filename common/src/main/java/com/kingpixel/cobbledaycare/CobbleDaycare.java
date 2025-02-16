package com.kingpixel.cobbledaycare;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbledaycare.commands.CommandTree;
import com.kingpixel.cobbledaycare.config.Config;
import com.kingpixel.cobbledaycare.config.Language;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Incense;
import com.kingpixel.cobbledaycare.models.mecanics.DaycareIvs;
import com.kingpixel.cobbledaycare.models.mecanics.DaycareNature;
import com.kingpixel.cobbledaycare.models.mecanics.Mechanics;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author Carlos Varas Alonso - 23/07/2024 9:24
 */
public class CobbleDaycare {
  public static final String MOD_ID = "cobbledaycare";
  public static final String PATH = "/config/cobbledaycare/";
  public static final String PATH_LANGUAGE = PATH + "lang/";
  public static final String PATH_DATA = PATH + "data/";
  public static final String PATH_INCENSE = PATH + "incenses/";
  private static final String API_URL_IP = "http://ip-api.com/json/";
  private static final Map<UUID, UserInfo> playerCountry = new HashMap<>();
  public static Config config = new Config();
  public static Language language = new Language();
  public static List<Mechanics> mechanics = new ArrayList<>();
  public static List<Incense> incenses = new ArrayList<>();


  public static void init() {
    load();
    events();
  }

  private static void load() {
    files();
    DatabaseClientFactory.createDatabaseClient(config.getDataBase());
  }

  private static void files() {
    config.init();
    language.init();
    incenses.clear();
    Incense.init();
    mechanics.clear();
    mechanics.add(new DaycareIvs());
    mechanics.add(new DaycareNature());
  }

  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(instance -> {
      load();
    });

    LifecycleEvent.SERVER_STOPPING.register(instance -> {

    });

    PlayerEvent.PLAYER_JOIN.register(player -> {
      countryPlayer(player);
      DatabaseClientFactory.databaseClient.getUserInformation(player);
    });


  }

  private static void countryPlayer(ServerPlayerEntity player) {
    if (playerCountry.get(player.getUuid()) != null) return; // Verifica si ya se obtuvo la información del jugador

    CompletableFuture.runAsync(() -> {
      try {
        URL url = new URL(API_URL_IP + player.getIp());
        // Establece la conexión HTTP con la API
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        // Usar try-with-resources para asegurar el cierre de BufferedReader
        try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
          // Parsear la respuesta en un JsonObject
          JsonObject json = JsonParser.parseReader(in).getAsJsonObject();

          // Verifica si el JSON tiene la información del país
          if (json.has("country")) {
            String country = json.get("country").getAsString();
            String countryCode = json.get("countryCode").getAsString();

            // Determina el idioma según el código del país
            String language = switch (countryCode) {
              case "AR", "ES" -> "es";
              case "US", "GB", "AU" -> "en";
              default -> "en"; // Idioma por defecto
            };

            // Crea y almacena la información del usuario
            UserInfo userInfo = new UserInfo(country, countryCode, language);
            playerCountry.put(player.getUuid(), userInfo);
          }
        } finally {
          conn.disconnect(); // Desconectar la conexión HTTP
        }

      } catch (Exception e) {
        // Maneja cualquier excepción que ocurra durante la solicitud
        e.printStackTrace();
      }
    });
  }

  public record UserInfo(String country, String countryCode, String language) {
  }
}
