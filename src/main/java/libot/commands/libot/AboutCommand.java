package libot.commands.libot;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.time.Instant.ofEpochMilli;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.LIBOT;
import static libot.utils.DiscordUtils.LINKS;
import static libot.utils.Utilities.array;

import java.util.Objects;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.music.GlobalMusicManager;
import libot.core.music.GlobalMusicManager.MusicManager;
import libot.core.processes.ProcessManager;

public class AboutCommand extends Command {

	private static final String FORMAT_STATISTICS = """
		Guild count: **%d**,
		Total of **%d** commands launched,
		Currently playing music on **%d** guild%s.""";
	private static final String FORMAT_DESCRIPTION = """
		[LiBot](https://git.zajc.eu.org/libot.git/) is a Discord multi-purpose bot written by Marko Zajc in \
		[Java](https://openjdk.java.net/) using [JDA](https://github.com/DV8FromTheWorld/JDA/).
		%s""";

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM);
		b.setTitle("About LiBot");
		b.setDescriptionf(FORMAT_DESCRIPTION, LINKS);
		b.setThumbnail(c.getSelfUser().getAvatarUrl());
		appendStatistics(c, b);
		b.setFooterf("v%s | Last reboot: ", VERSION);
		b.setTimestamp(ofEpochMilli(getRuntimeMXBean().getStartTime()));
		c.reply(b);
	}

	private static void appendStatistics(@Nonnull CommandContext c, @Nonnull EmbedPrebuilder b) {
		long playing = GlobalMusicManager.getMusicManagers()
			.values()
			.stream()
			.map(MusicManager::getPlayingTrack)
			.filter(Objects::nonNull)
			.count();
		b.addFieldf("Statistics", FORMAT_STATISTICS, c.shredder().getGuildCount(), ProcessManager.getCount(), playing,
					playing == 1 ? "" : "s", true);
	}

	@Override
	public String getName() {
		return "about";
	}

	@Override
	public String[] getAliases() {
		return array("info", "botinfo", "stats");
	}

	@Override
	public String getInfo() {
		return "Displays information about the bot.";
	}

	@Override
	public CommandCategory getCategory() {
		return LIBOT;
	}

}
