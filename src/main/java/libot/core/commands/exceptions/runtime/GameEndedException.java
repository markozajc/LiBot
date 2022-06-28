package libot.core.commands.exceptions.runtime;

import javax.annotation.Nonnull;

import libot.core.commands.bettable.BettableGame.GameResult;
import libot.core.commands.exceptions.CommandException;

public class GameEndedException extends CommandException {

	@Nonnull
	private final GameResult result;

	public GameEndedException(@Nonnull GameResult result, boolean registerRatelimit) {
		super(null, registerRatelimit);
		this.result = result;
	}

	@Nonnull
	public GameResult getResult() {
		return this.result;
	}

}
