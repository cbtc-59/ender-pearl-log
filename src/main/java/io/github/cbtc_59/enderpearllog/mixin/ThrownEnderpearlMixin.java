/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog.mixin;

import io.github.cbtc_59.enderpearllog.PearlLogEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在末影珍珠碰撞和移除时记录上下文。
 */
@Mixin(ThrownEnderpearl.class)
public abstract class ThrownEnderpearlMixin {
	@Inject(method = "onHit", at = @At("HEAD"))
	private void pearlLog$onHit(HitResult hitResult, CallbackInfo callbackInfo) {
		PearlLogEvents.onHit((ThrownEnderpearl) (Object) this, hitResult);
	}

	@Inject(method = "onRemoval", at = @At("HEAD"))
	private void pearlLog$onRemoval(Entity.RemovalReason reason, CallbackInfo callbackInfo) {
		PearlLogEvents.onRemoval((ThrownEnderpearl) (Object) this, reason);
	}
}
