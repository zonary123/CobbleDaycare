package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.Model.CobbleUtilsTags;
import com.kingpixel.cobbleutils.api.PermissionApi;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.PokemonUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import lombok.Data;
import lombok.ToString;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 31/01/2025 1:17
 */
@Data
@ToString
public class Plot {
  private Pokemon male;
  private Pokemon female;
  private List<Pokemon> eggs;
  private long timeToHatch;
  private long canOpen;

  public Plot() {
    this.male = null;
    this.female = null;
    this.eggs = new ArrayList<>();
    this.timeToHatch = 0;
    this.canOpen = 0;
  }


  public static boolean isNotBreedable(Pokemon pokemon) {
    return pokemon.getForm().getEggGroups().contains(EggGroup.UNDISCOVERED) || CobbleDaycare.config.getBlackList().isBlackListed(pokemon);
  }

  private boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  public boolean canBreed(Pokemon pokemon, SelectGender gender) {
    if (pokemon == null) return false;
    CobbleDaycare.fixBreedable(pokemon);
    if (isNotBreedable(pokemon)) return false;
    Pokemon other = getEmptyParent();
    boolean otherIsDitto = isDitto(other);
    boolean pokemonIsDitto = isDitto(pokemon);
    if (pokemonIsDitto && otherIsDitto) return CobbleDaycare.config.isDobbleDitto();
    if (!pokemon.getPersistentData().getBoolean(CobbleUtilsTags.BREEDABLE_TAG)) return false;
    Gender pokemonGender = pokemon.getGender();
    if (gender == SelectGender.MALE) {
      if (!pokemonGender.equals(Gender.MALE) && !pokemonGender.equals(Gender.GENDERLESS) && !otherIsDitto)
        return false;
    } else {
      if (!pokemonGender.equals(Gender.FEMALE) && !pokemonGender.equals(Gender.GENDERLESS) && !otherIsDitto)
        return false;
    }
    if (other == null) return true;
    if (other.getGender().equals(Gender.GENDERLESS)) {
      if (otherIsDitto) {
        return true;
      } else {
        return pokemonIsDitto;
      }
    }
    for (EggGroup eggGroup : pokemon.getForm().getEggGroups()) {
      if (other.getForm().getEggGroups().contains(eggGroup) || pokemon.getForm().getEggGroups().contains(EggGroup.DITTO))
        return true;
    }
    return false;
  }

  public boolean hasCooldownToOpen(ServerPlayerEntity player) {
    return canOpen > System.currentTimeMillis();
  }


  private void setTime(ServerPlayerEntity player) {
    if (hasTwoParents()) {
      long cooldown = PlayerUtils.getCooldown(CobbleDaycare.config.getCooldowns(), CobbleDaycare.config.getCooldown()
        , player);
      timeToHatch = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cooldown);
    } else {
      timeToHatch = 0;
    }
  }

  public void addFemale(ServerPlayerEntity player, Pokemon female) {
    this.female = female;
    if (!Cobblemon.INSTANCE.getStorage().getParty(player).remove(female)) {
      Cobblemon.INSTANCE.getStorage().getPC(player).remove(female);
    }
    setTime(player);
  }

  public void addMale(ServerPlayerEntity player, Pokemon male) {
    this.male = male;
    if (!Cobblemon.INSTANCE.getStorage().getParty(player).remove(male)) {
      Cobblemon.INSTANCE.getStorage().getPC(player).remove(male);
    }
    setTime(player);
  }

  public boolean hasEggs() {
    return !eggs.isEmpty();
  }

  public boolean hasTwoParents() {
    return male != null && female != null;
  }

  public boolean notParents() {
    return male == null && female == null;
  }

  public Pokemon getEmptyParent() {
    if (male == null && female == null) {
      return null;
    } else if (male == null) {
      return female;
    } else {
      return male;
    }
  }

  public boolean giveEggs(ServerPlayerEntity player) {
    if (!hasEggs()) return false;
    boolean update = false;
    List<Pokemon> remove = new ArrayList<>();
    for (Pokemon egg : eggs) {
      if (egg == null) continue;
      Cobblemon.INSTANCE.getStorage().getParty(player).add(egg);
      remove.add(egg);
    }
    if (!remove.isEmpty()) {
      eggs.removeAll(remove);
      update = true;
    }
    return update;
  }

  public boolean check(ServerPlayerEntity player) {
    return false;
  }

  private boolean hasCooldown(ServerPlayerEntity player) {
    return timeToHatch > System.currentTimeMillis();
  }

  public int limitEggs(ServerPlayerEntity player) {
    int limit = 1;
    for (Map.Entry<String, Integer> limitEgg : CobbleDaycare.config.getLimitEggs().entrySet()) {
      if (limit > limitEgg.getValue()) continue;
      if (PermissionApi.hasPermission(player, limitEgg.getKey(), 4)) {
        limit = limitEgg.getValue();
      }
    }
    return limit;
  }

  public boolean checkEgg(ServerPlayerEntity player, UserInformation userInformation) {
    try {
      boolean update = false;
      if (!hasTwoParents()) {
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.checkEgg: !hasTwoParents");
        }
        return false;
      }
      int sizeEggs = eggs.size();
      if (sizeEggs >= limitEggs(player)) {
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.checkEgg: limitEggs < sizeEggs");
        }
        if (userInformation.isNotifyLimitEggs()) {
          PlayerUtils.sendMessage(
            player,
            CobbleDaycare.language.getMessageLimitEggs()
              .replace("%plot%", userInformation.getPlots().indexOf(this) + ""),
            CobbleDaycare.language.getPrefix(),
            TypeMessage.CHAT
          );
        }
        return false;
      }
      if (hasBannedPokemons(player, userInformation)) {
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.checkEgg: hasBannedPokemons");
        }
        return true;
      }
      if (notCorrectCooldown(player)) {
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.checkEgg: notCorrectCooldown");
        }
        return true;
      }
      if (!hasCooldown(player)) {
        if (CobbleDaycare.config.isDebug()) {
          CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.checkEgg: !hasCooldown");
        }
        Pokemon egg = createEgg(player);
        if (!egg.getSpecies().showdownId().equals("egg")) {
          PlayerUtils.sendMessage(
            player,
            "You need install the datapack to use this feature",
            CobbleDaycare.language.getPrefix(),
            TypeMessage.CHAT
          );
        } else {
          if (CobbleDaycare.config.isDebug()) {
            CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Egg created: " + egg.showdownId());
          }
          if (userInformation.isNotifyCreateEgg()) {
            List<Pokemon> pokemons = new ArrayList<>();
            pokemons.add(male);
            pokemons.add(female);
            pokemons.add(egg);
            PlayerUtils.sendMessage(
              player,
              PokemonUtils.replace(CobbleDaycare.language.getMessageEggCreated(), pokemons),
              CobbleDaycare.language.getPrefix(),
              TypeMessage.CHAT
            );
          }
          eggs.add(egg);
          update = true;
          setTime(player);
        }
      }
      return update;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }
  }

  private boolean notCorrectCooldown(ServerPlayerEntity player) {
    long correctCooldown = PlayerUtils.getCooldown(CobbleDaycare.config.getCooldowns(), CobbleDaycare.config.getCooldown(), player);
    long correctTimeToHatch = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(correctCooldown);

    if (timeToHatch > correctTimeToHatch) {
      timeToHatch = correctTimeToHatch;
      if (CobbleDaycare.config.isDebug()) {
        CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.notCorrectCooldown");
      }
      return true;
    }

    return false;
  }

  public Pokemon createEgg(ServerPlayerEntity player) {
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID, "Plot.createEgg");
    }
    Pokemon egg = PokemonProperties.Companion.parse("egg").create();
    Pokemon firstEvolution = female;
    List<Pokemon> parents = new ArrayList<>();
    parents.add(this.male);
    parents.add(this.female);
    EggBuilder eggBuilder = EggBuilder.builder()
      .firstEvolution(firstEvolution)
      .parents(parents)
      .egg(egg)
      .female(female)
      .male(male)
      .player(player)
      .build();
    for (Mechanics mechanic : CobbleDaycare.mechanics) {
      if (mechanic.isActive()) mechanic.applyEgg(eggBuilder);
    }
    // Update the Plot with the modified values from the EggBuilder
    this.male = eggBuilder.getMale();
    this.female = eggBuilder.getFemale();
    return egg;
  }

  private boolean hasBannedPokemons(ServerPlayerEntity player, UserInformation userInformation) {
    boolean maleBanned = false;
    boolean femaleBanned = false;
    if (male != null) maleBanned = CobbleDaycare.config.getBlackList().isBlackListed(male);
    if (maleBanned) {
      sendBanNotification(player, male, userInformation);
      male = null;
    }
    if (female != null) femaleBanned = CobbleDaycare.config.getBlackList().isBlackListed(female);
    if (femaleBanned) {
      sendBanNotification(player, female, userInformation);
      female = null;
    }
    return maleBanned || femaleBanned;
  }

  private void sendBanNotification(ServerPlayerEntity player, Pokemon pokemon, UserInformation userInformation) {
    Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
    if (userInformation.isNotifyBanPokemon()) {
      PlayerUtils.sendMessage(
        player,
        CobbleDaycare.language.getMessageBanPokemon()
          .replace("%pokemon%", pokemon.getSpecies().showdownId()),
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
    }
  }

  public void addPokemon(ServerPlayerEntity player, Pokemon pokemon, SelectGender gender, UserInformation userInformation) {
    if (gender == SelectGender.FEMALE) {
      addFemale(player, pokemon);
    } else {
      addMale(player, pokemon);
    }
    DatabaseClientFactory.INSTANCE.updateUserInformation(player, userInformation);
  }
}
