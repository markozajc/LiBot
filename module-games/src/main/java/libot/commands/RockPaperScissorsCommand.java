package libot.commands;

import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.module.money.BettableGame.GameResult.*;
import static libot.utils.Utilities.random;

import javax.annotation.Nullable;

import libot.core.commands.CommandCategory;
import libot.module.money.*;
import net.dv8tion.jda.api.entities.Message;

public class RockPaperScissorsCommand extends BettableGame {

	private enum Gesture {
		ROCK,
		PAPER,
		SCISSORS
	}

	private static final String FORMAT_END = """
		_Rock.._
		_Paper.._
		_Scissors!_

		You choose **%s**.
		LiBot chooses **%s**.""";

	@Override
	public GameResult play(BettableGameContext c) throws InterruptedException {
		Gesture player = null;
		while (player == null) {
			var resp = c.askraw("Please pick between **ROCK**, **PAPER**, **SCISSORS**, or **EXIT**.");
			player = selectGesture(c, resp);
		}

		Gesture libot = random(Gesture.values());
		int winner = (player.ordinal() - libot.ordinal() + 4) % 3 - 1;
		return switch (winner) {
			case 1 -> {
				c.replyf("You win!", FORMAT_END, SUCCESS, player, libot);
				yield WIN;
			}
			case -1 -> {
				c.replyf("You lose", FORMAT_END, FAILURE, player, libot);
				yield LOSE;
			}
			case 0 -> {
				c.replyf("Draw", FORMAT_END, DISABLED, player, libot);
				yield RETURN;
			}
			default -> throw c.continuum(winner);
		};
	}

	@Nullable
	private static Gesture selectGesture(BettableGameContext c, Message resp) {
		return switch (resp.getContentDisplay().toLowerCase()) {
			case "rock" -> Gesture.ROCK;
			case "paper" -> Gesture.PAPER;
			case "scissors" -> Gesture.SCISSORS;
			case "exit" -> {
				resp.addReaction(ACCEPT_EMOJI).queue();
				throw c.greturn();
			}
			default -> null;
		};
	}

	@Override
	public String getName() {
		return "rockpaperscissors";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "rps" };
	}

	@Override
	public String getGameInfo() {
		return "Plays a game of \"Rock Paper Scissors\" with LiBot.";
	}

	@Override
	public CommandCategory getCategory() {
		return GAMES;
	}

}
