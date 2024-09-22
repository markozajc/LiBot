package libot.commands;

import static libot.commands.MusicCommandUtils.*;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.MUSIC;
import static libot.module.music.GlobalMusicManager.getMusicManager;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class SkipCommand extends Command {

	@Nonnull private static final Parameter TRACKS =
		optional(POSITIONAL, "tracks", "number of tracks to skip, 1 by default");

	public SkipCommand() {
		super(CommandMetadata.builder(MUSIC, "skip")
			.requireDjRole(true)
			.parameters(TRACKS)
			.description("Skips to the next track in queue, stops playing if there are no more queued tracks."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var vc = c.getConnectedAChannel();
		if (vc == null)
			throw nothingIsPlaying(c);

		int amount = c.arg(TRACKS).map(Argument::valueAsInt).orElse(1);
		if (amount < 1)
			throw c.error("Can not skip backwards", FAILURE);

		var sched = getMusicManager(vc).getScheduler();
		var track = sched.skipTrack(amount);

		if (track != null)
			c.replyf(FORMAT_PLAY_TRACK, SUCCESS, sched.isLoop() ? EMOJI_LOOP : EMOJI_PLAYING,
					 escape(track.getInfo().title, true), track.getInfo().uri, escape(track.getInfo().author, true));
		else
			c.react(ACCEPT_EMOJI);
	}

}
