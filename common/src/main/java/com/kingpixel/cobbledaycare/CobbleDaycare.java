package com.kingpixel.cobbledaycare;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.kingpixel.cobbledaycare.commands.CommandTree;
import com.kingpixel.cobbledaycare.config.Config;
import com.kingpixel.cobbledaycare.config.Language;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.*;
import com.kingpixel.cobbledaycare.migrate.Migrate;
import com.kingpixel.cobbledaycare.models.Plot;
import com.kingpixel.cobbledaycare.models.UserInformation;
import com.kingpixel.cobbledaycare.properties.BreedablePropertyType;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.Utils;
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
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 23/07/2024 9:24
 */
public class CobbleDaycare {
  public static final String MOD_ID = "cobbledaycare";
  public static final String PATH = "/config/cobbledaycare/";
  public static final String PATH_LANGUAGE = PATH + "lang/";
  public static final String PATH_DATA = PATH + "data/";
  public static final String PATH_OLD_DATA = PATH + "old_data/";
  public static final String PATH_MODULES = PATH + "modules/";
  public static final String TAG_SPAWNED = "spawned";
  private static final String API_URL_IP = "http://ip-api.com/json/";
  private static final Map<UUID, UserInfo> playerCountry = new HashMap<>();
  public static MinecraftServer server;
  public static Config config = new Config();
  public static Language language = new Language();
  public static Task task;
  public static List<Mechanics> mechanics = new ArrayList<>();
  private static HttpURLConnection conn;

  public static void init() {
    load();
    events();
  }


  public static void load() {
    CobbleUtils.info(MOD_ID, "1.0.0", "https://github.com/zonary123/CobbleDaycare");
    files();
    DatabaseClientFactory.userPlots.clear();
    DatabaseClientFactory.createDatabaseClient(config.getDataBase());
    tasks();
    Migrate.migrate();
  }

  private static void tasks() {
    if (task != null) task.setExpired();
    long cooldown = 20L * 30;
    task = Task.builder()
      .execute(() -> {
        CompletableFuture.runAsync(() -> {
            long start = System.currentTimeMillis();
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
              if (player == null) continue;
              boolean update = false;
              UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
              update = userInformation.fix(player);
              if (update) DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
              fixPlayer(player);
            }
            long end = System.currentTimeMillis();
            if (config.isDebug())
              CobbleUtils.LOGGER.info(MOD_ID, "Time to update players: " + (end - start) + "ms");
          })
          .orTimeout(30, TimeUnit.SECONDS)
          .exceptionally(e -> {
            CobbleUtils.LOGGER.error(MOD_ID, "Error Task Daycare.");
            e.printStackTrace();
            return null;
          });
      })
      .infinite()
      .interval(cooldown)
      .build();
  }

  private static void files() {
    config.init();
    language.init();
    mechanics.clear();

    mechanics.addAll(
      List.of(
        new DayCarePokemon().getInstance(),
        new DayCareForm().getInstance(),
        new DayCareAbility().getInstance(),
        new DayCareEggMoves().getInstance(),
        new DayCareMirrorHerb().getInstance(),
        new DayCareNature().getInstance(),
        new DayCareCountry().getInstance(),
        new DayCareShiny().getInstance(),
        new DayCarePokeBall().getInstance(),
        new DaycareIvs().getInstance(),
        new DayCareInciense().getInstance()
      )
    );
    mechanics.removeIf(Objects::isNull);
    mechanics.removeIf(mechanic -> {
      if (mechanic instanceof DayCarePokemon || mechanic instanceof DayCareForm) {
        return false;
      }
      return !mechanic.isActive();
    });

    List<String> activeMechanics = new ArrayList<>();
    for (Mechanics mechanic : mechanics) {
      activeMechanics.add(mechanic.fileName());
    }
    CobbleUtils.LOGGER.info(MOD_ID, "Active mechanics:\n- " + String.join("\n- ", activeMechanics));
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
        PermissionApi.hasPermission(server.getCommandSource(), Plot.plotPermission(i), 4);
      }
      CustomPokemonProperty.Companion.register(BreedablePropertyType.getInstance());
    });

    PlayerEvent.PLAYER_JOIN.register(player -> {
      CompletableFuture.runAsync(() -> {
          long start = System.currentTimeMillis();
          UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
          if (userInformation.getCountry() == null) {
            countryPlayer(player);
            var countryInfo = getCountry(player);
            if (countryInfo != null) {
              userInformation.setCountry(countryInfo.country());
            }
          }
          fixPlayer(player);
          int numPlots = 0;
          int size = CobbleDaycare.config.getSlotPlots().size();
          for (int i = 0; i < size; i++) {
            if (PermissionApi.hasPermission(player, Plot.plotPermission(i), 4)) {
              numPlots = i + 1;
            }
          }
          if (numPlots == 0) numPlots = 1;
          boolean update = userInformation.check(numPlots, player);
          if (userInformation.fix(player)) update = true;
          if (update) DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
          long end = System.currentTimeMillis();
          if (config.isDebug())
            CobbleUtils.LOGGER.info(MOD_ID, "Time to load player " + player.getName().getString() + ": " + (end - start) + "ms");
        })
        .orTimeout(5, TimeUnit.SECONDS)
        .exceptionally(e -> {
          CobbleUtils.LOGGER.error(MOD_ID, "Error Event Player Join Daycare " + player.getName().getString() + ".");
          return null;
        });
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      var userinfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
      if (userinfo.getCountry() == null) {
        var countryInfo = getCountry(player);
        if (countryInfo != null) {
          userinfo.setCountry(countryInfo.country());
        }
      }
      DatabaseClientFactory.INSTANCE.updateUserInformation(player, userinfo);
      DatabaseClientFactory.INSTANCE.removeIfNecessary(player);
    });


    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, evt -> {
      fixBreedable(evt.getPokemon());
      return Unit.INSTANCE;
    });


    CobblemonEvents.POKEMON_ENTITY_SPAWN.subscribe(Priority.LOWEST, evt -> {
      if (!config.isSpawnEggWorld()) return Unit.INSTANCE;
      var pokemonEntity = evt.getEntity();
      var pokemon = pokemonEntity.getPokemon();
      if (pokemon.getSpecies().showdownId().equals("egg")) return Unit.INSTANCE;
      boolean rarity = Utils.RANDOM.nextInt(config.getRaritySpawnEgg()) == 0;
      if (rarity) {
        var egg = PokemonProperties.Companion.parse("egg").create();
        for (Mechanics mechanic : mechanics) {
          mechanic.createEgg(null, pokemon, egg);
        }
        egg.getPersistentData().putBoolean(TAG_SPAWNED, true);
        pokemonEntity.setMovementSpeed(0);
        pokemonEntity.setAiDisabled(true);
        pokemonEntity.setPokemon(egg);
      }
      return Unit.INSTANCE;
    });

  }

  private static void fixPlayer(ServerPlayerEntity player) {
    var userinfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
    var party = Cobblemon.INSTANCE.getStorage().getParty(player);
    for (Pokemon pokemon : party) {
      fixBreedable(pokemon);
      fixCountryInfo(pokemon, userinfo.getCountry());
      if (config.isFixIlegalAbilities())
        PokemonUtils.isLegalAbility(pokemon);
    }

    var pc = Cobblemon.INSTANCE.getStorage().getPC(player);
    for (Pokemon pokemon : pc) {
      fixBreedable(pokemon);
      fixCountryInfo(pokemon, userinfo.getCountry());
      if (config.isFixIlegalAbilities())
        PokemonUtils.isLegalAbility(pokemon);
    }
  }

  private static void fixCountryInfo(Pokemon pokemon, String country) {
    if (country == null) return;
    if (!pokemon.getPersistentData().contains(DayCareCountry.TAG)) {
      pokemon.getPersistentData().putString(DayCareCountry.TAG, country);
    }
  }

  public static void fixBreedable(Pokemon pokemon) {
    var nbt = pokemon.getPersistentData();
    boolean isNotBreedable = Plot.isNotBreedable(pokemon);
    if (!nbt.getBoolean(CobbleUtilsTags.BREEDABLE_BUILDER_TAG)) setBreedable(pokemon, !isNotBreedable);
    if (isNotBreedable) setBreedable(pokemon, false);
  }

  public static void countryPlayer(ServerPlayerEntity player) {
    if (playerCountry.get(player.getUuid()) != null) return;


    CompletableFuture.runAsync(() -> {
        try {
          if (conn == null) {
            URL url = new URL(API_URL_IP + player.getIp());
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
          }

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
          }

        } catch (Exception e) {
          CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID,
            "Error while getting country info for player " + player.getName().getString() + ".");
        }
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        return null;
      });
  }

  public static UserInfo getCountry(ServerPlayerEntity player) {
    return playerCountry.get(player.getUuid());
  }

  public static void setBreedable(Pokemon pokemon, boolean value) {
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_TAG, value);
  }

  public record UserInfo(String country, String countryCode, String language) {
  }
}
