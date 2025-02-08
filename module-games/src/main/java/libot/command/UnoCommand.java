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
package libot.command;

import static java.lang.Integer.parseInt;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.command.CommandCategory.GAMES;
import static libot.module.money.BettableGame.GameResult.*;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;
import static org.eu.zajc.juno.cards.UnoCardColor.*;
import static org.eu.zajc.juno.rules.pack.impl.UnoOfficialRules.UnoHouseRule.PROGRESSIVE;
import static org.eu.zajc.juno.rules.pack.impl.house.UnoProgressiveRulePack.getConsecutive;
import static org.eu.zajc.juno.utils.UnoRuleUtils.getProhibitingRule;

import javax.annotation.Nonnull;

import org.eu.zajc.juno.cards.*;
import org.eu.zajc.juno.cards.impl.UnoDrawCard;
import org.eu.zajc.juno.decks.impl.UnoStandardDeck;
import org.eu.zajc.juno.game.*;
import org.eu.zajc.juno.players.UnoPlayer;
import org.eu.zajc.juno.players.impl.UnoStrategicPlayer;
import org.eu.zajc.juno.rules.impl.placement.DrawPlacementRules.DrawFourHitchPlacementRule;
import org.eu.zajc.juno.rules.pack.impl.UnoOfficialRules;
import org.eu.zajc.juno.rules.pack.impl.house.UnoProgressiveRulePack;
import org.eu.zajc.juno.utils.UnoGameUtils;

import libot.core.command.CommandMetadata;
import libot.core.command.exception.runtime.CanceledException;
import libot.core.extension.EmbedPrebuilder;
import libot.module.money.*;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class UnoCommand extends BettableGame {

	public UnoCommand() {
		super(CommandMetadata.builder(GAMES, "uno").description("""
			Plays a game of UNO with the official rules + Progressive UNO rule using the original deck of 108 cards. \
			Read more about UNO here: https://en.wikipedia.org/wiki/Uno_(card_game).
			Made using [JUNO](https://github.com/markozajc/JUNO)."""));
	}

	private static final String INVALID_NUMBER = "Please input a number.";
	private static final String PLACEHOLDER_EMOJI = "\uD83D\uDFE6";
	private static final String UNO_LOGO_URL = "https://libot.eu.org/img/uno.png";

	private static class DiscordUnoGame extends UnoControlledGame {

		@Nonnull private final StringBuilder feed;

		public DiscordUnoGame(@Nonnull BettableGameContext c, @Nonnull StringBuilder feed) {
			super(UnoStandardDeck.getDeck(), 7, UnoOfficialRules.getPack(PROGRESSIVE), RANDOM,
				  new DiscordPlayer(c, feed), new UnoStrategicPlayer("LiBot"));
			this.feed = feed;
		}

		@Override
		public void onEvent(String format, Object... arguments) {
			this.feed.append(format.formatted(arguments) + "\n");
		}

	}

	private static class DiscordPlayer extends UnoPlayer {

		@Nonnull private final BettableGameContext c;
		@Nonnull private final EmbedPrebuilder e;
		@Nonnull private final StringBuilder feed;

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
		public UnoCard playCard(UnoGame game) {
			reportChooseAction(game, game.getNextPlayer());
			return inputAction(game);
		}

		@SuppressWarnings("null")
		private UnoCard inputAction(@Nonnull UnoGame game) {
			while (true) {
				String response = this.c.ask().toLowerCase();
				if (response.endsWith("exit"))
					confirmExit(this.c);

				int choice;
				try {
					choice = parseInt(response);
					if (choice == 0)
						return null;
				} catch (NumberFormatException ex) {
					this.c.reply(INVALID_NUMBER);
					continue;
				}

				if (choice > getCards().size() || choice < 0) {
					this.c.reply("Invalid choice!");
					continue;
				}

				UnoCard card = this.getCards().get(choice - 1);
				if (UnoGameUtils.canPlaceCard(this, game, card)) {
					return card;

				} else {
					var rule = getProhibitingRule(game.getTopCard(), card, game.getRules(), getHand());

					if (rule instanceof DrawFourHitchPlacementRule) {
						this.c.replyf("""
							Invalid choice! You may only place a **%s** when you hold no cards the same color as \
							the top card (%s).""", getEmojiWithName(this.c, card),
									  getEmojiWithName(this.c, game.getTopCard()));

					} else if (card instanceof UnoDrawCard d && rule instanceof UnoProgressiveRulePack.PlacementRule) {
						this.c.replyf("""
							Invalid choice! You may only place a **%s** on Draw %d cards.""",
									  getEmojiWithName(this.c, card), d.getAmount());

					} else {
						this.c.reply("Invalid choice!");
					}

				}

			}
		}

		private static void confirmExit(@Nonnull BettableGameContext c) {
			if (c.confirmf("Are you sure that you want to exit this UNO game%s?",
						   c.hasBet() ? " **(you'll lose your full bet)**" : "")) {

				throw c.glose();
			}
		}

		@SuppressWarnings("null")
		private void reportChooseAction(@Nonnull UnoGame game, @Nonnull UnoPlayer next) {
			this.e.clearFields();
			appendFeed();
			this.e.addField("Game info", constructStatus(game, next), false);
			this.e.addField("Choose your action", constructActions(game, false), false);
			this.c.reply(this.e);
		}

		private String constructActions(@Nonnull UnoGame game, boolean placeholderEmoji) {
			var actions = new StringBuilder();
			var drawCards = getConsecutive(game.getDiscard());
			if (!drawCards.isEmpty()) {
				actions.append("``0 -`` Draw **");
				actions.append(drawCards.size() * drawCards.get(0).getAmount());
				actions.append("** cards from a ");
				actions.append(getEmojiWithName(this.c, game.getTopCard(), placeholderEmoji));

			} else {
				actions.append("``0 -`` ");
				actions.append(getPileEmoji(this.c, placeholderEmoji));
				actions.append(" Draw");
			}

			var cards = this.getCards();
			for (int i = 0; i < cards.size(); i++) {
				actions.append("\n``");
				actions.append(i + 1);
				actions.append(" -`` ");
				actions.append(getEmojiWithName(this.c, cards.get(i), placeholderEmoji));
			}

			if (!placeholderEmoji && actions.length() > MessageEmbed.VALUE_MAX_LENGTH)
				return constructActions(game, true);
			else
				return actions.toString();
		}

		private String constructStatus(@Nonnull UnoGame game, @Nonnull UnoPlayer next) {
			var status = new StringBuilder();
			status.append("__Top card: ");
			status.append(getEmojiWithName(this.c, game.getTopCard()));
			status.append("__\n");
			status.append(next.getName());
			status.append("'s hand size: ");
			status.append(next.getHand().getSize());
			status.append(' ');
			status.append(getPileEmoji(this.c, false));
			if (next.getHand().getSize() == 1)
				status.append(" __**UNO!**__");
			return status.toString();
		}

		@Override
		@SuppressWarnings("null")
		public UnoCardColor chooseColor(UnoGame game) {
			reportChooseColor(game);
			return inputColor();
		}

		@SuppressWarnings("null")
		private void reportChooseColor(@Nonnull UnoGame game) {
			this.e.clearFields();
			appendFeed();
			var top = game.getDiscard().getTop();
			this.e.addFieldf(true, "Top card", getEmojiWithName(this.c, top));

			this.e.addField("Your cards", createCardList(false), true);
			this.e.addField("Choose a color", """
				1 - Yellow
				2 - Red
				3 - Green
				4 - Blue""", false);
			this.c.reply(this.e);
		}

		private String createCardList(boolean placeholderEmoji) {
			var cards = this.getHand()
				.getCards()
				.stream()
				.map(card -> getEmojiWithName(this.c, card, placeholderEmoji))
				.collect(joining(" "));
			if (!placeholderEmoji && cards.length() > MessageEmbed.VALUE_MAX_LENGTH)
				return createCardList(true);
			else
				return cards;
		}

		private UnoCardColor inputColor() {
			UnoCardColor color = null;
			while (color == null) {
				String response = this.c.ask().toLowerCase();
				if (response.endsWith("exit"))
					confirmExit(this.c);

				try {
					color = switch (parseInt(response)) {
						case 1 -> YELLOW;
						case 2 -> RED;
						case 3 -> GREEN;
						case 4 -> BLUE;
						default -> {
							this.c.reply("Invalid choice!");
							yield null;
						}
					};
				} catch (NumberFormatException ex) {
					this.c.reply(INVALID_NUMBER);
				}
			}
			return color;
		}

		@Override
		public boolean shouldPlayDrawnCard(UnoGame game, UnoCard drawnCard) {
			return this.c.confirmf(true, "You draw a %s. Would you like to place it?", LITHIUM,
								   getEmojiWithName(this.c, drawnCard));
		}

		@SuppressWarnings("null")
		private void appendFeed() {
			if (!this.feed.isEmpty())
				this.e.setDescription(codeblock(this.feed.toString()));
			this.feed.setLength(0);
		}

		@Nonnull
		private static String getEmojiWithName(BettableGameContext c, UnoCard card) {
			return getEmojiWithName(c, card, false);
		}

		@Nonnull
		@SuppressWarnings("null")
		private static String getEmojiWithName(BettableGameContext c, UnoCard card, boolean placeholderEmoji) {
			var emoji = placeholderEmoji ? PLACEHOLDER_EMOJI
				: c.getShredder()
					.getEmojiResource(card.toString().toLowerCase().replace(" ", ""), MISSING_EMOJI)
					.getFormatted();
			return emoji + " " + card;
		}

		@Nonnull
		private static String getPileEmoji(BettableGameContext c, boolean placeholderEmoji) {
			return placeholderEmoji ? PLACEHOLDER_EMOJI
				: c.getShredder().getEmojiResource("pile", MISSING_EMOJI).getFormatted();
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
				c.reply("Draw", """
					Both discard and draw piles were emptied, the player with least cards wins.
					Both players has the same amount of cards.""", DISABLED);
				return REFUND;
			}
		} catch (CanceledException ce) {
			return LOSE;
		}
	}

}
