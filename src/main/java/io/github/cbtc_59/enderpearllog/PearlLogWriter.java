/*
 * Copyright (c) 2026 cbtc-59
 * Released under the MIT License.
 */

package io.github.cbtc_59.enderpearllog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import net.fabricmc.loader.api.FabricLoader;

/**
 * 在独立线程中写入服务端控制台和事件日志，避免阻塞游戏线程。
 */
final class PearlLogWriter {
	private static final int QUEUE_CAPACITY = 16384;
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	private static final Path LOG_PATH = FabricLoader.getInstance()
		.getGameDir()
		.resolve("logs")
		.resolve("ender-pearl.log");
	private static final BlockingQueue<LogEntry> QUEUE = new ArrayBlockingQueue<>(QUEUE_CAPACITY);
	private static final AtomicLong DROPPED_MESSAGES = new AtomicLong();
	private static final Object LIFECYCLE_LOCK = new Object();
	private static final LogEntry SHUTDOWN = LogEntry.shutdownMarker();
	private static volatile Thread writerThread;
	private static volatile boolean stopping;

	private PearlLogWriter() {
	}

	static void start() {
		synchronized (LIFECYCLE_LOCK) {
			if (writerThread != null) {
				return;
			}

			stopping = false;
			Thread thread = new Thread(PearlLogWriter::runWriter, "EnderPearlLog-Writer");
			thread.setDaemon(true);
			writerThread = thread;
			thread.start();
		}
	}

	static void write(String category, String message) {
		start();
		if (stopping || !QUEUE.offer(LogEntry.of(category, message))) {
			DROPPED_MESSAGES.incrementAndGet();
		}
	}

	static void shutdown() {
		Thread thread;
		synchronized (LIFECYCLE_LOCK) {
			thread = writerThread;
			if (thread == null) {
				return;
			}
			stopping = true;
		}

		try {
			if (!QUEUE.offer(SHUTDOWN, 10L, TimeUnit.SECONDS)) {
				EnderPearlLog.LOGGER.error("[EnderPearlLog] 日志队列未能及时排空");
				thread.interrupt();
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			thread.interrupt();
		}

		try {
			thread.join(10_000L);
			if (thread.isAlive()) {
				EnderPearlLog.LOGGER.error("[EnderPearlLog] 日志线程未能在超时前停止");
				thread.interrupt();
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			thread.interrupt();
		}

		synchronized (LIFECYCLE_LOCK) {
			if (writerThread == thread) {
				writerThread = null;
			}
		}
	}

	static Path path() {
		return LOG_PATH;
	}

	private static void runWriter() {
		try {
			while (true) {
				LogEntry entry = QUEUE.take();
				if (entry.shutdown()) {
					writeDroppedCount();
					return;
				}

				writeDroppedCount();
				writeEntry(entry);
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		} finally {
			synchronized (LIFECYCLE_LOCK) {
				if (writerThread == Thread.currentThread()) {
					writerThread = null;
				}
			}
		}
	}

	private static void writeDroppedCount() {
		long dropped = DROPPED_MESSAGES.getAndSet(0L);
		if (dropped > 0L) {
			writeEntry(LogEntry.of(
				"日志丢弃",
				"日志队列已满，丢弃 %d 条事件；请缩短记录时间或降低事件量".formatted(dropped)
			));
		}
	}

	private static void writeEntry(LogEntry entry) {
		EnderPearlLog.LOGGER.info("[末影珍珠日志] [{}] {}", entry.category(), entry.message());

		try {
			Files.createDirectories(LOG_PATH.getParent());
			Files.writeString(
				LOG_PATH,
				entry.line() + System.lineSeparator(),
				StandardCharsets.UTF_8,
				StandardOpenOption.CREATE,
				StandardOpenOption.APPEND
			);
		} catch (IOException exception) {
			EnderPearlLog.LOGGER.error(
				"[EnderPearlLog] 无法写入日志文件 {}：{}",
				LOG_PATH,
				exception.toString()
			);
		}
	}

	private record LogEntry(String category, String message, LocalDateTime createdAt, boolean shutdown) {
		private static LogEntry of(String category, String message) {
			return new LogEntry(category, message, LocalDateTime.now(), false);
		}

		private static LogEntry shutdownMarker() {
			return new LogEntry("", "", LocalDateTime.now(), true);
		}

		private String line() {
			return "[%s] [%s] %s".formatted(
				this.createdAt.format(TIME_FORMAT),
				this.category,
				this.message
			);
		}
	}
}
