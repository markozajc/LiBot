package libot.core;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getenv;
import static java.util.concurrent.TimeUnit.MINUTES;
import static libot.core.Constants.*;
import static libot.core.processes.ProcessManager.getProcesses;
import static libot.utils.ReflectionUtils.scanClasspath;
import static net.dv8tion.jda.api.OnlineStatus.*;
import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.*;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.*;

import javax.annotation.Nonnull;
import javax.security.auth.login.LoginException;

import org.slf4j.Logger;

import libot.core.commands.CommandManager;
import libot.core.data.DataManagerFactory;
import libot.core.data.providers.ProviderManager;
import libot.core.entities.BotContext;
import libot.core.processes.ProcessManager;
import libot.core.shred.Shredder;
import libot.core.shred.Shredder.Shred;
import libot.management.ManagementServer;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public class Main {

	private static final Logger LOG = getLogger(Main.class);

	private static final String FORMAT_NO_SHREDS =
		"No shreds are created (either none were configured, or they all failed to log in)";

	@SuppressWarnings("null")
	public static void main(String[] argv) throws ReflectiveOperationException, IOException {
		Thread.currentThread().setUncaughtExceptionHandler((t, e) -> {
			LOG.error("Encountered an error on boot, shutting down", e);
			System.exit(1);
		});
		LOG.info("Creating shreds");
		var builder =
			JDABuilder.create(GUILD_MEMBERS, GUILD_EMOJIS, GUILD_VOICE_STATES, GUILD_MESSAGES, GUILD_MESSAGE_REACTIONS)
				.enableCache(VOICE_STATE, EMOTE, MEMBER_OVERRIDES)
				.disableCache(ACTIVITY, CLIENT_STATUS, ONLINE_STATUS)
				.setChunkingFilter(ChunkingFilter.ALL)
				.setStatus(IDLE);

		var shreds = startShreds(builder);
		if (shreds.isEmpty())
			throw new IllegalStateException(FORMAT_NO_SHREDS);

		LOG.info("Creating context");
		var shredder = new Shredder(shreds);
		var data = DataManagerFactory.fromEnvironment();
		var providers = ProviderManager.fromClasspath(shredder, data);
		var config = BotConfiguration.fromEnvironment();
		var commands = CommandManager.fromClasspath();
		var bot = new BotContext(config, commands, data, shredder, providers);

		bot.cron().scheduleWithFixedDelay(providers::storeAll, 2, 2, MINUTES);
		getRuntime().addShutdownHook(new Thread(() -> stop(bot), "libot-shutdown"));

		LOG.info("Loading providers");
		providers.loadAll();

		LOG.info("Loading event listeners");
		loadEventListeners(shredder, bot);

		shredder.awaitComplete();

		providers.onShredderReady();

		LOG.info("Launching the management server");
		new ManagementServer(shredder, parseInt(getenv(ENV_MANAGEMENT_PORT))).start();

		setPresence(shredder);

		LOG.info("Resolving shredder clashes");
		resolveClashes(shredder);

		LOG.info("Checking resource guilds");
		checkResourceGuilds(shredder);

		LOG.info("Finished loading");
		LOG.info("    |_|_ LiBot {}", VERSION);
		LOG.info("  |_|_|_ {} shreds running", shredder.getShreds().size());
		LOG.info("    |_   {} guilds visible", shredder.getGuildCount());
	}

	@SuppressWarnings("null")
	public static void stop(@Nonnull BotContext bot) {
		LOG.info("Shutting down providers");
		bot.providers().shutdownAll();

		LOG.info("Shutting down processes");
		getProcesses().stream().forEach(ProcessManager::interrupt);
		bot.shredder().getShreds().forEach(s -> {
			LOG.info("Shutting down {}", s.name());
			s.jda().shutdown();
		});
	}

	private static List<Shred> startShreds(JDABuilder builder) {
		return getenv().entrySet()
			.stream()
			.filter(e -> e.getKey().startsWith(ENV_SHRED_TOKEN))
			.sorted((e1, e2) -> e1.getKey().compareTo(e2.getKey()))
			.map(e -> {
				var name = e.getKey().substring(ENV_SHRED_TOKEN.length(), e.getKey().length());
				try {
					var jda = builder.setToken(e.getValue()).build();
					return new Shred(jda, name);
				} catch (LoginException le) {
					LOG.error("Failed to log into shred {}", name);
					LOG.error("", le);
					return null;
				}
			})
			.filter(Objects::nonNull)
			.toList();
	}

	@SuppressWarnings("null")
	private static void loadEventListeners(@Nonnull Shredder shredder, @Nonnull BotContext bot) {
		var listeners = scanClasspath(EventListener.class, libot.listeners.Anchor.class, c -> {
			try {
				return c.getDeclaredConstructor(BotContext.class).newInstance(bot);
			} catch (NoSuchMethodException e) {
				return c.getDeclaredConstructor().newInstance();
			}
		}).toArray();
		shredder.getShreds().stream().map(Shred::jda).forEach(j -> j.addEventListener(listeners));
	}

	private static void checkResourceGuilds(@Nonnull Shredder shredder) {
		shredder.getShreds().stream().forEach(s -> {
			for (long resourceGuild : RESOURCE_GUILDS) {
				if (s.jda().getGuildCache().getElementById(resourceGuild) == null)
					LOG.warn("{} is not in resource guild {}", s.name(), resourceGuild);
			}
		});
	}

	private static void resolveClashes(@Nonnull Shredder shredder) {
		shredder.getShreds()
			.stream()
			.map(Shred::jda)
			.map(JDA::getGuildCache)
			.flatMap(SnowflakeCacheView::stream)
			.distinct()
			.forEach(shredder::resolveClashes);
	}

	private static void setPresence(@Nonnull Shredder shredder) {
		shredder.getShreds()
			.stream()
			.map(Shred::jda)
			.map(JDA::getPresence)
			.forEach(p -> p.setPresence(ONLINE, Activity.listening("commands // LiBot")));
	}

	private Main() {}

}
