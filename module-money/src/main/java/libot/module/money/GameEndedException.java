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
package libot.module.money;

import javax.annotation.Nonnull;

import libot.core.commands.exceptions.CommandException;
import libot.module.money.BettableGame.GameResult;

public class GameEndedException extends CommandException {

	@Nonnull private final GameResult result;

	public GameEndedException(@Nonnull GameResult result, boolean registerRatelimit) {
		super(null, registerRatelimit);
		this.result = result;
	}

	@Nonnull
	public GameResult getResult() {
		return this.result;
	}

}
