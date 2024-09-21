package libot.commands;

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.module.money.BettableGame.GameResult.*;
import static libot.utils.Utilities.random;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.*;
import libot.core.commands.CommandMetadata;
import libot.module.money.*;

public class RockPaperScissorsCommand extends BettableGame {

	@Nonnull private static final MandatoryParameter GESTURE =
		Parameter.mandatory(POSITIONAL, "gesture", "rock/paper/scissors");

	public RockPaperScissorsCommand() {
		super(CommandMetadata.builder(GAMES, "rockpaperscissors").aliases("rps").parameters(GESTURE).description("""
			Plays a game of [Rock Paper Scissors](https://en.wikipedia.org/wiki/Rock_paper_scissors) with LiBot."""));
	}

	private enum Gesture {
		ROCK,
		PAPER,
		SCISSORS
	}

	private static final String FORMAT_OUTCOME = """
		_Rock.._
		_Paper.._
		_Scissors!_

		You choose **%s**.
		LiBot chooses **%s**.""";

	@Override
	public GameResult play(BettableGameContext c) throws InterruptedException {
		Gesture player = switch (c.arg(GESTURE).value().toLowerCase()) {
			case "rock", "r" -> Gesture.ROCK;
			case "paper", "p" -> Gesture.PAPER;
			case "scissors", "scissor", "s" -> Gesture.SCISSORS;
			default -> {
				c.reply("Please pick between **ROCK**, **PAPER**, **SCISSORS**");
				throw c.grefund();
			}
		};

		Gesture libot = random(Gesture.values());
		int winner = (player.ordinal() - libot.ordinal() + 4) % 3 - 1;
		return switch (winner) {
			case 1 -> {
				c.replyf("You win!", FORMAT_OUTCOME, SUCCESS, player, libot);
				yield WIN;
			}
			case -1 -> {
				c.replyf("You lose", FORMAT_OUTCOME, FAILURE, player, libot);
				yield LOSE;
			}
			case 0 -> {
				c.replyf("Draw", FORMAT_OUTCOME, DISABLED, player, libot);
				yield REFUND;
			}
			default -> throw c.continuum(winner);
		};
	}

}
