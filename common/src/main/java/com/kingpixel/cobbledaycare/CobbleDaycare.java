package com.kingpixel.cobbledaycare;

import ca.landonjw.gooeylibs2.api.tasks.Task;
import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.properties.CustomPokemonProperty;
import com.cobblemon.mod.common.battles.actor.PlayerBattleActor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
  public static ExecutorService DAYCARE_EXECUTOR = Executors.newFixedThreadPool(4, new ThreadFactoryBuilder()
    .setDaemon(true)
    .setNameFormat("CobbleDaycare-Executor-%d")
    .build());
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
    /*if (task != null) task.setExpired();
    long cooldown = 20L * 60;
    task = Task.builder()
      .execute(() -> {
        var players = server.getPlayerManager().getPlayerList();
        CompletableFuture.runAsync(() -> {
            for (ServerPlayerEntity player : players) {
              if (player == null) continue;
              boolean update;
              UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
              update = userInformation.fix(player);
              if (update) DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
              fixPlayer(player);
            }
          }, DAYCARE_EXECUTOR)
          .orTimeout(30, TimeUnit.SECONDS)
          .exceptionally(e -> {
            CobbleUtils.LOGGER.error(MOD_ID, "Error Task Daycare.");
            e.printStackTrace();
            return null;
          });

      })
      .infinite()
      .interval(cooldown)
      .build();*/
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

    CobblemonEvents.BATTLE_STARTED_PRE.subscribe(Priority.HIGHEST, evt -> {
      var actors = evt.getBattle().getActors();
      for (BattleActor actor : actors) {
        if (actor instanceof PlayerBattleActor playerBattleActor) {
          var pokemons = playerBattleActor.getPokemonList();
          pokemons.removeIf(pokemon -> pokemon.getOriginalPokemon().getSpecies().showdownId().equals("egg"));
          if (pokemons.isEmpty()) evt.cancel();
        }
      }
      return Unit.INSTANCE;
    });

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
      long start = System.currentTimeMillis();
      CompletableFuture.runAsync(() -> {
          UserInformation userInformation = DatabaseClientFactory.INSTANCE.getUserInformation(player);
          boolean update = false;
          if (userInformation.getCountry() == null) {
            UserInfo info = playerCountry.computeIfAbsent(player.getUuid(), uuid -> fetchCountryInfo(player));
            if (info != null) {
              userInformation.setCountry(info.country());
              update = true;
            }
          }

          fixPlayer(player);

          int numPlots = 1;
          int size = CobbleDaycare.config.getSlotPlots().size();
          for (int i = 0; i < size; i++) {
            if (PermissionApi.hasPermission(player, Plot.plotPermission(i), 4)) {
              numPlots = i + 1;
            }
          }

          if (userInformation.check(numPlots, player)) update = true;
          if (userInformation.fix(player)) update = true;

          if (update) {
            DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
          }

          long end = System.currentTimeMillis();
          if (config.isDebug()) {
            CobbleUtils.LOGGER.info(MOD_ID, "Time to load player " + player.getName().getString() + ": " + (end - start) + "ms");
          }

        }, DAYCARE_EXECUTOR)
        .exceptionallyAsync(e -> {
          CobbleUtils.LOGGER.error(MOD_ID, "Error on player join: " + player.getName().getString());
          e.printStackTrace();
          return null;
        });
    });

    PlayerEvent.PLAYER_QUIT.register(player -> {
      CompletableFuture.runAsync(() -> {
        try {
          UserInformation userInfo = DatabaseClientFactory.INSTANCE.getUserInformation(player);
          if (userInfo.getCountry() == null) {
            UserInfo info = playerCountry.computeIfAbsent(player.getUuid(), uuid -> fetchCountryInfo(player));
            if (info != null) userInfo.setCountry(info.country());
          }
          DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInfo);
          DatabaseClientFactory.INSTANCE.removeIfNecessary(player);
        } catch (Exception e) {
          CobbleUtils.LOGGER.error(MOD_ID, "Error on player quit: " + player.getName().getString());
        }
      }, DAYCARE_EXECUTOR);
    });

    CobblemonEvents.POKEMON_CAPTURED.subscribe(Priority.HIGHEST, evt -> {
      fixBreedable(evt.getPokemon());
      return Unit.INSTANCE;
    });

    CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.HIGHEST, evt -> {
      Pokemon pokemon = evt.getPokemon();
      if (pokemon.getSpecies().showdownId().equals("egg")) {
        evt.cancel();
        return Unit.INSTANCE;
      }
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

  private static UserInfo fetchCountryInfo(ServerPlayerEntity player) {
    try {
      URL url = new URL(API_URL_IP + player.getIp());
      HttpURLConnection conn = (HttpURLConnection) url.openConnection();
      conn.setRequestMethod("GET");

      try (BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
        JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
        if (json.has("country")) {
          String country = json.get("country").getAsString();
          String countryCode = json.get("countryCode").getAsString();
          String language = switch (countryCode) {
            case "AR", "ES" -> "es";
            case "US", "GB", "AU" -> "en";
            default -> "en";
          };
          return new UserInfo(country, countryCode, language);
        }
      }
    } catch (Exception e) {
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.warn(MOD_ID, "Error fetching country for player " + player.getName().getString());
      }
    }
    return null;
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

  public static void setBreedable(Pokemon pokemon, boolean value) {
    pokemon.getPersistentData().putBoolean(CobbleUtilsTags.BREEDABLE_TAG, value);
  }

  public record UserInfo(String country, String countryCode, String language) {
  }
}
