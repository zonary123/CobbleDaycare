package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import com.kingpixel.cobbleutils.CobbleUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 15/04/2025 22:35
 */
@Mixin(PokemonEntity.class)
public abstract class PreventDamage {

  @Inject(method = "offerHeldItem", at = @At("HEAD"), cancellable = true)
  private void offerHeldItem(PlayerEntity player, ItemStack itemStack, CallbackInfoReturnable<Boolean> cir) {
    try {
      PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
      if (pokemonEntity == null) return;
      var pokemon = pokemonEntity.getPokemon();
      if (pokemon == null) return;
      if (cobbleDaycare$isEgg(pokemon)) {
        cir.setReturnValue(false);
      }
    } catch (Exception ignored) {
    }
  }

  @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
  private void damage(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
    try {
      PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
      if (pokemonEntity == null) return;
      var pokemon = pokemonEntity.getPokemon();
      if (pokemon == null) return;
      var attacker = source.getAttacker();
      if (attacker == null) return;
      if (attacker instanceof ServerPlayerEntity player) {
        if (cobbleDaycare$isEgg(pokemon)) {
          cobbleDaycare$givePokemon(player, pokemon, pokemonEntity);
          cir.cancel();
        }
      }
    } catch (Exception ignored) {
    }
  }

  @Inject(method = "interactMob", at = @At("HEAD"), cancellable = true)
  private void interact(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
    try {
      PokemonEntity pokemonEntity = (PokemonEntity) (Object) this;
      if (pokemonEntity == null) return;
      var pokemon = pokemonEntity.getPokemon();
      if (pokemon == null) return;
      if (pokemon.isPlayerOwned()) return;
      if (player == null) return;
      if (cobbleDaycare$isEgg(pokemon)) {
        cobbleDaycare$givePokemon((ServerPlayerEntity) player, pokemon, pokemonEntity);
        cir.cancel();
      }
    } catch (Exception ignored) {
    }
  }


  @Unique
  private void cobbleDaycare$givePokemon(ServerPlayerEntity player, Pokemon pokemon, PokemonEntity pokemonEntity) {
    if (CobbleDaycare.config.isDebug()) {
      CobbleUtils.LOGGER.info(CobbleDaycare.MOD_ID,
        " Persistent data: " + pokemon.getPersistentData().getBoolean(CobbleDaycare.TAG_SPAWNED) + " " +
          " Owner: " + pokemon.isPlayerOwned());
    }
    if (pokemon.getPersistentData().getBoolean(CobbleDaycare.TAG_SPAWNED) && !pokemon.isPlayerOwned()) {
      pokemon.getPersistentData().remove(CobbleDaycare.TAG_SPAWNED);
      Cobblemon.INSTANCE.getStorage().getParty(player).add(pokemon);
      pokemonEntity.remove(Entity.RemovalReason.DISCARDED);
    }
  }

  @Unique
  private boolean cobbleDaycare$isEgg(Pokemon pokemon) {
    return pokemon.getSpecies().showdownId().equals("egg");
  }
}
