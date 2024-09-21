package libot.commands;

import static java.lang.management.ManagementFactory.getRuntimeMXBean;
import static java.time.Instant.ofEpochMilli;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.LIBOT;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.processes.ProcessManager;
import libot.module.music.GlobalMusicManager;
import libot.module.music.GlobalMusicManager.MusicManager;

public class AboutCommand extends Command {

	public AboutCommand() {
		super(CommandMetadata.builder(LIBOT, "about")
			.aliases("info", "botinfo", "stats")
			.description("Displays information about the bot."));
	}

	public static final String LINKS = """
		**[Get LiBot](https://libot.eu.org/get/)** - \
		**[Website](https://libot.eu.org/)** - \
		**[Support guild](https://discord.gg/asDUrbR)** - \
		**[Source](https://git.zajc.tel/libot/)**""";

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM);
		b.setTitle("About LiBot");
		b.setDescriptionf("""
			[LiBot](https://libot.eu.org/) is a Discord multi-purpose bot written by [Marko Zajc](https://zajc.tel/) in \
			[Java](https://openjdk.java.net/) using [JDA](https://github.com/DV8FromTheWorld/JDA/).
			%s""", LINKS);
		b.setThumbnail(c.getSelfUser().getAvatarUrl());
		appendStatistics(c, b);
		b.setFooterf("v%s | Last reboot ", VERSION);
		b.setTimestamp(ofEpochMilli(getRuntimeMXBean().getStartTime()));
		c.reply(b);
	}

	private static void appendStatistics(@Nonnull CommandContext c, @Nonnull EmbedPrebuilder b) {
		long playing = GlobalMusicManager.getManagers().values().stream().filter(MusicManager::isPlayingTrack).count();
		b.addFieldf("Statistics", """
			Guild count: **%d**,
			Total of **%d** commands launched,
			Currently playing music on **%d** guild%s.""", c.getShredder().getGuildCount(), ProcessManager.getCount(),
					playing, playing == 1 ? "" : "s", true);
	}

}
