package libot.core.shred;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static libot.core.Constants.RESOURCE_GUILDS;
import static libot.core.processes.ProcessManager.getCurrentProcess;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import javax.annotation.*;

import com.github.markozajc.functions.exceptionable.all.AEConsumer;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.requests.RestAction;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public class Shredder {

	private static final String FORMAT_CLASH_LEAVE = """
		Leaving %s because there is another shred present (%s).""";

	public static record Shred(JDA jda, String name) {

		public long id() {
			return this.jda.getSelfUser().getIdLong();
		}

	}

	@Nonnull
	private final List<Shred> shreds;
	@Nonnull
	private final Map<String, String> emojiCache = new ConcurrentHashMap<>();

	public Shredder(@Nonnull List<Shred> shreds) {
		this.shreds = shreds;
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

	@Nullable
	public RestAction<PrivateChannel> openPrivateChannelById(long userId) {
		return getJDAObject(j -> j.getUserById(userId)).map(User::openPrivateChannel).orElse(null);
	}

	@Nullable
	public VoiceChannel getVoiceChannelById(long id) {
		return getJDAObject(j -> j.getVoiceChannelById(id)).orElse(null);
	}

	@Nullable
	public TextChannel getTextChannelById(long id) {
		return getJDAObject(j -> j.getTextChannelById(id)).orElse(null);
	}

	@Nonnull
	@SuppressWarnings("null")
	private <T> Optional<T> getJDAObject(Function<JDA, T> getter) {
		return this.shreds.stream().map(Shred::jda).map(getter).filter(Objects::nonNull).findAny();
	}

	public boolean isInCurrentShred(@Nonnull User user) {
		return getCurrentShred().id() == user.getJDA().getSelfUser().getIdLong();
	}

	public boolean isInCurrentShred(@Nonnull Guild guild) {
		return getCurrentShred().id() == guild.getJDA().getSelfUser().getIdLong();
	}

	public boolean isInCurrentShred(@Nonnull AbstractChannel channel) {
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

	public String getEmojiResource(@Nonnull String name, @Nullable String fallback) {
		@SuppressWarnings("null")
		String result = this.emojiCache
			.computeIfAbsent(name,
							 n -> stream(RESOURCE_GUILDS).mapToObj(this.shreds.get(0).jda()::getGuildById)
								 .map(Guild::getEmoteCache)
								 .map(c -> c.getElementsByName(n, true))
								 .flatMap(List::stream)
								 .findAny()
								 .map(Emote::getAsMention)
								 .orElse(""));
		if (result.isEmpty())
			return fallback;
		else
			return result;
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
