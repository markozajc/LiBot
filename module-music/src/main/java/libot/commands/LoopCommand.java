package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.SUCCESS;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class LoopCommand extends Command {

	private static final String FORMAT_LOOP_OFF = "\u25B6 No longer looping.";
	private static final String FORMAT_LOOP = "\uD83D\uDD01 Now looping over **[%s](%s)**.";

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedVChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);

		if (manager.getScheduler().isLoop()) {
			manager.getScheduler().setLoop(false);
			c.reply("Loop OFF", FORMAT_LOOP_OFF, SUCCESS);

		} else {
			var track = manager.getPlayingTrack();
			if (track == null)
				throw nothingIsPlaying(c);

			manager.getScheduler().setLoop(true);
			c.replyf("Loop ON", FORMAT_LOOP, SUCCESS, escape(track.getInfo().title).replace("]", "\\]"),
					 track.getInfo().uri);
		}
	}

	@Override
	public String getName() {
		return "loop";
	}

	@Override
	public String getInfo() {
		return """
			Toggles looping state over the current track.""";
	}

	@Override
	public void startupCheck(CommandContext c) {
		super.startupCheck(c);
		c.requireDj();
	}

	@Override
	public CommandCategory getCategory() {
		return MUSIC;
	}

}
