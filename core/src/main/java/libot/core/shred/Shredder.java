package libot.core.shred;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static libot.core.Constants.RESOURCE_GUILDS;
import static libot.core.processes.ProcessManager.getCurrentProcess;
import static libot.utils.Utilities.concat;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.*;

import org.eu.zajc.ef.consumer.execpt.all.AEConsumer;
import org.slf4j.*;

import com.google.common.cache.Cache;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.*;
import net.dv8tion.jda.api.entities.channel.concrete.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;
import net.dv8tion.jda.api.utils.messages.*;

public class Shredder {

	private static final Logger LOG = LoggerFactory.getLogger(Shredder.class);

	private static final String FORMAT_CLASH_LEAVE = """
		Leaving %s because there is another shred present (%s).""";

	public static record Shred(JDA jda, String name) {

		public long id() {
			return this.jda.getSelfUser().getIdLong();
		}

	}

	public static record PrivateMessageFailure(long userId, long shredId) {}

	@Nonnull private final List<Shred> shreds;
	@Nonnull private final Map<String, Emoji> emojiCache = new ConcurrentHashMap<>();

	private static final Object FAILURE_CACHE_VALUE = new Object();
	@Nonnull private final Cache<PrivateMessageFailure, Object> failures;

	@SuppressWarnings("null")
	public Shredder(@Nonnull List<Shred> shreds) {
		this.shreds = shreds;
		this.failures = newBuilder().expireAfterWrite(Duration.ofSeconds(shreds.size() * 5L)).softValues().build();
	}

	public List<Shred> getShreds() {
		return this.shreds;
	}

	public long getGuildCount() {
		return this.shreds.stream().map(Shred::jda).map(JDA::getGuildCache).mapToLong(SnowflakeCacheView::size).sum()
			   - RESOURCE_GUILDS.length * this.shreds.size();
	}

	public Stream<User> getJoinedUserCache() {
		return this.shreds.stream().map(Shred::jda).map(JDA::getUserCache).flatMap(SnowflakeCacheView<User>::stream);
	}

	@Nullable
	public User getUserById(long userId) {
		var current = getCurrentShred();
		var user = current.jda().getUserById(userId);
		if (user != null)
			return user;
		return this.shreds.stream()
			.filter(s -> s.id() != current.id())
			.map(Shred::jda)
			.map(j -> j.getUserById(userId))
			.filter(Objects::nonNull)
			.findAny()
			.orElse(null);
	}

	@Nonnull
	public CompletableFuture<Message> sendPrivateMessage(long userId, @Nonnull String message) {
		return sendPrivateMessage(userId, new MessageCreateBuilder().setContent(message).build());
	}

	@Nonnull
	public CompletableFuture<Message> sendPrivateMessageEmbeds(long userId, @Nonnull MessageEmbed embed,
															   @Nonnull MessageEmbed... more) {
		return sendPrivateMessageEmbeds(userId, concat(embed, more));
	}

	@Nonnull
	public CompletableFuture<Message> sendPrivateMessageEmbeds(long userId,
															   @Nonnull Collection<? extends MessageEmbed> embeds) {
		return sendPrivateMessage(userId, new MessageCreateBuilder().setEmbeds(embeds));
	}

	@Nonnull
	public CompletableFuture<Message> sendPrivateMessage(long userId, @Nonnull MessageCreateBuilder message) {
		return sendPrivateMessage(userId, message.build());
	}

	@Nonnull
	public CompletableFuture<Message> sendPrivateMessage(long userId, @Nonnull MessageCreateData message) {
		var result = new CompletableFuture<Message>();
		sendPrivateMessage(0, result, userId, message);
		return result;
	}

	private void sendPrivateMessage(int shredIndex, @Nonnull CompletableFuture<Message> result, long userId,
									@Nonnull MessageCreateData message) {
		if (shredIndex >= getShreds().size()) {
			result.completeExceptionally(new IllegalStateException("Failed to send the message"));
			return;
		}

		var shred = getShreds().get(shredIndex);
		var user = shred.jda().getUserById(userId);

		var failureRecord = new PrivateMessageFailure(userId, shred.id());
		if (user == null || this.failures.getIfPresent(failureRecord) != null) {
			sendPrivateMessage(shredIndex + 1, result, userId, message);
			return;
		}

		user.openPrivateChannel().flatMap(p -> p.sendMessage(message)).queue(result::complete, t -> {
			if (t instanceof ErrorResponseException) {
				this.failures.put(failureRecord, FAILURE_CACHE_VALUE);
				sendPrivateMessage(shredIndex + 1, result, userId, message);

			} else {
				LOG.error("Failed to send a message to {} due to an unexpected error", userId);
				LOG.error("", t);
				result.completeExceptionally(t);
			}
		});
	}

	@Nullable
	public VoiceChannel getVoiceChannelById(long id) {
		return getJDAObject(j -> j.getVoiceChannelById(id)).orElse(null);
	}

	@Nullable
	public TextChannel getTextChannelById(long id) {
		return getJDAObject(j -> j.getTextChannelById(id)).orElse(null);
	}

	@Nullable
	public <T extends Channel> T getChannelById(@Nonnull Class<T> type, long id) {
		return getJDAObject(j -> j.getChannelById(type, id)).orElse(null);
	}

	@Nullable
	public Channel getChannelById(@Nonnull ChannelType type, long id) {
		return getJDAObject(j -> j.getChannelCache().getElementById(type, id)).orElse(null);
	}

	@Nonnull
	@SuppressWarnings("null")
	public <T> Optional<T> getJDAObject(Function<JDA, T> getter) {
		return this.shreds.stream().map(Shred::jda).map(getter).filter(Objects::nonNull).findAny();
	}

	public boolean isInCurrentShred(@Nonnull User user) {
		return getCurrentShred().id() == user.getJDA().getSelfUser().getIdLong();
	}

	public boolean isInCurrentShred(@Nonnull Guild guild) {
		return getCurrentShred().id() == guild.getJDA().getSelfUser().getIdLong();
	}

	public boolean isInCurrentShred(@Nonnull Channel channel) {
		return getCurrentShred().id() == channel.getJDA().getSelfUser().getIdLong();
	}

	@Nonnull
	@SuppressWarnings("null")
	public Shred getCurrentShred() {
		long guildId = getCurrentProcess().getGuildId();
		return getShreds().stream()
			.filter(s -> s.jda().getGuildById(guildId) != null)
			.findAny()
			.orElseThrow(() -> new IllegalStateException("Not in a command thread"));
	}

	public void awaitComplete() {
		getShreds().stream().map(Shred::jda).forEach((AEConsumer<JDA>) JDA::awaitReady);
	}

	@SuppressWarnings("null")
	public Emoji getEmojiResource(@Nonnull String name, @Nullable Emoji fallback) {
		return this.emojiCache.computeIfAbsent(name, n -> {
			return stream(RESOURCE_GUILDS).mapToObj(this.shreds.get(0).jda()::getGuildById)
				.map(Guild::getEmojiCache)
				.map(c -> c.getElementsByName(n, true))
				.flatMap(List::stream)
				.map(Emoji.class::cast)
				.findAny()
				.orElse(fallback);
		});
	}

	@SuppressWarnings("null")
	public void resolveClashes(Guild guild) {
		if (contains(RESOURCE_GUILDS, guild.getIdLong()))
			return;
		// ignore resource guilds

		var residents = this.shreds.stream()
			.map(Shred::jda)
			.filter(jda -> jda.getGuildById(guild.getIdLong()) != null)
			.sorted((j1, j2) -> getTimeJoined(j1, guild).compareTo(getTimeJoined(j2, guild)))
			.toArray(n -> new JDA[n]);
		if (residents.length > 1) {
			var mention = residents[0].getSelfUser().getAsMention();
			for (int i = 1; i < residents.length; i++) {
				var g = residents[i].getGuildById(guild.getIdLong());
				if (g == null)
					continue;
				var c = g.getSystemChannel();
				if (c != null && c.canTalk())
					c.sendMessage(format(FORMAT_CLASH_LEAVE, guild.getName(), mention)).queue();
				g.leave().queue();
			}
		}
	}

	private static OffsetDateTime getTimeJoined(@Nonnull JDA jda, @Nonnull Guild guild) {
		var localGuild = jda.getGuildById(guild.getIdLong());
		if (localGuild == null)
			return null;
		return localGuild.getSelfMember().getTimeJoined();
	}

}
