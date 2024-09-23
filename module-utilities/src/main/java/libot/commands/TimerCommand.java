package libot.commands;

import static java.lang.System.currentTimeMillis;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.ParseUtils.parseTime;
import static net.dv8tion.jda.api.utils.TimeFormat.DATE_TIME_LONG;
import static org.apache.commons.lang3.time.DurationFormatUtils.formatDurationWords;

import javax.annotation.Nonnull;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.*;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.TimerProvider;
import libot.providers.TimerProvider.UserTimer;

public class TimerCommand extends Command {

	@Nonnull private static final MandatoryParameter TIME =
		mandatory(POSITIONAL, "time", "[time](https://libot.eu.org/doc/commands/parameter-types.html) to set");
	@Nonnull private static final Parameter TEXT = optional(NAMED, "text", "text to display when the timer runs out");

	public TimerCommand() {
		super(CommandMetadata.builder(UTILITIES, "timer")
			.aliases("reminder", "remind", "remindme", "pomodoro")
			.parameters(TIME, TEXT)
			.description("Sets up a timer that will remind you (through direct messages) upon running out."));
	}

	@Override
	public void execute(CommandContext c) {
		long time = parseTime(c.arg(TIME).value());

		if (time < 0) {
			// can't happen right now, keeping it in case parseTime is changed to support past
			// timestamps
			throw c.error("""
				Sorry, the time travel module is currently unavailable. Please try again 5 hours ago.""", FAILURE);
		}

		long absTime = currentTimeMillis() + time;
		var timer = new UserTimer(c.arg(TEXT).map(Argument::value).orElse(null), absTime, c);
		c.getProvider(TimerProvider.class).register(timer);

		c.replyf("Timer successfully set for `%s`. You will be reminded on %s.", SUCCESS,
				 formatDurationWords(time, true, true), DATE_TIME_LONG.format(absTime));
	}

}
