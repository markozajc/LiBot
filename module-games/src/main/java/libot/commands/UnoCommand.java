package libot.commands;

import static com.github.markozajc.juno.cards.UnoCardColor.*;
import static com.github.markozajc.juno.rules.pack.impl.UnoOfficialRules.UnoHouseRule.PROGRESSIVE;
import static com.github.markozajc.juno.rules.pack.impl.house.UnoProgressiveRulePack.getConsecutive;
import static com.github.markozajc.juno.utils.UnoRuleUtils.combinedPlacementAnalysis;
import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.module.money.BettableGame.GameResult.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;

import java.util.List;

import javax.annotation.Nonnull;

import com.github.markozajc.juno.cards.*;
import com.github.markozajc.juno.decks.impl.UnoStandardDeck;
import com.github.markozajc.juno.game.*;
import com.github.markozajc.juno.players.UnoPlayer;
import com.github.markozajc.juno.players.impl.UnoStrategicPlayer;
import com.github.markozajc.juno.rules.pack.impl.UnoOfficialRules;

import libot.core.commands.CommandCategory;
import libot.core.commands.exceptions.runtime.CanceledException;
import libot.core.extensions.EmbedPrebuilder;
import libot.module.money.*;

public class UnoCommand extends BettableGame {

	private static final String FORMAT_FALLBACK_DRAW = """
		Both discard and draw piles were emptied, the player with least cards wins.
		Both players has the same amount of cards.""";
	private static final String FORMAT_COLOR_CHOOSE = """
		1 - Yellow
		2 - Red
		3 - Green
		4 - Blue""";
	private static final String FORMAT_CONFIRM_EXIT = "Are you sure that you want to exit this UNO game%s?";
	private static final String FORMAT_PLAYER_CONFIRM_PLACE_DRAWN = "You draw a %s. Do you want to place it?";
	private static final String FORMAT_PLAYER_NAN = "Please input a number.";
	private static final String FORMAT_PLAYER_INVALID_CHOICE = "Invalid choice!";
	private static final String FORMAT_PLAYER_ACTION_DRAW = "``0 -`` %s Draw";
	private static final String FORMAT_PLAYER_ACTION = "\n``%d -`` %s";
	private static final String FORMAT_PLAYER_ACTION_DRAW_FORCED = "``0 -`` Draw **%d** cards from a %s";
	private static final String FORMAT_PLAYER_STATUS_BOT = "\n%s's hand size: %d %s%s";
	private static final String FORMAT_PLAYER_STATUS_TOP_CARD = "__Top card: %s__";

	private static final String UNO_LOGO_URL = "https://libot.eu.org/img/uno.png";

	private static class DiscordUnoGame extends UnoControlledGame {

		@Nonnull
		private final StringBuilder feed;

		public DiscordUnoGame(@Nonnull BettableGameContext c, @Nonnull StringBuilder feed) {
			super(new DiscordPlayer(c, feed), new UnoStrategicPlayer("LiBot"), UnoStandardDeck.getDeck(), 7,
				  UnoOfficialRules.getPack(PROGRESSIVE));
			this.feed = feed;
		}

		@Override
		public void onEvent(String format, Object... arguments) {
			this.feed.append(format.formatted(arguments) + "\n");
		}

	}

	private static class DiscordPlayer extends UnoPlayer {

		@Nonnull
		private final BettableGameContext c;
		@Nonnull
		private final EmbedPrebuilder e;
		@Nonnull
		private final StringBuilder feed;
		@Nonnull
		private final StringBuilder status = new StringBuilder();
		@Nonnull
		private final StringBuilder actions = new StringBuilder();

		public DiscordPlayer(@Nonnull BettableGameContext c, @Nonnull StringBuilder feed) {
			super(c.getEffectiveName());
			this.c = c;
			this.feed = feed;
			this.e = new EmbedPrebuilder(LITHIUM);
			this.e.setThumbnail(UNO_LOGO_URL);
			this.e.setTitlef("UNO - %s versus LiBot", escape(c.getUsername()));
			this.e.setFooter("Type in EXIT to exit");
		}

		@Override
		@SuppressWarnings("null")
		public UnoCard playCard(UnoGame game, UnoPlayer next) {
			var possible = combinedPlacementAnalysis(game.getTopCard(), this.getHand().getCards(), game.getRules(),
													 this.getHand());
			reportChooseAction(game, next);
			return inputAction(possible);
		}

		private UnoCard inputAction(@Nonnull List<UnoCard> possible) {
			while (true) {
				String response = this.c.ask().toLowerCase();
				if ("exit".equals(response))
					confirmExit(this.c);

				try {
					int choice = parseInt(response);
					if (choice == 0)
						return null;

					UnoCard card = null;
					boolean valid = choice > getCards().size() || choice < 0
						|| !possible.contains(card = this.getCards().get(choice - 1));
					if (valid)
						this.c.reply(FORMAT_PLAYER_INVALID_CHOICE);
					else
						return card;
				} catch (NumberFormatException ex) {
					this.c.reply(FORMAT_PLAYER_NAN);
				}
			}
		}

		private static void confirmExit(@Nonnull BettableGameContext c) {
			String warning = "";
			if (c.hasBet())
				warning = " **(you'll lose your full bet)**";
			if (c.confirmf(FORMAT_CONFIRM_EXIT, warning))
				throw c.glose();
		}

		private void reportChooseAction(@Nonnull UnoGame game, @Nonnull UnoPlayer next) {
			this.e.clearFields();
			appendFeed();
			constructStatus(game, next);
			this.e.addField("Game info", this.status.toString(), false);
			constructActions(game);
			this.e.addField("Choose your action", this.actions.toString(), false);
			this.c.reply(this.e);
		}

		private void constructActions(@Nonnull UnoGame game) {
			this.actions.setLength(0);
			var drawCards = getConsecutive(game.getDiscard());
			if (!drawCards.isEmpty()) {
				this.actions
					.append(FORMAT_PLAYER_ACTION_DRAW_FORCED.formatted(drawCards.size() * drawCards.get(0).getAmount(),
																	   getEmoteWithName(this.c, game.getTopCard())));
			} else {
				this.actions.append(FORMAT_PLAYER_ACTION_DRAW.formatted(getPileEmote(this.c)));
			}

			int i = 1;
			for (UnoCard card : this.getCards()) {
				this.actions.append(FORMAT_PLAYER_ACTION.formatted(i, getEmoteWithName(this.c, card)));
				i++;
			}
		}

		private void constructStatus(@Nonnull UnoGame game, @Nonnull UnoPlayer next) {
			this.status.setLength(0);
			this.status.append(FORMAT_PLAYER_STATUS_TOP_CARD.formatted(getEmoteWithName(this.c, game.getTopCard())));
			this.status.append(FORMAT_PLAYER_STATUS_BOT
				.formatted(next.getName(), next.getHand().getSize(), getPileEmote(this.c),
						   next.getHand().getSize() == 1 ? " __**UNO!**__" : ""));
		}

		@Override
		@SuppressWarnings("null")
		public UnoCardColor chooseColor(UnoGame game) {
			reportChooseColor(game);
			return inputColor();
		}

		private void reportChooseColor(@Nonnull UnoGame game) {
			this.e.clearFields();
			appendFeed();
			var top = game.getDiscard().getTop();
			this.e.addFieldf(true, "Top card", getEmoteWithName(this.c, top));

			this.e.addField("Your cards",
							this.getHand()
								.getCards()
								.stream()
								.map(card -> getEmoteWithName(this.c, card))
								.collect(joining(" ")),
							true);
			this.e.addField("Choose a color", FORMAT_COLOR_CHOOSE, false);
			this.c.reply(this.e);
		}

		@Nonnull
		private UnoCardColor inputColor() {
			UnoCardColor color = null;
			while (color == null) {
				String response = this.c.ask().toLowerCase();
				if ("exit".equals(response))
					confirmExit(this.c);

				try {
					switch (parseInt(response)) { // TODO 🚫BLOCKED https://bugs.eclipse.org/bugs/show_bug.cgi?id=574905
						case 1 -> color = YELLOW;
						case 2 -> color = RED;
						case 3 -> color = GREEN;
						case 4 -> color = BLUE;
						default -> this.c.reply(FORMAT_PLAYER_INVALID_CHOICE);
					}
				} catch (NumberFormatException ex) {
					this.c.reply(FORMAT_PLAYER_NAN);
				}
			}
			return color;
		}

		@Override
		public boolean shouldPlayDrawnCard(UnoGame game, UnoCard drawnCard, UnoPlayer next) {
			return this.c.confirmf(true, FORMAT_PLAYER_CONFIRM_PLACE_DRAWN, LITHIUM, getEmoteWithName(this.c, drawnCard));
		}

		@SuppressWarnings("null")
		private void appendFeed() {
			if (!this.feed.isEmpty())
				this.e.setDescription(codeblock(this.feed.toString()));
			this.feed.setLength(0);
		}

		@Nonnull
		@SuppressWarnings("null")
		private static String getEmoteWithName(BettableGameContext c, UnoCard card) {
			return "%s %s"
				.formatted(c.shredder().getEmojiResource(card.toString().toLowerCase().replace(" ", ""), MISSING_EMOJI),
						   card);
		}

		@Nonnull
		@SuppressWarnings("null")
		private static String getPileEmote(BettableGameContext c) {
			return c.shredder().getEmojiResource("pile", MISSING_EMOJI);
		}

	}

	@Override
	@SuppressWarnings("null")
	public GameResult play(BettableGameContext c) {
		StringBuilder feed = new StringBuilder();
		UnoGame game = new DiscordUnoGame(c, feed);

		UnoPlayer winner;
		try {
			winner = game.play().getWinner();

			if (winner instanceof DiscordPlayer) {
				c.replyf("You won!", codeblock(feed.toString()), SUCCESS);
				return WIN;

			} else if (winner instanceof UnoStrategicPlayer) {
				c.replyf("You lost", codeblock(feed.toString()), FAILURE);
				return LOSE;

			} else {
				c.reply("Draw", FORMAT_FALLBACK_DRAW, DISABLED);
				return RETURN;
			}
		} catch (CanceledException ce) {
			return LOSE;
		}
	}

	@Override
	public String getName() {
		return "uno";
	}

	@Override
	public String getGameInfo() {
		return """
			Plays a game of UNO with the official rules + Progressive UNO rule using the original deck of 108 cards. \
			Read more about UNO here: https://en.wikipedia.org/wiki/Uno_(card_game).
			Made using [JUNO](https://github.com/markozajc/JUNO).""";
	}

	@Override
	public CommandCategory getCategory() {
		return GAMES;
	}

}
