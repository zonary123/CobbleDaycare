package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.pokemon.Pokemon;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * @author Carlos Varas Alonso - 15/03/2025 23:59
 */
@Mixin(Pokemon.class)
public abstract class EggMixin {

  @Inject(method = "setHeldItem$common", at = @At("HEAD"), cancellable = true)
  public void swapHeldItem(ItemStack _set___, CallbackInfo ci) {
    Pokemon pokemon = (Pokemon) (Object) this;
    if (pokemon.getSpecies().showdownId().equals("egg")) ci.cancel();
  }
}
