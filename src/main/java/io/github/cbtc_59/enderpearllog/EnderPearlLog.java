/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog;

import net.fabricmc.api.ModInitializer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ender Pearl Log 的服务端入口。
 */
public final class EnderPearlLog implements ModInitializer {
	public static final String MOD_ID = "ender_pearl_log";
	public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PearlLogWriter.start();
		PearlLogEvents.registerEvents();
		LOGGER.info("[EnderPearlLog] 末影珍珠只读日志已加载");
	}
}
