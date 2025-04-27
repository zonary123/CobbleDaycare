package com.kingpixel.cobbledaycare.models;


import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.events.HatchEggEvent;
import com.kingpixel.cobbledaycare.mechanics.DayCareForm;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.CobbleUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.block.Blocks;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Carlos Varas Alonso - 23/07/2024 23:01
 */
@Getter
@Setter
@ToString
public class EggData {
  private String pokemon;
  private String form;
  private double steps;
  private int cycles;


  public static EggData from(Pokemon pokemon) {
    if (pokemon == null) return null;
    EggData eggData = new EggData();
    NbtCompound nbt = pokemon.getPersistentData();
    eggData.setPokemon(nbt.getString(DayCarePokemon.TAG_POKEMON));
    eggData.setCycles(nbt.getInt(DayCarePokemon.TAG_CYCLES));
    eggData.setSteps(nbt.getDouble(DayCarePokemon.TAG_STEPS));
    eggData.setForm(nbt.getString(DayCareForm.TAG));
    return eggData;
  }


  public void steps(ServerPlayerEntity player, Pokemon egg, double deltaMovement, UserInformation userInformation) {
    double totalSteps = deltaMovement * userInformation.getActualMultiplier(player);
    egg.setCurrentHealth(0);

    while (totalSteps > 0 && cycles > 0) {
      double stepsPerCycle = egg.getPersistentData().getDouble(DayCarePokemon.TAG_REFERENCE_STEPS);

      if (steps <= 0) {
        steps = stepsPerCycle;
      }

      if (totalSteps >= steps) {
        totalSteps -= steps;
        steps = 0;
        cycles--;

        if (cycles % 3 == 0 && cycles > 0) {
          player.playSoundToPlayer(SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.PLAYERS, 1.0F, 1.0F);
        }
      } else {
        steps -= totalSteps;
        totalSteps = 0;
      }
    }

    if (cycles <= 0) {
      player.playSoundToPlayer(SoundEvents.ENTITY_TURTLE_EGG_HATCH, SoundCategory.PLAYERS, 1.0F, 1.0F);
      BlockStateParticleEffect particleEffect = new BlockStateParticleEffect(ParticleTypes.BLOCK, Blocks.TURTLE_EGG.getDefaultState());
      player.getServerWorld().spawnParticles(
        player,
        particleEffect,
        true,
        player.getX(),
        player.getY() + 0.5,
        player.getZ(),
        20,
        0.5,
        0.5,
        0.5,
        0.1
      );
      hatch(player, egg);
      return;
    } else {
      egg.getPersistentData().putDouble(DayCarePokemon.TAG_STEPS, steps);
      egg.getPersistentData().putInt(DayCarePokemon.TAG_CYCLES, cycles);
    }

    updateName(egg);
  }


  private void updateName(Pokemon egg) {
    egg.setNickname(Text.literal(
      CobbleDaycare.language.getEggName()
        .replace("%steps%", String.format("%.2f", steps))
        .replace("%cycles%", String.valueOf(cycles))
        .replace("%pokemon%", pokemon)
    ));
  }

  public void hatch(ServerPlayerEntity player, Pokemon egg) {
    CompletableFuture.runAsync(() -> {
        for (Mechanics mechanic : CobbleDaycare.mechanics) {
          mechanic.applyHatch(player, egg);
        }
        egg.setNickname(null);
        egg.heal();
        HatchEggEvent.HATCH_EGG_EVENT.emit(player, egg);
        PokemonProperties pokemonProperties = egg.createPokemonProperties(PokemonPropertyExtractor.ALL);
        CobblemonEvents.HATCH_EGG_POST.emit(new com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent.Post(pokemonProperties, player));
      })
      .orTimeout(5, TimeUnit.SECONDS)
      .exceptionally(e -> {
        CobbleUtils.LOGGER.error(CobbleDaycare.MOD_ID, "Error hatching egg");
        e.printStackTrace();
        return null;
      });
  }

}