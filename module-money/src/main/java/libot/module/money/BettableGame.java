package libot.module.money;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.processes.ProcessManager.getProcesses;
import static libot.module.money.BettableGame.GameResult.*;

import java.util.Optional;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import libot.commands.MoneyCommand;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.processes.ProcessManager;
import libot.providers.MoneyProvider;

public abstract class BettableGame extends Command {

	@Nonnull private static final Parameter BET =
		optional(POSITIONAL, "bet", "amount of Ł to bet (leave empty to play without betting)");

	protected BettableGame(@Nonnull CommandMetadata.Builder meta) {
		super(patchMeta(meta));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static CommandMetadata.Builder patchMeta(@Nonnull CommandMetadata.Builder meta) {
		var newDescription = meta.getDescription()
			.orElse("") + """

				**You can bet your LiBot cash on this game.**
				If you win, your bet will be doubled, and if you lose, you'll lose the bet. You can still play the game \
				without betting anything.
				Note that LiBot cash (Ł) is a virtual currency and has no real use. Please do not attempt to buy sandwiches \
				with it.""";

		var newParameters = Stream.concat(meta.getParameters().parameters(), Stream.of(BET)).toList();

		return meta.description(newDescription).parameters(newParameters);
	}

	public enum GameResult {
		WIN,
		REFUND,
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

		public boolean isBetRefunded() {
			return this.returned;
		}

		public void markKilled() {
			this.killed = true;
		}

		public void markBetRefunded() {
			this.returned = true;
		}

	}

	@Nonnull
	public abstract GameResult play(@Nonnull BettableGameContext c) throws Exception;

	@Override
	@SuppressWarnings("null")
	public final void execute(CommandContext c) throws Exception {
		int bet = c.arg(BET).map(Argument::valueAsInt).orElse(0);

		if (bet < 0) {
			throw c.error("The bet must not be a negative number.", FAILURE);

		} else if (bet == 0) {
			playRaw(new BettableGameContext(c, bet));

		} else {
			var provider = c.getProvider(MoneyProvider.class);
			if (bet > provider.getBalance(c.getUserIdLong())) {
				throw c.errorf("You don't have that much money! Check your balance with `%s`.", FAILURE,
							   c.getCommandWithPrefix(MoneyCommand.class));
			}

			killExistingBets(c);

			provider.takeMoney(c.getUserIdLong(), bet);
			ProcessManager.getCurrentProcess().setData(new BettableProcessData(bet));

			playRaw(new BettableGameContext(c, bet)).ifPresent(result -> cashout(c, bet, result));
		}
	}

	private static void killExistingBets(CommandContext c) {
		getProcesses().stream()
			.filter(p -> p.getUserId() == c.getUserIdLong())
			.filter(p -> p.getData() instanceof BettableProcessData)
			.findAny()
			.ifPresent(p -> {
				boolean kill = c.confirmf("""
					You have already placed a bet on **%s**. Would you like to kill it (and lose the bet) before \
					proceeding?""", FAILURE, p.getCommand().getName());
				if (kill) {
					((BettableProcessData) p.getData()).markKilled();
					ProcessManager.interrupt(p);
				} else {
					throw c.cancel();
				}
			});
	}

	@Nonnull
	@SuppressWarnings("null")
	private Optional<GameResult> playRaw(@Nonnull BettableGameContext c) throws Exception {
		try {
			return Optional.of(play(c));

		} catch (GameEndedException e) {
			return Optional.of(e.getResult());

		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			if (ProcessManager.getCurrentProcess().getData() instanceof BettableProcessData data) {
				if (data.isKilled())
					return Optional.of(QUIT);
				else if (data.isBetRefunded())
					return Optional.empty();
				else
					return Optional.of(REFUND);
			} else {
				return Optional.empty();
			}
		}
	}

	private static void cashout(@Nonnull CommandContext c, long bet, @Nonnull GameResult result) {
		long earned = switch (result) {
			case WIN -> {
				c.replyf("You won! **(+%dŁ)**", SUCCESS, bet);
				yield bet * 2;
			}
			case REFUND -> {
				c.replyf("Your bet has been refunded.", DISABLED, bet);
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

		c.getProvider(MoneyProvider.class).addMoney(c.getUserIdLong(), earned);
	}

}
