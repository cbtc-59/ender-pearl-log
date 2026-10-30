/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog.mixin;

import java.util.UUID;

import io.github.cbtc_59.enderpearllog.PearlOwnerAccessor;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityReference;
import net.minecraft.world.entity.projectile.Projectile;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

/**
 * 暴露投射物的原始所属玩家 UUID。
 */
@Mixin(Projectile.class)
public abstract class ProjectileOwnerMixin implements PearlOwnerAccessor {
	@Shadow
	protected @Nullable EntityReference<Entity> owner;

	@Override
	public UUID pearlLog$getOwnerUuid() {
		return this.owner == null ? null : this.owner.getUUID();
	}
}
