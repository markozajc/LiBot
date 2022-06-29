package libot.core.processes;

import static java.lang.Integer.*;
import static java.lang.Thread.currentThread;
import static java.util.concurrent.Executors.newSingleThreadExecutor;
import static libot.core.ratelimits.RatelimitsManager.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.*;

import org.slf4j.Logger;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import libot.core.commands.Command;
import libot.core.commands.exceptions.ExceptionHandler;
import libot.core.commands.exceptions.startup.*;
import libot.core.entities.CommandContext;
import libot.core.ratelimits.RatelimitsManager;
import libot.providers.ConfigurationProvider;

public class ProcessManager {

	private static final String THREAD_NAME_PREFIX = "libot-proc-";

	public static class CommandProcess {

		private final int pid;
		private final long userId;
		private final long channelId;
		private final long guildId;
		@Nonnull
		private final Command command;
		private Object data;

		public CommandProcess(@Nonnull CommandContext c, int pid) {
			this.pid = pid;
			this.userId = c.getUserIdLong();
			this.channelId = c.getChannelIdLong();
			this.guildId = c.getGuildIdLong();
			this.command = c.getCommand();
			this.data = null;
		}

		public int getPid() {
			return this.pid;
		}

		public long getUserId() {
			return this.userId;
		}

		public long getChannelId() {
			return this.channelId;
		}

		public long getGuildId() {
			return this.guildId;
		}

		@Nonnull
		public Command getCommand() {
			return this.command;
		}

		public Object getData() {
			return this.data;
		}

		public void setData(Object data) {
			this.data = data;
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

	}

	private static final Logger LOG = getLogger(ProcessManager.class);

	public static final int MAX_COMMANDS_PER_USER = 5;

	private static final AtomicInteger COUNT = new AtomicInteger();
	private static final Map<CommandProcess, Thread> PROCESSES = new ConcurrentHashMap<>(50);
	private static final ExecutorService STARTUPS =
		newSingleThreadExecutor(new ThreadFactoryBuilder().setNameFormat("startup-executor").build());

	public static void run(@Nonnull CommandContext c) {
		STARTUPS.submit(() -> runBlocking(c));
	}

	private static void runBlocking(@Nonnull CommandContext c) {
		if (!checkStartup(c))
			return;

		getProcesses().stream()
			.filter(p -> p.getUserId() == c.getUserIdLong())
			.skip(MAX_COMMANDS_PER_USER - 1L)
			.forEach(ProcessManager::interrupt);

		int pid = remainderUnsigned(COUNT.getAndIncrement(), 1000);
		var process = new CommandProcess(c, pid);

		if (LOG.isTraceEnabled())
			LOG.trace("Launching a command: {}", process);

		var thread = new Thread(() -> executeCommandBlocking(c, process));
		thread.setName(THREAD_NAME_PREFIX + toUnsignedString(pid));
		thread.setUncaughtExceptionHandler((t, e) -> {
			PROCESSES.remove(process);
			LOG.error("ExceptionHandler threw an exception", e);
			var defaultUeh = Thread.getDefaultUncaughtExceptionHandler();
			if (defaultUeh != null)
				defaultUeh.uncaughtException(t, e);
		});
		PROCESSES.put(process, thread);
		thread.start();
	}

	private static void executeCommandBlocking(@Nonnull CommandContext c, @Nonnull CommandProcess process) {
		try {
			c.getCommand().execute(c);

			if (c.getCommandRatelimit() != 0)
				RatelimitsManager.getRatelimits(c.getCommand()).register(c.getUserIdLong());

		} catch (Throwable t) { // NOSONAR no
			ExceptionHandler.handle(c, t);

		} finally {
			PROCESSES.remove(process);
		}
	}

	private static boolean checkStartup(@Nonnull CommandContext c) {
		try {
			if (c.provider(ConfigurationProvider.class).isDisabled(c.getCommand()))
				throw new CommandDisabledException(true);

			if (c.getGuildCustomization().isDisabled(c.getCommand()))
				throw new CommandDisabledException(false);

			if (!c.params().check(c.getCommand().getMinParameters() - 1))
				throw new UsageException();

			if (isRatelimited(c.getCommand(), c.getUserIdLong()))
				throw new RatelimitedException(getRemaining(c.getCommand(), c.getUserIdLong()));

			c.getCommand().startupCheck(c);
			return true;

		} catch (CommandStartupException t) {
			if (LOG.isTraceEnabled())
				LOG.trace("Command execution rejected: {}", t.toString());
			ExceptionHandler.handle(c, t);
			return false;
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Set<CommandProcess> getProcesses() {
		return PROCESSES.keySet();
	}

	@Nullable
	public static CommandProcess getProcess(int pid) {
		return getProcesses().stream().filter(p -> p.getPid() == pid).findAny().orElse(null);
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

	@Nullable
	public static Thread getThread(@Nonnull CommandProcess proc) {
		return PROCESSES.get(proc);
	}

	public static long getCount() {
		return toUnsignedLong(COUNT.get());
	}

	public static boolean interrupt(@Nonnull CommandProcess process) {
		var thread = getThread(process);
		if (thread == null || !thread.isAlive())
			return false;

		thread.interrupt();
		return true;
	}

	private ProcessManager() {}

}
