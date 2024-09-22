package libot.commands;

import static java.lang.Integer.parseInt;
import static java.lang.Math.min;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toCollection;
import static libot.core.Constants.*;
import static libot.module.music.GlobalMusicManager.*;
import static libot.module.music.TrackScheduler.QUEUE_MAX_SIZE;
import static libot.utils.Utilities.plural;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.math.NumberUtils.isParsable;

import java.util.List;

import javax.annotation.Nonnull;

import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.*;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.*;

import libot.core.commands.exceptions.CommandException;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.module.music.GlobalMusicManager.MusicManager;
import libot.utils.MessageLock;
import net.dv8tion.jda.api.entities.MessageEmbed;

class MusicCommandUtils {

	private static final Logger LOG = LoggerFactory.getLogger(MusicCommandUtils.class);

	static final String YOUTUBE_URL_PREFIX = "https://www.youtube.com/watch?v=";

	static final String EMOJI_PLAYING = "\u25B6\uFE0F";
	static final String EMOJI_PAUSE = "\u23F8\uFE0F";
	static final String EMOJI_LOOP = "\uD83D\uDD01";

	private static final String FORMAT_FOOTER_PICKER = """
		Please type in a track's index (the bold number) or EXIT to abort""";
	public static final String FORMAT_PLAY_TRACK = """
		%s Now playing: **[%s](%s)** by _%s_.""";
	static final String FORMAT_QUEUE_FULL = """
		This track has not been added to the queue because the queue may not exceed %d elements!""";
	private static final String FORMAT_URL_NOT_FOUND = """
		No playable track found on `%s`. Did you mean `%s %s`?""";

	static void playUrl(@Nonnull CommandContext c, @Nonnull String url) {
		var vchannel = c.getMemberVoiceState().getChannel();
		if (vchannel == null)
			throw c.error("You must be in a voice channel in order to play music", FAILURE);

		var am = c.getAudioManager();

		if (!am.isConnected())
			am.openAudioConnection(vchannel);

		var manager = getMusicManager(vchannel);

		if (am.getSendingHandler() == null)
			am.setSendingHandler(manager.getSendHandler());
		playTrack(c, url, manager);
	}

	@SuppressWarnings("null")
	static void playTrack(@Nonnull CommandContext c, @Nonnull String url, @Nonnull MusicManager manager) {
		var lock = new MessageLock<List<AudioTrack>>();

		APM.loadItem(url, new AudioLoadResultHandlerImpl(c, lock, manager, url));

		var tracks = lock.receive();
		if (tracks != null && !tracks.isEmpty())
			playTrack(c, selectTrack(c, tracks).getInfo().uri, manager);
	}

	@Nonnull
	@SuppressWarnings("null")
	static List<AudioTrack> youtubeSearch(@Nonnull String query) {
		var lock = new MessageLock<List<AudioTrack>>();

		APM.loadItem("ytsearch:" + query, new AudioLoadResultHandler() {

			@Override
			public void trackLoaded(AudioTrack track) {
				lock.send(emptyList());
			}

			@Override
			public void playlistLoaded(AudioPlaylist playlist) {
				lock.send(playlist.getTracks());
			}

			@Override
			public void noMatches() {
				lock.send(emptyList());
			}

			@Override
			public void loadFailed(FriendlyException e) {
				lock.throwException(e);
			}

		});

		return lock.receive();
	}

	@Nonnull
	@SuppressWarnings("null")
	static AudioTrack selectTrack(@Nonnull CommandContext c, @Nonnull List<AudioTrack> results) {
		if (results.isEmpty())
			throw c.error("No results were found", DISABLED);

		var b = new StringBuilder();
		var i = new MutableInt(1);
		results.stream().limit(10).forEach(track -> {
			b.append("**#");
			b.append(i.getAndIncrement());
			b.append(":** ");
			b.append(escape(track.getInfo().author, true));
			b.append(" - ");
			b.append("_");
			b.append(escape(track.getInfo().title, true));
			b.append("_\n");
		});

		var message = c
			.reply("Search results from %s".formatted(capitalize(results.get(0).getSourceManager().getSourceName())),
				   b.toString(), FORMAT_FOOTER_PICKER, LITHIUM)
			.join();

		while (true) {
			String indexString = c.ask();

			int index = 0;
			if ("exit".equalsIgnoreCase(indexString)) {
				message.delete().queue();
				throw c.cancel();

			} else if (!isParsable(indexString)) {
				c.reply("Index is a number!");

			} else if ((index = parseInt(indexString)) < 1 || index > results.size()) {
				c.reply("Invalid index");

			} else {
				message.delete().queue();
				return results.get(index - 1);
			}
		}
	}

	static CommandException nothingIsPlaying(@Nonnull CommandContext c) {
		return c.errorf("Nothing is playing", "Play a track with `%s [track's name]` or `%s [url]`!", DISABLED,
						c.getCommandWithPrefix(YoutubePlayCommand.class), c.getCommandWithPrefix(PlayCommand.class));
	}

	private static class AudioLoadResultHandlerImpl implements AudioLoadResultHandler {

		@Nonnull private final CommandContext c;
		@Nonnull private final MessageLock<List<AudioTrack>> lock;
		@Nonnull private final MusicManager manager;
		@Nonnull private final String url;

		public AudioLoadResultHandlerImpl(@Nonnull CommandContext c, @Nonnull MessageLock<List<AudioTrack>> lock,
										  @Nonnull MusicManager manager, @Nonnull String url) {
			this.c = c;
			this.lock = lock;
			this.manager = manager;
			this.url = url;
		}

		@SuppressWarnings("null")
		@Override
		public void trackLoaded(AudioTrack track) {
			this.lock.send(null);

			var info = track.getInfo();
			var sched = this.manager.getScheduler();
			sched.queueCallback(track, () -> {
				var a = this.c.replyf("Started playing", FORMAT_PLAY_TRACK, SUCCESS,
									  sched.isLoop() ? EMOJI_LOOP : EMOJI_PLAYING, escape(info.title, true), info.uri,
									  escape(info.author, true));
				return e -> a.thenAccept(m -> m.editMessageEmbeds(getFailureEmbed(e)).queue());
			}, () -> { // NOSONAR
				var playing = this.manager.getPlayingTrack();
				String playingTitle = "nothing";
				String playingUri = "https://www.youtube.com/watch?v=dQw4w9WgXcQ"; // :)
				if (playing != null) {
					playingTitle = playing.getInfo().title;
					playingUri = playing.getInfo().uri;
				}

				this.c.replyf("Track queued", """
					\u23EC Put **[%s](%s)** by _%s_ into queue.
					*Currently %s: **[%s](%s)**.*""", SUCCESS, escape(info.title, true), info.uri,
							  escape(info.author, true), sched.isLoop() ? "looping" : "playing",
							  escape(playingTitle, true), playingUri);
			}, () -> { // NOSONAR
				this.c.replyf("Queue is full", FORMAT_QUEUE_FULL, FAILURE, QUEUE_MAX_SIZE);
			});
		}

		@Override
		public void playlistLoaded(AudioPlaylist playlist) {
			if (playlist.isSearchResult()) {
				this.lock.send(playlist.getTracks());
			} else {
				this.lock.send(null);
				loadPlaylist(playlist);
			}
		}

		@SuppressWarnings("null")
		public void loadPlaylist(AudioPlaylist playlist) {
			this.c.typing();
			var s = this.manager.getScheduler();
			var selected = playlist.getSelectedTrack();
			if (selected == null)
				selected = playlist.getTracks().get(0);
			boolean started = this.manager.getPlayer().startTrack(selected, true);
			int total = playlist.getTracks().size();
			int added = min(QUEUE_MAX_SIZE - s.size(), total);
			playlist.getTracks()
				.stream()
				.filter(t -> !started || !t.equals(playlist.getSelectedTrack()))
				.limit((long) QUEUE_MAX_SIZE - s.size())
				.collect(toCollection(s::getQueue));

			var e = new EmbedPrebuilder();
			if (started) {
				e.setTitle("Started playing");
				e.setColor(SUCCESS);
			} else {
				e.setTitle("Queued");
				e.setColor(LITHIUM);
			}

			var b = new StringBuilder();
			b.append("Added **");
			b.append(added);
			b.append("** track");
			b.append(plural(added));
			b.append(" from playlist **");
			b.append(escape(playlist.getName(), true));
			b.append("** to the queue");
			if (started)
				b.append(" and started the player");
			b.append(".");
			if (total - added > 0) {
				e.setColor(WARN);
				b.append(" **");
				b.append(total - added);
				b.append("** elements were not added to the queue because the queue is capped at **");
				b.append(QUEUE_MAX_SIZE);
				b.append("** elements!");
			}
			e.setDescription(b);
			this.c.reply(e);
		}

		@Override
		public void noMatches() {
			this.lock.send(null);
			this.c.replyf(FORMAT_URL_NOT_FOUND, DISABLED, this.url,
						  this.c.getCommandWithPrefix(YoutubePlayCommand.class), this.url);
		}

		@Override
		public void loadFailed(FriendlyException e) {
			LOG.error("Failed to load a track from url {}", this.url);
			LOG.error("", e);
			this.lock.send(null);
			this.c.reply(getFailureEmbed(e));
		}

		@Nonnull
		@SuppressWarnings("null")
		private static MessageEmbed getFailureEmbed(FriendlyException e) {
			var b = new EmbedPrebuilder(e.getMessage(), FAILURE);
			if (e.getCause() != null && e.getCause().getMessage() != null)
				b.setFooter(e.getCause().getMessage());
			return b.build();
		}
	}

	private MusicCommandUtils() {}

}
