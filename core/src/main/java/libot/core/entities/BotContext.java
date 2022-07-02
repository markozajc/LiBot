package libot.core.entities;

import static java.util.concurrent.Executors.newScheduledThreadPool;

import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import libot.core.BotConfiguration;
import libot.core.commands.CommandManager;
import libot.core.data.DataManager;
import libot.core.data.providers.*;
import libot.core.listeners.EventWaiterListener;
import libot.core.shred.Shredder;

public class BotContext {

	@Nonnull @SuppressWarnings("null")
	private final ScheduledExecutorService cron = newScheduledThreadPool(1);
	@Nonnull
	private final BotConfiguration config;
	@Nonnull
	private final CommandManager commands;
	@Nonnull
	private final DataManager data;
	@Nonnull
	private final Shredder shredder;
	@Nonnull
	private final ProviderManager providers;
	@Nonnull
	private final EventWaiterListener ewl;

	public BotContext(@Nonnull BotConfiguration config, @Nonnull CommandManager commands, @Nonnull DataManager data,
					  @Nonnull Shredder shredder, @Nonnull ProviderManager providers,
					  @Nonnull EventWaiterListener ewl) {
		this.config = config;
		this.commands = commands;
		this.data = data;
		this.shredder = shredder;
		this.providers = providers;
		this.ewl = ewl;
	}

	@Nonnull
	public BotConfiguration config() {
		return this.config;
	}

	@Nonnull
	public CommandManager commands() {
		return this.commands;
	}

	@Nonnull
	public DataManager data() {
		return this.data;
	}

	@Nonnull
	public Shredder shredder() {
		return this.shredder;
	}

	@Nonnull
	public ProviderManager providers() {
		return this.providers;
	}

	@Nonnull
	public ScheduledExecutorService cron() {
		return this.cron;
	}

	@Nonnull
	public EventWaiterListener ewl() {
		return this.ewl;
	}

	@Nonnull
	public <T extends Provider<?>> T provider(@Nonnull Class<T> clazz) {
		return this.providers.get(clazz);
	}

}
