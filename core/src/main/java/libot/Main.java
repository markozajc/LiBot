package libot;

import static java.lang.Integer.parseInt;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getenv;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Stream.concat;
import static libot.core.Constants.*;
import static libot.core.processes.ProcessManager.getProcesses;
import static libot.utils.ReflectionUtils.scanClasspath;
import static net.dv8tion.jda.api.OnlineStatus.*;
import static net.dv8tion.jda.api.requests.GatewayIntent.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.*;
import static net.dv8tion.jda.api.utils.cache.CacheFlag.SCHEDULED_EVENTS;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import com.sedmelluq.discord.lavaplayer.jdaudp.NativeAudioSendFactory;

import libot.core.BotConfiguration;
import libot.core.commands.CommandManager;
import libot.core.data.DataManagerFactory;
import libot.core.data.providers.ProviderManager;
import libot.core.entities.BotContext;
import libot.core.listeners.*;
import libot.core.processes.ProcessManager;
import libot.core.shred.Shredder;
import libot.core.shred.Shredder.Shred;
import libot.listeners.BotEventListener;
import libot.management.ManagementServer;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
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
		var ewl = new EventWaiterListener();

		var builder = JDABuilder
			.create(GUILD_MEMBERS, GUILD_EMOJIS_AND_STICKERS, GUILD_VOICE_STATES, GUILD_MESSAGES, MESSAGE_CONTENT,
					GUILD_MESSAGE_REACTIONS)
			.enableCache(VOICE_STATE, EMOJI, MEMBER_OVERRIDES)
			.disableCache(ACTIVITY, CLIENT_STATUS, ONLINE_STATUS, SCHEDULED_EVENTS)
			.setChunkingFilter(ChunkingFilter.ALL)
			.addEventListeners(ewl)
			.setStatus(IDLE)
			.setAudioSendFactory(new NativeAudioSendFactory());

		var shreds = startShreds(builder);
		if (shreds.isEmpty())
			throw new IllegalStateException(FORMAT_NO_SHREDS);

		LOG.info("Creating context");
		var shredder = new Shredder(shreds);

		LOG.info("Creating DataManager");
		var data = DataManagerFactory.fromEnvironment();
		LOG.info("Using {} for storage", data.getClass().getSimpleName());

		LOG.info("Creating providers");
		var providers = ProviderManager.fromClasspath(shredder, data);
		LOG.info("Created {} providers", providers.size());

		LOG.info("Loading configuration");
		var config = BotConfiguration.fromEnvironment();

		LOG.info("Creating commands");
		var commands = CommandManager.fromClasspath();
		LOG.info("Created {} commands", commands.size());

		var bot = new BotContext(config, commands, data, shredder, providers, ewl);
		LOG.info("Context created, finalizing startup");

		bot.cron().scheduleWithFixedDelay(providers::storeAll, 2, 2, MINUTES);
		getRuntime().addShutdownHook(new Thread(() -> stop(bot), "libot-shutdown"));

		LOG.info("Loading providers");
		providers.loadAll();

		LOG.info("Creating event listeners");
		loadEventListeners(shredder, bot);

		shredder.awaitComplete();

		providers.onShredderReady();

		if (getenv(ENV_MANAGEMENT_PORT) != null) {
			LOG.info("Launching the management server");
			new ManagementServer(shredder, parseInt(getenv(ENV_MANAGEMENT_PORT))).start();
		}

		setPresence(shredder);

		LOG.info("Resolving shredder clashes");
		resolveClashes(shredder);

		if (RESOURCE_GUILDS.length != 0) {
			LOG.info("Checking resource guilds");
			checkResourceGuilds(shredder);
		}

		LOG.info("Invoking post-startup listeners");
		scanClasspath(BotEventListener.class, libot.listeners.Anchor.class).forEach(l -> l.onStartup(bot));

		LOG.info("Finished loading");
		LOG.info("    |_|_ LiBot {}", VERSION);
		LOG.info("  |_|_|_ {} shreds running", shredder.getShreds().size());
		LOG.info("    |_   {} guilds visible", shredder.getGuildCount());

	}

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
			.sorted(Comparator.comparing(Entry<String, String>::getKey))
			.map(e -> {
				var name = e.getKey().substring(ENV_SHRED_TOKEN.length(), e.getKey().length());
				try {
					var jda = builder.setToken(e.getValue()).build();
					jda.addEventListener(new ListenerAdapter() {

						@Override
						public void onReady(ReadyEvent event) {
							LOG.info("{} is online", name);
						}

					});
					return new Shred(jda, name);
				} catch (InvalidTokenException le) {
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
		var listeners = concat(Stream.of(new MessageListener(bot), new ShredClashListener(bot), new EventLogListener()),
							   scanClasspath(EventListener.class, libot.listeners.Anchor.class, c -> {
								   try {
									   return c.getDeclaredConstructor(BotContext.class).newInstance(bot);
								   } catch (NoSuchMethodException e) {
									   return c.getDeclaredConstructor().newInstance();
								   }
							   })).toArray();
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
