//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.util;

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
