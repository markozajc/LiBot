package libot.utils;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.ZoneOffset.UTC;
import static java.time.temporal.ChronoUnit.*;
import static java.util.regex.Pattern.*;
import static org.apache.commons.lang3.math.NumberUtils.isDigits;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;
import java.util.regex.*;

import javax.annotation.*;

import libot.core.commands.exceptions.runtime.*;

public class ParseUtils {

	public static record Prefix(@Nonnull String string, long selfId) {}

	private static final Map<Prefix, Pattern> PATTERN_CACHE = new ConcurrentHashMap<>();

	private static final Pattern SPACES = compile("\\s", UNICODE_CHARACTER_CLASS);
	private static final String FORMAT_REGEX = "^(?:<@!?%d>|%s) *([^\\s]+)(?:\\s(.*))?$";

	public static int parseInt(@Nonnull String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			if (isDigits(s))
				throw new NumberOverflowException();
			else
				throw e;
		}

	}

	public static long parseLong(@Nonnull String s) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException e) {
			if (isDigits(s))
				throw new NumberOverflowException();
			else
				throw e;
		}
	}

	@Nullable
	public static String parseCommandName(@Nonnull String input, @Nonnull Prefix prefix) {
		boolean isCommand = input.startsWith(prefix.string()) || input.startsWith("<@!" + prefix.selfId() + ">")
			|| input.startsWith("<@" + prefix.selfId() + ">");
		if (!isCommand)
			return null;

		var matcher = getMatcher(input, prefix);
		if (matcher.find())
			return matcher.group(1);
		else
			return null;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static String[] parseParameters(@Nonnull String input, @Nonnull Prefix prefix, int limit) {
		var matcher = getMatcher(input, prefix);
		String group;
		if (matcher.find() && (group = matcher.group(2)) != null)
			return SPACES.split(group, limit);
		else
			return new String[0];
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Matcher getMatcher(@Nonnull String input, @Nonnull Prefix prefix) {
		var pattern =
			PATTERN_CACHE.computeIfAbsent(prefix, p -> compile(format(FORMAT_REGEX, p.selfId(), quote(p.string())),
															   DOTALL | UNICODE_CHARACTER_CLASS));
		return pattern.matcher(input);
	}

	public static long parseTime(String input) {
		long time;
		if (input.contains(":"))
			time = parseClock(input);
		else
			time = parseRelativeTime(input);

		if (time < 0)
			throw new TimeParseException();
		else
			return time;
	}

	private static long parseClock(String input) {
		String[] parts = input.split(":");
		int[] time = new int[3];
		try {
			for (int i = 0; i < parts.length; i++) {
				time[i] = Integer.parseInt(parts[i]);
			}
		} catch (NumberFormatException e) {
			return -1;
		}
		if (time[0] > 23 || time[1] > 59 || time[2] > 59 || time[0] < 0 || time[1] < 0 || time[2] < 0) {
			return -1;
		}

		var ldt = LocalDateTime.now(UTC);
		if (ldt.getHour() > time[0] || ldt.getHour() == time[0] && ldt.getMinute() > time[1]
			|| ldt.getHour() == time[0] && ldt.getMinute() == time[1] && ldt.getSecond() > time[2]) {
			ldt = ldt.plusDays(1);
		}
		return ldt.withHour(time[0]).withMinute(time[1]).withSecond(time[2]).toInstant(UTC).toEpochMilli()
			   - currentTimeMillis();
	}

	public static long parseRelativeTime(String input) {
		var chars = input.toLowerCase().toCharArray();
		var durationBuffer = new StringBuilder(3);
		long duration = 0;
		for (char c : chars) {
			boolean append = false;
			ChronoUnit unit = switch (c) {
				case 'w' -> WEEKS;
				case 'd' -> DAYS;
				case 'h' -> HOURS;
				case 'm' -> MINUTES;
				case 's' -> SECONDS;
				default -> {
					if (c >= '0' && c <= '9') {
						append = true;
						durationBuffer.append(c);
					}
					yield null;
				}
			};
			if (append)
				continue;
			if (unit == null || durationBuffer.length() == 0) // not a number and not a unit
				return -1;

			duration += unit.getDuration().toMillis() * Long.parseLong(durationBuffer.toString());
			durationBuffer.setLength(0);
		}

		if (durationBuffer.length() != 0)
			duration += TimeUnit.SECONDS.toMillis(Long.parseLong(durationBuffer.toString()));
		// any number without unit is treated as seconds

		return duration;
	}

	private ParseUtils() {}

}
