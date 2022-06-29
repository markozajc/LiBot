package libot.module.money;

import javax.annotation.*;

import libot.core.entities.CommandContext;
import libot.module.money.BettableGame.GameResult;

public class BettableGameContext extends CommandContext {

	private final int bet;

	public BettableGameContext(@Nonnull CommandContext c, int bet) {
		super(c);
		this.bet = bet;
	}

	// ===============* Getters *===============

	public int getBet() {
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
	public GameEndedException greturn(boolean ratelimit) {
		throw new GameEndedException(GameResult.RETURN, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public GameEndedException greturn() {
		throw new GameEndedException(GameResult.RETURN, false);
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
