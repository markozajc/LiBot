package libot.utils;

import java.util.function.Predicate;

import javax.annotation.Nonnull;

import net.dv8tion.jda.api.entities.Member;

public class DiscordUtils {

	private static final char NUMBER_PREFIX = '\u20E3';
	private static final char LETTER_PREFIX = '\uD83C';
	private static final char LETTER_OFFSET_CAPITAL = 65;
	private static final char LETTER_OFFSET_LOWER = 97;
	private static final char LETTER_OFFSET = 56806;
	private static final String NUMBER_TEN = "\uD83D\uDD1F";

	public static final Predicate<Member> NO_BOT = m -> !m.getUser().isBot();

	@SuppressWarnings("null")
	@Nonnull
	public static final String emoji(int i) {
		StringBuilder builder = new StringBuilder();
		if (i < 10) {
			builder.append(i);
			builder.append(NUMBER_PREFIX);
		} else if (i == 10) {
			builder.append(NUMBER_TEN);
		} else {
			throw new IllegalArgumentException("Invalid number: " + i);
		}
		return builder.toString();
	}

	@SuppressWarnings("null")
	@Nonnull
	public static final String emoji(char c) {
		StringBuilder builder = new StringBuilder();
		builder.append(LETTER_PREFIX);
		if (c < LETTER_OFFSET_LOWER)
			builder.append((char) (LETTER_OFFSET - LETTER_OFFSET_CAPITAL + c));
		else
			builder.append((char) (LETTER_OFFSET - LETTER_OFFSET_LOWER + c));
		return builder.toString();
	}

	private DiscordUtils() {}

}
