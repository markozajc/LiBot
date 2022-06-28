package libot.commands.music;

import static java.lang.String.format;
import static libot.commands.music.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.core.music.GlobalMusicManager.getMusicManager;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.entities.MessageEmbed.DESCRIPTION_MAX_LENGTH;

import java.util.*;

import javax.annotation.Nonnull;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.music.GlobalMusicManager.MusicManager;

public class QueueCommand extends Command {

	private static final String FORMAT_TITLE = "Queue for %s";
	private static final String FORMAT_FOOTER = "Displaying page %d out of %d";
	private static final String FORMAT_FOOTER_APPENDIX = " • %s [page] to show a specific page";
	private static final String FORMAT_QUEUED_TRACK = "**#%d** [%s: %s](%s)\n";
	private static final String FORMAT_PLAYING_TRACK = "\u25B6 **[%s: %s](%s)**\n";

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedVChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);
		var current = manager.getPlayingTrack();
		if (current == null)
			throw nothingIsPlaying(c);

		int page = c.params().getIntOrDefault(0, 1);
		var pages = buildQueuePages(manager, current);

		if (page > pages.size() || page < 1)
			throw c.error("Page index out of range", FAILURE);

		String footer = format(FORMAT_FOOTER, page, pages.size());
		if (page == 1)
			footer += format(FORMAT_FOOTER_APPENDIX, c.getCommandWithPrefix());
		c.reply(format(FORMAT_TITLE, c.getGuildName()), pages.get(page - 1).toString(), footer, LITHIUM);
	}

	@Nonnull
	private static List<StringBuilder> buildQueuePages(@Nonnull MusicManager gmm, @Nonnull AudioTrack current) {
		var list = new ArrayList<StringBuilder>();
		var b = new StringBuilder();

		b.append(format(FORMAT_PLAYING_TRACK, current.getInfo().author, current.getInfo().title,
						current.getInfo().uri));

		int i = 1;
		for (var track : gmm.getScheduler().getQueue()) {
			var info =
				format(FORMAT_QUEUED_TRACK, i, track.getInfo().author, track.getInfo().title, track.getInfo().uri);

			if (b.length() + info.length() > DESCRIPTION_MAX_LENGTH) {
				list.add(b);
				b = new StringBuilder();
			}

			b.append(info);

			i++;
		}
		list.add(b);
		return list;
	}

	@Override
	public String getName() {
		return "queue";
	}

	@Override
	public String getInfo() {
		return """
			Displays the current playing queue.""";
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public String[] getParameters() {
		return array("page");
	}

	@Override
	public String[] getParameterInfo() {
		return array("page number");
	}

	@Override
	public CommandCategory getCategory() {
		return MUSIC;
	}

}
