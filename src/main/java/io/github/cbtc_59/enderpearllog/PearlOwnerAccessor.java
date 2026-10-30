/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog;

import java.util.UUID;

/**
 * 读取投射物保存的所属玩家 UUID，即使玩家当前离线。
 */
public interface PearlOwnerAccessor {
	UUID pearlLog$getOwnerUuid();
}
