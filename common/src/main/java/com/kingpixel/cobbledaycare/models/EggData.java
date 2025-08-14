package com.kingpixel.cobbledaycare.models;


import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.api.pokemon.PokemonPropertyExtractor;
import com.cobblemon.mod.common.api.pokemon.feature.IntSpeciesFeature;
import com.cobblemon.mod.common.net.messages.client.pokemon.update.SpeciesFeatureUpdatePacket;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbledaycare.events.HatchEggEvent;
import com.kingpixel.cobbledaycare.mechanics.DayCarePokemon;
import com.kingpixel.cobbledaycare.mechanics.Mechanics;
import com.kingpixel.cobbleutils.CobbleUtils;
import com.kingpixel.cobbleutils.util.PlayerUtils;
import com.kingpixel.cobbleutils.util.TypeMessage;
import kotlin.jvm.functions.Function0;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import net.minecraft.block.Blocks;
import net.minecraft.particle.BlockStateParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/**
 * @author Carlos Varas Alonso - 23/07/2024 23:01
 */
@Getter
@Setter
@ToString
public class EggData {
  private static final String PERCENTAGE_ROUND_TAG = "PERCENTAGE_ROUND_EGG";
  private static final String PERCENTAGE_TAG = "percentage";


  public static void steps(ServerPlayerEntity player, Pokemon egg, double deltaMovement, UserInformation userInformation) {
    double totalSteps = deltaMovement * userInformation.getActualMultiplier(player);
    double steps = egg.getPersistentData().getDouble(DayCarePokemon.TAG_STEPS);
    int cycles = egg.getPersistentData().getInt(DayCarePokemon.TAG_CYCLES);
    int referenceCycles = egg.getPersistentData().getInt(DayCarePokemon.TAG_REFERENCE_CYCLES);
    int pasosPorCiclo = (int) egg.getPersistentData().getDouble(DayCarePokemon.TAG_REFERENCE_STEPS);
    int totalReferenceSteps = referenceCycles * pasosPorCiclo;
    int totalPasosRecorridos = (referenceCycles - cycles) * pasosPorCiclo + (pasosPorCiclo - (int) steps);
    int percentage = (int) ((totalPasosRecorridos * 100.0) / totalReferenceSteps);
    int percentageBlock = (percentage / 25) * 25;

    var features = egg.getFeatures();
    IntSpeciesFeature feature = null;

    for (var f : features) {
      if (f == null) continue;
      if (f.getName().equals(PERCENTAGE_TAG)) {
        feature = (IntSpeciesFeature) f;
        break;
      }
    }

    if (feature == null) {
      feature = new IntSpeciesFeature(PERCENTAGE_TAG, percentageBlock);
      features.add(feature);
    } else {
      feature.setValue(percentage);
    }


    try {
      SpeciesFeatureUpdatePacket packet = new SpeciesFeatureUpdatePacket(
        (Function0<? extends Pokemon>) () -> egg,
        egg.getSpecies().resourceIdentifier,
        feature
      );
      packet.sendToPlayer(player);
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error actualizando el feature del huevo: " + e.getMessage());
      e.printStackTrace();
    }

    if (percentageBlock != egg.getPersistentData().getInt(PERCENTAGE_ROUND_TAG)) {
      egg.getPersistentData().putInt(PERCENTAGE_ROUND_TAG, percentageBlock);
      PokemonProperties.Companion.parse("egg_crack=" + percentageBlock).apply(egg);
    }

    while (totalSteps > 0 && cycles > 0) {
      double stepsPerCycle = egg.getPersistentData().getDouble(DayCarePokemon.TAG_REFERENCE_STEPS);

      if (steps <= 0) steps = stepsPerCycle;

      if (totalSteps >= steps) {
        totalSteps -= steps;
        steps = 0;
        cycles--;

        if (cycles % 3 == 0 && cycles > 0)
          player.playSoundToPlayer(SoundEvents.ENTITY_TURTLE_EGG_CRACK, SoundCategory.PLAYERS, 1.0F, 1.0F);

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

    updateName(egg, cycles, steps);
  }


  private static void updateName(Pokemon egg, int cycles, double steps) {
    egg.setNickname(Text.literal(
      CobbleDaycare.language.getEggName()
        .replace("%steps%", String.format("%.2f", steps))
        .replace("%cycles%", String.valueOf(cycles))
        .replace("%pokemon%", egg.getPersistentData().getString(DayCarePokemon.TAG_POKEMON))
    ));
  }

  public static void hatch(ServerPlayerEntity player, Pokemon egg) {
    try {
      egg.setOriginalTrainer(player.getUuid());
      egg.getFeatures().removeIf(feature -> feature.getName().equals(PERCENTAGE_TAG));
      egg.getPersistentData().remove(PERCENTAGE_TAG);
      egg.getPersistentData().remove(PERCENTAGE_ROUND_TAG);
      HatchBuilder builder = HatchBuilder.builder()
        .egg(egg)
        .player(player)
        .pokemon(null)
        .build();

      int level = egg.getLevel();
      for (Mechanics mechanic : CobbleDaycare.mechanics) {
        try {
          mechanic.applyHatch(builder);
        } catch (Exception e) {
          CobbleUtils.LOGGER.info("Error applying hatch mechanic: " + mechanic.fileName() + " - " + e.getClass().getName());
          e.printStackTrace();
        }
      }
      var party = Cobblemon.INSTANCE.getStorage().getParty(player);
      if (builder.getPokemon() != null && builder.getEgg() != null) {
        party.remove(egg);
        builder.getPokemon().setLevel(level);
        party.add(builder.getPokemon());
        HatchEggEvent.HATCH_EGG_EVENT.emit(builder.getPlayer(), builder.getPokemon());
        PokemonProperties pokemonProperties = builder.getPokemon().createPokemonProperties(PokemonPropertyExtractor.ALL);
        CobblemonEvents.HATCH_EGG_POST.emit(new com.cobblemon.mod.common.api.events.pokemon.HatchEggEvent.Post(pokemonProperties, player));
      }
    } catch (Exception e) {
      CobbleUtils.LOGGER.error("Error hatching egg");
      e.printStackTrace();
      Cobblemon.INSTANCE.getStorage().getParty(player).remove(egg);
      PlayerUtils.sendMessage(
        player,
        "Error hatching egg corrupted data or invalid egg talk to the admins for help",
        CobbleDaycare.language.getPrefix(),
        TypeMessage.CHAT
      );
    }
  }

}