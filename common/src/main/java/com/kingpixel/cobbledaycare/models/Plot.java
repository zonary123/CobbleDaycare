package com.kingpixel.cobbledaycare.models;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.egg.EggGroup;
import com.cobblemon.mod.common.pokemon.Gender;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.database.DatabaseClientFactory;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
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
import java.util.UUID;
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

  public static String plotPermission(int i) {
    return "cobbledaycare.plot." + (i + 1);
  }

  private boolean isDitto(Pokemon pokemon) {
    if (pokemon == null) return false;
    return pokemon.getForm().getEggGroups().contains(EggGroup.DITTO);
  }

  public boolean canBreed(Pokemon pokemon, SelectGender gender) {
    if (pokemon == null) return false;
    CobbleDaycare.fixBreedable(pokemon);
    if (isNotBreedable(pokemon)) return false;
    if (CobbleDaycare.config.getBlackList().isBlackListed(pokemon)) return false;
    Pokemon other = gender == SelectGender.MALE ? female : male;

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
    if (pokemon.getGender().equals(Gender.GENDERLESS) && !pokemon.getForm().getEggGroups().contains(EggGroup.DITTO))
      return false;
    for (EggGroup eggGroup : pokemon.getForm().getEggGroups()) {
      if (other.getForm().getEggGroups().contains(eggGroup) || pokemon.getForm().getEggGroups().contains(EggGroup.DITTO))
        return true;
    }
    return false;
  }

  public boolean hasCooldownToOpen(ServerPlayerEntity player) {
    return canOpen > System.currentTimeMillis();
  }

  public void setTime(ServerPlayerEntity player) {
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
      if (PermissionApi.hasPermission(player, List.of(limitEgg.getKey(), "cobbleutils.breeding." + limitEgg), 4)) {
        limit = limitEgg.getValue();
      }
    }
    return limit;
  }

  public boolean checkEgg(ServerPlayerEntity player, UserInformation userInformation) {
    try {
      boolean update = false;

      if (!hasTwoParents()) return update;
      int index = userInformation.getPlots().indexOf(this) + 1;
      int sizeEggs = eggs.size();
      if (sizeEggs >= limitEggs(player)) {
        if (userInformation.isNotifyLimitEggs()) {
          PlayerUtils.sendMessage(
            player,
            CobbleDaycare.language.getMessageLimitEggs()
              .replace("%plot%", index + ""),
            CobbleDaycare.language.getPrefix(),
            TypeMessage.CHAT
          );
        }
        return update;
      }
      boolean femaleCanBreed = canBreed(female, SelectGender.FEMALE);
      boolean maleCanBreed = canBreed(male, SelectGender.MALE);
      if (!femaleCanBreed) {
        PlayerUtils.sendMessage(
          player,
          PokemonUtils.replace(CobbleDaycare.language.getMessageRemovedFemale(), female)
            .replace("%plot%", index + ""),
          CobbleDaycare.language.getPrefix(),
          TypeMessage.CHAT
        );
        Cobblemon.INSTANCE.getStorage().getParty(player).add(female);
        female = null;
      }
      if (!maleCanBreed) {
        PlayerUtils.sendMessage(
          player,
          PokemonUtils.replace(CobbleDaycare.language.getMessageRemovedMale(), male)
            .replace("%plot%", index + ""),
          CobbleDaycare.language.getPrefix(),
          TypeMessage.CHAT
        );
        Cobblemon.INSTANCE.getStorage().getParty(player).add(male);
        male = null;
      }
      if (!maleCanBreed || !femaleCanBreed) return true;
      fixCooldown(player);
      if (!hasCooldown(player)) {
        Pokemon egg = createEgg(player);
        if (!egg.getSpecies().showdownId().equals("egg")) {
          PlayerUtils.sendMessage(
            player,
            "You need install the datapack to use this feature",
            CobbleDaycare.language.getPrefix(),
            TypeMessage.CHAT
          );
        } else {
          if (userInformation.isNotifyCreateEgg()) {
            List<Pokemon> pokemons = new ArrayList<>();
            pokemons.add(male);
            pokemons.add(female);
            pokemons.add(egg);
            PlayerUtils.sendMessage(
              player,
              PokemonUtils.replace(CobbleDaycare.language.getMessageEggCreated()
                .replace("%pokemon3%", egg.getPersistentData().getString(DayCarePokemon.TAG_POKEMON)), pokemons)
              ,
              CobbleDaycare.language.getPrefix(),
              TypeMessage.CHAT
            );
          }
          if (PermissionApi.hasPermission(player, List.of("cobbleutils.autoclaim", "cobbleutils.admin"), 2)) {
            Cobblemon.INSTANCE.getStorage().getParty(player).add(egg);
          } else {
            eggs.add(egg);
          }
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

  private void fixCooldown(ServerPlayerEntity player) {
    long correctCooldown = PlayerUtils.getCooldown(CobbleDaycare.config.getCooldowns(), CobbleDaycare.config.getCooldown(), player);
    long correctTimeToHatch = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(correctCooldown);
    if (timeToHatch > correctTimeToHatch) {
      timeToHatch = correctTimeToHatch;
    }
  }

  public Pokemon createEgg(ServerPlayerEntity player) {
    Pokemon egg = PokemonProperties.Companion.parse("egg").create();
    egg.setUuid(UUID.randomUUID());
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
    return egg;
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
