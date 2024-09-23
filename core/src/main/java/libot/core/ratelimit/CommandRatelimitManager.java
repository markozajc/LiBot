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
package libot.core.ratelimit;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import libot.core.command.Command;

public class CommandRatelimitManager {

	private static final Map<String, Ratelimit> RATELIMITS = new ConcurrentHashMap<>();

	@Nonnull
	@SuppressWarnings("null")
	public static Ratelimit getRatelimits(@Nonnull Command command) {
		return RATELIMITS.computeIfAbsent(command.getRatelimitBucket(), k -> new Ratelimit(command.getRatelimit()));
	}

	public static long getRemaining(@Nonnull Command command, long user) {
		return getRatelimits(command).check(user);
	}

	public static boolean isRatelimited(@Nonnull Command command, long user) {
		return getRemaining(command, user) > 0;
	}

	private CommandRatelimitManager() {}

}
