package libot.core.commands.bettable;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.core.commands.bettable.BettableGame.GameResult.*;
import static libot.core.processes.ProcessManager.getProcesses;
import static libot.utils.Utilities.array;

import javax.annotation.*;

import libot.core.commands.*;
import libot.core.commands.exceptions.runtime.GameEndedException;
import libot.core.data.providers.impl.MoneyProvider;
import libot.core.entities.CommandContext;
import libot.core.processes.ProcessManager;

public abstract class BettableGame extends Command {

	public enum GameResult {
		WIN,
		RETURN,
		QUIT,
		LOSE
	}

	public static class BettableProcessData {

		private final int bet;
		private boolean killed;
		private boolean returned;

		public BettableProcessData(int bet) {
			this.bet = bet;
			this.killed = false;
			this.returned = false;
		}

		public int getBet() {
			return this.bet;
		}

		public boolean isKilled() {
			return this.killed;
		}

		public boolean isReturned() {
			return this.returned;
		}

		public void markKilled() {
			this.killed = true;
		}

		public void markReturned() {
			this.returned = true;
		}

	}

	private static final String FORMAT_DESCRIPTION = """


		**You can bet your LiBot cash on this game.**
		If you win, your bet will be doubled, and if you lose, you'll lose the bet. You can still play the game \
		without betting anything.
		Note that LiBot cash (Ł) is a virtual currency and has no real use. Please do not attempt to buy sandwiches \
		with it.""";
	private static final String FORMAT_ALREADY_RUNNING = """
		You have already placed a bet on **%s**. Would you like to kill it (and lose the bet) before proceeding?""";
	private static final String FORMAT_BET_NEGATIVE = """
		The bet must be positive!""";
	private static final String FORMAT_INSUFFICIENT_BALANCE =
		"You don't have that much money! Check your balance with `%smoney`.";

	@Nonnull
	public abstract GameResult play(@Nonnull BettableGameContext c) throws Exception;

	@Nonnull
	public abstract String getGameInfo();

	@Override
	public void execute(CommandContext c) throws Exception {
		int bet = getBet(c);
		var result = playRaw(new BettableGameContext(c, bet));
		if (bet != 0 && result != null)
			cashout(c, bet, result);
	}

	private static int getBet(@Nonnull CommandContext c) {
		if (c.params().check(0)) {
			var provider = c.provider(MoneyProvider.class);
			int bet = c.params().getInt(0);
			checkArgument(c, provider, bet);
			checkIfAlreadyRunning(c);
			initializeBettable(c, provider, bet);
			return bet;
		} else {
			return 0;
		}
	}

	private static void checkArgument(@Nonnull CommandContext c, @Nonnull MoneyProvider provider, long bet) {
		if (bet < 0)
			throw c.error(FORMAT_BET_NEGATIVE, FAILURE);

		if (bet > provider.getBalance(c.getUserIdLong()))
			throw c.errorf(FORMAT_INSUFFICIENT_BALANCE, FAILURE, c.getEffectivePrefix());
	}

	private static void checkIfAlreadyRunning(@Nonnull CommandContext c) {
		getProcesses().stream()
			.filter(p -> p.getUserId() == c.getUserIdLong())
			.filter(p -> p.getData() instanceof BettableProcessData)
			.findAny()
			.ifPresent(p -> {
				boolean kill = c.confirmf(FORMAT_ALREADY_RUNNING, FAILURE, p.getCommand().getName());
				if (kill) {
					((BettableProcessData) p.getData()).markKilled();
					ProcessManager.interrupt(p);
				} else {
					throw c.cancel();
				}
			});
	}

	private static void initializeBettable(@Nonnull CommandContext c, @Nonnull MoneyProvider provider, int bet) {
		provider.takeMoney(c.getUserIdLong(), bet);
		ProcessManager.getCurrentProcess().setData(new BettableProcessData(bet));
	}

	@Nullable
	private GameResult playRaw(@Nonnull BettableGameContext c) throws Exception {
		try {
			return play(c);

		} catch (GameEndedException e) {
			return e.getResult();

		} catch (InterruptedException e) { // NOSONAR no
			if (ProcessManager.getCurrentProcess().getData() instanceof BettableProcessData data) {
				if (data.isKilled())
					return QUIT;
				else if (data.isReturned())
					return null;
				else
					return RETURN;
			} else {
				return null;
			}
		}
	}

	private static void cashout(@Nonnull CommandContext c, long bet, @Nonnull GameResult result) {
		long earned = switch (result) {
			case WIN -> {
				c.replyf("You won! **(+%dŁ)**", SUCCESS, bet);
				yield bet * 2;
			}
			case RETURN -> {
				c.replyf("Your bet has been returned.", DISABLED, bet);
				yield bet;
			}
			case QUIT -> {
				c.replyf("You quit. **(-%dŁ)**", FAILURE, bet);
				yield 0;
			}
			case LOSE -> {
				c.replyf("You lost. **(-%dŁ)**", FAILURE, bet);
				yield 0;
			}
		};

		c.provider(MoneyProvider.class).addMoney(c.getUserIdLong(), earned);
	}

	@Override
	public String getInfo() {
		return getGameInfo() + FORMAT_DESCRIPTION;
	}

	@Override
	public String[] getParameters() {
		return array("bet");
	}

	@Override
	public String[] getParameterInfo() {
		return array("amount of Ł to bet (leave empty to play without betting)");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return GAMES;
	}

}
