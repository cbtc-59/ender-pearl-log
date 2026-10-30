/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog.mixin;

import io.github.cbtc_59.enderpearllog.PearlLogEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在玩家注册珍珠引用前检查是否已有相同 UUID。
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerPearlRegistryMixin {
	@Inject(method = "registerEnderPearl", at = @At("HEAD"))
	private void pearlLog$beforeRegister(
		ThrownEnderpearl pearl,
		CallbackInfo callbackInfo
	) {
		PearlLogEvents.onOwnerRegistration((ServerPlayer) (Object) this, pearl);
	}
}
