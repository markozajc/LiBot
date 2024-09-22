package libot.commands;

import static libot.commands.MusicCommandUtils.nothingIsPlaying;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class ClearQueueCommand extends Command {

	public ClearQueueCommand() {
		super(CommandMetadata.builder(MUSIC, "clearqueue")
			.aliases("clear")
			.requireDjRole(true)
			.description("Clears the queue. The currently playing track will remain playing."));
	}

	@Override
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		var scheduler = getMusicManager(vc).getScheduler();

		if (scheduler.isEmpty())
			throw c.error("The queue is already empty.", DISABLED);

		scheduler.clear();
		c.reply("Queue cleared successfully", SUCCESS);
	}

}
