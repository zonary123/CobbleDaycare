package com.kingpixel.cobbledaycare.mixins;

import com.cobblemon.mod.common.block.PastureBlock;
import com.kingpixel.cobbledaycare.CobbleDaycare;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * @author Carlos Varas Alonso - 15/03/2025 2:11
 */
@Mixin(PastureBlock.class)
public abstract class PastureBlockMixin {
  @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
  public void onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit, CallbackInfoReturnable<ActionResult> cir) {
    try {
      if (player == null) return;
      if (CobbleDaycare.config.isCanUseNativeGUI() && player.isInPose(EntityPose.CROUCHING)) return;
      CobbleDaycare.language.getPrincipalMenu().open((ServerPlayerEntity) player);
      cir.setReturnValue(ActionResult.FAIL);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
