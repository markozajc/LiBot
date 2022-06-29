package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class ShuffleCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedVChannel();
		if (vc == null)
			throw nothingIsPlaying(c);
		var scheduler = getMusicManager(vc).getScheduler();

		if (scheduler.size() == 0)
			throw c.error("The queue is empty", DISABLED);
		if (scheduler.size() == 1)
			throw c.error("Only one track is in the queue.", DISABLED);

		scheduler.shuffle();
		c.react(ACCEPT_EMOJI);
	}

	@Override
	public String getName() {
		return "shuffle";
	}

	@Override
	public String[] getAliases() {
		return array("shuf");
	}

	@Override
	public String getInfo() {
		return """
			Shuffles the track queue. This will not change the currently playing track.""";
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
