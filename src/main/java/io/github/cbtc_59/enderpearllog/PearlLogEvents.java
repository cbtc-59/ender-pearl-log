/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrownEnderpearl;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * 跟踪已加载的末影珍珠，并记录其生命周期事件。
 */
public final class PearlLogEvents {
	private static final long SCAN_INTERVAL_TICKS = 100L;
	private static final Set<ThrownEnderpearl> ACTIVE_PEARLS =
		Collections.newSetFromMap(new IdentityHashMap<>());
	private static final Set<UUID> REPORTED_DUPLICATE_UUIDS = new HashSet<>();
	private static long serverTick;

	private PearlLogEvents() {
	}

	public static void registerEvents() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			ACTIVE_PEARLS.clear();
			REPORTED_DUPLICATE_UUIDS.clear();
			serverTick = 0L;
			PearlLogWriter.write(
				"服务端启动",
				"日志文件=%s，死亡清除规则=%s".formatted(
					PearlLogWriter.path(),
					server.getGameRules().get(GameRules.ENDER_PEARLS_VANISH_ON_DEATH)
				)
			);
			scan(server, "服务端启动");
		});

		ServerLifecycleEvents.BEFORE_SAVE.register((server, flush, force) -> {
			PearlLogWriter.write(
				"保存前",
				"flush=%s，force=%s，已加载珍珠=%d".formatted(flush, force, countPearls(server))
			);
		});

		ServerLifecycleEvents.AFTER_SAVE.register((server, flush, force) -> {
			PearlLogWriter.write(
				"保存后",
				"flush=%s，force=%s，已加载珍珠=%d".formatted(flush, force, countPearls(server))
			);
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			scan(server, "服务端停止前");
			PearlLogWriter.write("服务端停止", "停止前已加载珍珠=%d".formatted(countPearls(server)));
		});

		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			PearlLogWriter.write("服务端已停止", "开始排空日志队列");
			PearlLogWriter.shutdown();
		});

		ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
			if (entity instanceof ServerPlayer player) {
				UUID playerUuid = player.getUUID();
				PearlLogWriter.write(
					"玩家死亡",
					"%s，名下已加载珍珠=%d".formatted(
						describePlayer(player),
						countOwnedPearls(player.level().getServer(), playerUuid)
					)
				);
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				scan(server, "玩家上线");
				PearlLogWriter.write(
					"玩家上线",
					"%s，名下已加载珍珠=%d".formatted(
						describePlayer(player),
						countOwnedPearls(server, player.getUUID())
					)
				);
			}
		});

		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			ServerPlayer player = handler.getPlayer();
			if (player != null) {
				UUID playerUuid = player.getUUID();
				scan(server, "玩家断线");
				PearlLogWriter.write(
					"玩家断线",
					"%s，名下已加载珍珠=%d".formatted(
						describePlayer(player),
						countOwnedPearls(server, playerUuid)
					)
				);
			}
		});

		ServerTickEvents.END_SERVER_TICK.register(server -> {
			serverTick++;
			if (serverTick % SCAN_INTERVAL_TICKS == 0L) {
				scan(server, "周期扫描");
			}
			if (serverTick % 1200L == 0L) {
				PearlLogWriter.write("周期状态", "已加载珍珠=%d".formatted(countPearls(server)));
			}
		});
	}

	public static void onHit(ThrownEnderpearl pearl, HitResult hitResult) {
		String target;
		if (hitResult instanceof BlockHitResult blockHit) {
			BlockPos pos = blockHit.getBlockPos();
			target = "方块=%s，方块坐标=%s".formatted(
				pearl.level().getBlockState(pos),
				formatBlockPos(pos)
			);
		} else if (hitResult instanceof EntityHitResult entityHit) {
			Entity entity = entityHit.getEntity();
			target = "实体=%s，实体UUID=%s".formatted(entity.getType(), entity.getUUID());
		} else {
			target = "命中类型=" + hitResult.getType();
		}

		PearlLogWriter.write(
			"碰撞",
			"%s，%s，命中点=%s".formatted(describePearl(pearl), target, formatVec(hitResult.getLocation()))
		);
	}

	public static void onOwnerRegistration(ServerPlayer player, ThrownEnderpearl pearl) {
		int duplicates = 0;
		for (ThrownEnderpearl existing : player.getEnderPearls()) {
			if (existing != pearl && existing.getUUID().equals(pearl.getUUID())) {
				duplicates++;
			}
		}

		if (duplicates > 0) {
			PearlLogWriter.write(
				"玩家珍珠重复注册",
				"%s，重复数量=%d，%s".formatted(
					describePlayer(player),
					duplicates,
					describePearl(pearl)
				)
			);
		}
	}

	public static void onRemoval(ThrownEnderpearl pearl, Entity.RemovalReason reason) {
		ACTIVE_PEARLS.remove(pearl);
		String category = reason.shouldDestroy() ? "永久移除" : "临时卸载";
		PearlLogWriter.write(
			category,
			"%s，原因=%s，会销毁=%s，会保存=%s".formatted(
				describePearl(pearl),
				reason,
				reason.shouldDestroy(),
				reason.shouldSave()
			)
		);
	}

	private static void scan(MinecraftServer server, String trigger) {
		Set<ThrownEnderpearl> present = Collections.newSetFromMap(new IdentityHashMap<>());
		Map<UUID, List<ThrownEnderpearl>> presentByUuid = new HashMap<>();

		for (ServerLevel level : server.getAllLevels()) {
			for (ThrownEnderpearl pearl : level.getEntities(EntityType.ENDER_PEARL, ignored -> true)) {
				UUID pearlUuid = pearl.getUUID();
				present.add(pearl);
				presentByUuid.computeIfAbsent(pearlUuid, ignored -> new ArrayList<>()).add(pearl);
			}
		}

		for (List<ThrownEnderpearl> pearls : presentByUuid.values()) {
			UUID pearlUuid = pearls.getFirst().getUUID();
			if (pearls.size() > 1) {
				if (REPORTED_DUPLICATE_UUIDS.add(pearlUuid)) {
					PearlLogWriter.write(
						"重复UUID",
						"同一 UUID 同时存在 %d 颗珍珠，可能是 MC-306936 的重复持久化；当前=%s；先前=%s".formatted(
							pearls.size(),
							describePearl(pearls.get(1)),
							describePearl(pearls.get(0))
						)
					);
				}
			} else {
				REPORTED_DUPLICATE_UUIDS.remove(pearlUuid);
			}
		}

		for (ThrownEnderpearl pearl : present) {
			if (ACTIVE_PEARLS.add(pearl)) {
				PearlLogWriter.write("发现", "%s（%s）".formatted(describePearl(pearl), trigger));
			}
		}

		Iterator<ThrownEnderpearl> iterator = ACTIVE_PEARLS.iterator();
		while (iterator.hasNext()) {
			ThrownEnderpearl pearl = iterator.next();
			if (present.contains(pearl)) {
				continue;
			}

			if (pearl.isRemoved()) {
				PearlLogWriter.write(
					"离开实体表",
					"%s，原因=%s（%s）".formatted(
						describePearl(pearl),
						pearl.getRemovalReason(),
						trigger
					)
				);
			} else {
				PearlLogWriter.write(
					"异常缺失",
					"%s，对象尚未标记移除但已不在实体表中（%s）".formatted(describePearl(pearl), trigger)
				);
			}
			iterator.remove();
		}
	}

	private static int countPearls(MinecraftServer server) {
		int count = 0;
		for (ServerLevel level : server.getAllLevels()) {
			count += level.getEntities(EntityType.ENDER_PEARL, ignored -> true).size();
		}
		return count;
	}

	private static int countOwnedPearls(MinecraftServer server, UUID ownerUuid) {
		int count = 0;
		for (ServerLevel level : server.getAllLevels()) {
			for (ThrownEnderpearl pearl : level.getEntities(EntityType.ENDER_PEARL, ignored -> true)) {
				if (ownerUuid.equals(ownerUuid(pearl))) {
					count++;
				}
			}
		}
		return count;
	}

	private static String describePearl(ThrownEnderpearl pearl) {
		UUID ownerUuid = ownerUuid(pearl);
		String owner;
		if (ownerUuid == null) {
			owner = "所属玩家=未知";
		} else {
			ServerPlayer onlineOwner = pearl.level().getServer().getPlayerList().getPlayer(ownerUuid);
			owner = "所属玩家=%s，UUID=%s".formatted(
				onlineOwner == null ? "离线" : onlineOwner.getPlainTextName(),
				ownerUuid
			);
		}

		return "珍珠UUID=%s，%s，维度=%s，坐标=%s，区块=[%d, %d]，tick=%d，速度=%s".formatted(
			pearl.getUUID(),
			owner,
			pearl.level().dimension().identifier(),
			formatVec(pearl.position()),
			pearl.chunkPosition().x(),
			pearl.chunkPosition().z(),
			pearl.tickCount,
			formatVec(pearl.getDeltaMovement())
		);
	}

	private static UUID ownerUuid(ThrownEnderpearl pearl) {
		return ((PearlOwnerAccessor) pearl).pearlLog$getOwnerUuid();
	}

	private static String describePlayer(ServerPlayer player) {
		return "玩家=%s，UUID=%s，维度=%s，坐标=%s".formatted(
			player.getPlainTextName(),
			player.getUUID(),
			player.level().dimension().identifier(),
			formatVec(player.position())
		);
	}

	private static String formatBlockPos(BlockPos pos) {
		return "[%d, %d, %d]".formatted(pos.getX(), pos.getY(), pos.getZ());
	}

	private static String formatVec(Vec3 vec) {
		return String.format(Locale.ROOT, "[%.3f, %.3f, %.3f]", vec.x, vec.y, vec.z);
	}
}
