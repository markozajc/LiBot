package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.SUCCESS;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class LoopCommand extends Command {

	public LoopCommand() {
		super(CommandMetadata.builder(MUSIC, "loop")
			.requireDjRole(true)
			.description("Toggles looping state over the current track."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var manager = getMusicManager(vc);

		if (manager.getScheduler().isLoop()) {
			manager.getScheduler().setLoop(false);
			c.reply("Loop OFF", "\u25B6 No longer looping.", SUCCESS);

		} else {
			var track = manager.getPlayingTrack();
			if (track == null)
				throw nothingIsPlaying(c);

			manager.getScheduler().setLoop(true);
			c.replyf("Loop ON", "\uD83D\uDD01 Now looping over **[%s](%s)**.", SUCCESS,
					 escape(track.getInfo().title).replace("]", "\\]"), track.getInfo().uri);
		}
	}

}
