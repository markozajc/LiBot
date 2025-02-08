//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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

import javax.annotation.*;

import libot.core.entity.CommandContext;
import libot.module.money.BettableGame.GameResult;

public class BettableGameContext extends CommandContext {

	private final long bet;

	public BettableGameContext(@Nonnull CommandContext c, long bet) {
		super(c);
		this.bet = bet;
	}

	// ===============* Getters *===============

	public long getBet() {
		return this.bet;
	}

	public boolean hasBet() {
		return getBet() != 0;
	}

	// ===============* g... *===============

	@Nonnull
	@CheckReturnValue
	public GameEndedException gwin(boolean ratelimit) {
		throw new GameEndedException(GameResult.WIN, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException gwin() {
		throw new GameEndedException(GameResult.WIN, false);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException glose(boolean ratelimit) {
		throw new GameEndedException(GameResult.LOSE, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException glose() {
		throw new GameEndedException(GameResult.LOSE, false);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException grefund(boolean ratelimit) {
		throw new GameEndedException(GameResult.REFUND, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException grefund() {
		throw new GameEndedException(GameResult.REFUND, false);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException gquit(boolean ratelimit) {
		throw new GameEndedException(GameResult.QUIT, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException gquit() {
		throw new GameEndedException(GameResult.QUIT, false);
	}

}
