package libot.commands.music;

import static libot.commands.music.MusicCommandUtils.playUrl;
import static libot.commands.music.PauseCommand.FORMAT_RESUMED;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.core.music.GlobalMusicManager.getMusicManager;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.commands.exceptions.startup.UsageException;
import libot.core.entities.CommandContext;

public class PlayCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		if (c.params().check(0)) {
			playUrl(c, c.params().get(0));
		} else {
			var manager = getMusicManager(c.getGuildIdLong());
			if (manager != null && manager.getPlayer().isPaused()) {
				manager.getPlayer().setPaused(false);
				c.reply(FORMAT_RESUMED, LITHIUM);
			} else {
				throw new UsageException();
			}
		}
	}

	@Override
	public String getName() {
		return "play";
	}

	@Override
	public String getInfo() {
		return """
			Plays audio from a provided URL. Resumes playback if no URL is provided.""";
	}

	@Override
	public String[] getParameters() {
		return array("[URL]");
	}

	@Override
	public int getMinParameters() {
		return 0;
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
