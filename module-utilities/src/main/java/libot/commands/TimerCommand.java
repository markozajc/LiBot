package libot.commands;

import static java.lang.System.currentTimeMillis;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.ParseUtils.parseTime;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.utils.TimeFormat.DATE_TIME_LONG;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.TimerProvider;
import libot.providers.TimerProvider.UserTimer;

public class TimerCommand extends Command {

	private static final String FORMAT_SUCCESS = "Timer successfully set for `%s`. You will be reminded on %s.";
	private static final String FORMAT_NEGATIVE_DURATION = """
		Sorry, the time travel module is currently unavailable. Please try again before `5` hours.""";

	@Override
	public void execute(CommandContext c) {
		long time = parseTime(c.params().get(0));

		if (time < 0) // can't happen right now, keeping it in case parseTime is changed to support
					  // past timestamps
			throw c.error(FORMAT_NEGATIVE_DURATION, FAILURE);

		var timer =
			new UserTimer(c.getUserIdLong(), c.params().getOrDefault(1, "Beep, beep."), currentTimeMillis() + time);
		c.provider(TimerProvider.class).register(timer);

		c.replyf(FORMAT_SUCCESS, SUCCESS, formatDurationWords(time, true, true), DATE_TIME_LONG.format(time));
	}

	@Override
	public String getName() {
		return "timer";
	}

	@Override
	public String[] getAliases() {
		return array("reminder", "remind", "remindme", "pomodoro");
	}

	@Override
	public String getInfo() {
		return "Sets up a timer that will remind you (through direct messages) upon running out.";
	}

	@Override
	public String[] getParameters() {
		return array("time", "[text]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("[time](https://libot.eu.org/doc/commands/parameter-types.html) to set",
					 "text to display when the timer runs out");
	}

	@Override
	public int getMinParameters() {
		return 1;
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
