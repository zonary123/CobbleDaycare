package com.kingpixel.cobbledaycare;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbledaycare.commands.CommandTree;
import com.kingpixel.cobbledaycare.config.Config;
import com.kingpixel.cobbledaycare.config.Language;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.models.Incense;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbledaycare.models.mechanics.*;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.api.PermissionApi;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import kotlin.Unit;
import net.minecraft.server.MinecraftServer;
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
  public static MinecraftServer server;
  public static Config config = new Config();
  public static Language language = new Language();
  public static List<Mechanics> mechanics = new ArrayList<>();
  public static List<Incense> incenses = new ArrayList<>();
  public static Task task;

  public static void init() {
    load();
    events();
  }

  private static void load() {
    files();
    DatabaseClientFactory.createDatabaseClient(config.getDataBase());
    tasks();
  }

  private static void tasks() {
    long cooldown = 60 * 20;
    if (task != null) task.setExpired();
    task = Task.builder()
      .execute(() -> {
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
          boolean update = false;
          UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
          for (Plot plot : userInformation.getPlots()) {
            if (plot.checkEgg(player, userInformation) && !update) update = true;
          }
          DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
          fixPlayer(player);
        }
      })
      .interval(cooldown)
      .build();
  }

  private static void files() {
    config.init();
    language.init();
    incenses.clear();
    mechanics.clear();
    mechanics.add(new DayCarePokemon());
    mechanics.add(new DayCareForm());
    mechanics.add(new DaycareIvs());
    mechanics.add(new DayCareMirrorHerb());
    mechanics.add(new DayCareNature());
    mechanics.add(new DayCareShiny());
    mechanics.add(new DayCarePokeBall());
    mechanics.add(new DayCareAbility());
    mechanics.add(new DayCareMoves());
    mechanics.add(new DayCareCountry());
  }

  private static void events() {
    files();

    CommandRegistrationEvent.EVENT.register((dispatcher, registry, selection) -> {
      CommandTree.register(dispatcher, registry);
    });

    LifecycleEvent.SERVER_STARTED.register(minecraftServer -> {
      server = minecraftServer;
      load();
      for (int i = 0; i < config.getSlotPlots().size(); i++) {
        PermissionApi.hasPermission(server.getCommandSource(), "cobbledaycare.plot." + i + 1, 4);
      }
    });

    PlayerEvent.PLAYER_JOIN.register(player -> {
      countryPlayer(player);
      fixPlayer(player);
      UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
      boolean update = false;
      for (Plot plot : userInformation.getPlots()) {
        if (plot.checkEgg(player, userInformation) && !update) update = true;
      }
      if (update) DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
    });

    PlayerEvent.PLAYER_QUIT.register(player -> DatabaseClientFactory.INSTANCE.removeIfNecessary(player));

    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, evt -> {
      breedable(evt.getPokemon());
      return Unit.INSTANCE;
    });
  }

  private static void fixPlayer(ServerPlayerEntity player) {
    var countryInfo = getCountry(player);
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getParty(player)) {
      breedable(pokemon);
      fixCountryInfo(pokemon, countryInfo);
    }
    for (Pokemon pokemon : Cobblemon.INSTANCE.getStorage().getPC(player)) {
      breedable(pokemon);
      fixCountryInfo(pokemon, countryInfo);
    }
  }

  private static void fixCountryInfo(Pokemon pokemon, UserInfo countryInfo) {
    if (countryInfo == null) return;
    if (!pokemon.getPersistentData().contains(DayCareCountry.TAG)) {
      pokemon.getPersistentData().putString(DayCareCountry.TAG, countryInfo.country());
    }
  }

  public static void breedable(Pokemon pokemon) {
    var nbt = pokemon.getPersistentData();
    if (!nbt.getBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG)) {
      nbt.putBoolean(CobbleUtilsTags.BREEDABLE_TAG, !CobbleDaycare.config.getBlackList().isBlackListed(pokemon));
    }
  }

  public static void countryPlayer(ServerPlayerEntity player) {
    if (playerCountry.get(player.getUuid()) != null) return;

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

  public static UserInfo getCountry(ServerPlayerEntity player) {
    return playerCountry.get(player.getUuid());
  }

  public record UserInfo(String country, String countryCode, String language) {
  }
}
