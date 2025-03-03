//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.core.process;

import static java.lang.Integer.*;
import static java.lang.Thread.currentThread;
import static java.util.Collections.unmodifiableCollection;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static libot.core.ratelimit.CommandRatelimitManager.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Collection;
import java.util.concurrent.ExecutorService;

import javax.annotation.*;

import org.eclipse.collections.api.factory.primitive.LongObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableLongObjectMap;
import org.eu.zajc.ef.EHandle;
import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import libot.core.command.Command;
import libot.core.command.exception.ExceptionHandler;
import libot.core.command.exception.startup.*;
import libot.core.entity.*;
import libot.core.ratelimit.CommandRatelimitManager;
import libot.provider.ConfigurationProvider;

public class ProcessManager {

	private static final String THREAD_NAME_PREFIX = "libot-proc-";

	private static final Logger LOG = getLogger(ProcessManager.class);

	public static final int MAX_COMMANDS_PER_USER = 5;
	public static final int MAX_PID = 999;

	private static volatile int previousPid = -1;
	private static volatile long count = 0;
	private static final MutableLongObjectMap<CommandProcess> PROCESSES = LongObjectMaps.mutable.empty();
	private static final ExecutorService STARTUPS =
		newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("startup-executor").build());

	public static class CommandProcess {

		private final int pid;
		@Nonnull private final CommandContext ctx;
		private Object data = null;
		private Thread thread;

		private CommandProcess(int pid, @Nonnull CommandContext ctx) {
			this.pid = pid;
			this.ctx = ctx;
		}

		@Nonnull
		public static synchronized CommandProcess create(@Nonnull CommandContext ctx) {
			int attempt = 0;
			do {
				if (attempt++ == MAX_PID)
					throw new IllegalStateException("Ran out of PIDs");

				previousPid = (previousPid + 1) % (MAX_PID + 1);
			} while (PROCESSES.containsKey(previousPid));

			count++;
			return new CommandProcess(previousPid, ctx);
		}

		void start() {
			var thread = new Thread(() -> {
				try {
					this.ctx.getCommand().execute(this.ctx);

					if (this.ctx.isCommandRatelimited())
						CommandRatelimitManager.getRatelimits(this.ctx.getCommand()).register(this.ctx.getUserIdLong());

				} catch (Throwable t) { // NOSONAR no
					ExceptionHandler.handle(t, this.ctx);

				} finally {
					PROCESSES.remove(this.getPid());
				}
			});
			thread.setName(THREAD_NAME_PREFIX + toUnsignedString(this.pid));
			thread.setUncaughtExceptionHandler((t, e) -> {
				LOG.error("ExceptionHandler threw an exception", e);
				var defaultExHandler = Thread.getDefaultUncaughtExceptionHandler();
				if (defaultExHandler != null)
					defaultExHandler.uncaughtException(t, e);
			});
			this.thread = thread;

			if (PROCESSES.getIfAbsentPut(this.getPid(), this) != null)
				thread.start();
			else
				throw new IllegalStateException("PID collision while starting a process");
		}

		public int getPid() {
			return this.pid;
		}

		public long getUserId() {
			return this.ctx.getUserIdLong();
		}

		public long getChannelId() {
			return this.ctx.getChannelIdLong();
		}

		public long getGuildId() {
			return this.ctx.getGuildIdLong();
		}

		@Nonnull
		public Command getCommand() {
			return this.ctx.getCommand();
		}

		public Object getData() {
			return this.data;
		}

		public void setData(Object data) {
			this.data = data;
		}

		public Thread getThread() {
			return this.thread;
		}

		@Override
		public int hashCode() {
			return this.pid;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj instanceof CommandProcess other)
				return this.pid == other.pid;
			else
				return false;
		}

		@Override
		public String toString() {
			return "CommandProcess[%s@%d/%d/ by %d, pid %d]".formatted(getCommand().getName(), getGuildId(),
																	   getChannelId(), getUserId(), this.pid);
		}

	}

	@SuppressWarnings("null")
	public static void run(@Nonnull Command cmd, @Nonnull EventContext eventContext, @Nullable String input) {
		STARTUPS.submit(EHandle.handle(() -> {
			doStartupCheck(cmd, eventContext);
			killSuperfluousProcesses(eventContext);

			var args = cmd.getParameters().parse(input);
			var context = new CommandContext(eventContext, cmd, args);
			var process = CommandProcess.create(context);

			if (LOG.isTraceEnabled())
				LOG.trace("Launching {}", process);

			process.start();

		}, e -> {
			if (LOG.isTraceEnabled())
				LOG.trace("Command execution failed: {}", e.toString());
			ExceptionHandler.handle(e, cmd, eventContext);
		}));
	}

	private static void killSuperfluousProcesses(@Nonnull EventContext ctx) {
		PROCESSES.values()
			.stream()
			.filter(p -> p.getUserId() == ctx.getUserIdLong())
			.skip(MAX_COMMANDS_PER_USER - 1L)
			.forEach(ProcessManager::interrupt);
	}

	private static void doStartupCheck(@Nonnull Command cmd, @Nonnull EventContext ctx) {
		if (ctx.getProvider(ConfigurationProvider.class).isDisabled(cmd))
			throw new CommandDisabledException(true);

		if (ctx.getGuildCustomization().isDisabled(cmd))
			throw new CommandDisabledException(false);

		if (isRatelimited(cmd, ctx.getUserIdLong()))
			throw new RatelimitedException(getRemaining(cmd, ctx.getUserIdLong()));

		cmd.startupCheck(ctx);
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Collection<CommandProcess> getProcesses() {
		return unmodifiableCollection(PROCESSES.values());
	}

	@Nullable
	public static CommandProcess getProcess(int pid) {
		return PROCESSES.get(pid);
	}

	@Nonnull
	public static CommandProcess getProcess(@Nonnull Thread thread) {
		if (!thread.getName().startsWith(THREAD_NAME_PREFIX))
			throw new IllegalStateException("Not a command thread");
		var proc = getProcess(parseUnsignedInt(thread.getName().substring(THREAD_NAME_PREFIX.length())));
		if (proc == null) {
			LOG.error("Encountered an unregistered thread: {}", thread.getName());
			throw new IllegalStateException("Thread not registered");
		}
		return proc;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static CommandProcess getCurrentProcess() {
		return getProcess(currentThread());
	}

	public static long getCount() {
		return count;
	}

	public static boolean interrupt(@Nonnull CommandProcess process) {
		var thread = process.getThread();
		if (thread == null || !thread.isAlive())
			return false;

		thread.interrupt();
		return true;
	}

	private ProcessManager() {}

}
