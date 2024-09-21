package libot.commands;

import static java.lang.Math.max;
import static java.util.Collections.shuffle;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.GAMES;
import static libot.module.money.BettableGame.GameResult.*;
import static net.dv8tion.jda.api.utils.MarkdownUtil.*;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import javax.annotation.Nonnull;

import libot.core.commands.CommandMetadata;
import libot.core.extensions.EmbedPrebuilder;
import libot.module.money.*;
import net.dv8tion.jda.api.entities.MessageEmbed.Field;

public class BlackjackCommand extends BettableGame {

	@SuppressWarnings("null")
	public BlackjackCommand() {
		super(CommandMetadata.builder(GAMES, "blackjack").description("""
			Plays a game of Blackjack (%d-deck) against a robot dealer. \
			If you don't know the rules, you can read more about this game \
			[here](https://en.wikipedia.org/wiki/Blackjack).""".formatted(DECK_COUNT)));
	}

	private static final int BLACKJACK = 21;

	private static final int DEALER_SAFE_VALUE = 16;
	private static final int DECK_COUNT = 8;

	private static final String CHEATSHEET =
		"Type HIT to take another card, STAND to let the dealer play or EXIT to exit.";

	private static final List<Card> DECK;
	static {
		DECK = new ArrayList<>(52);
		for (Suit suit : Suit.values()) {
			DECK.add(new Card(suit.getEmoji(), 1, "A"));
			for (int i = 2; i < 11; i++)
				DECK.add(new Card(suit.getEmoji(), i, Integer.toString(i)));
			DECK.add(new Card(suit.getEmoji(), 10, "J"));
			DECK.add(new Card(suit.getEmoji(), 10, "Q"));
			DECK.add(new Card(suit.getEmoji(), 10, "K"));
		}

	}

	private static record Card(String emoji, int value, String display) {}

	private enum Suit {

		HEARTS("\u2665"),
		CLUBS("\u2663"),
		DIAMONDS("\u2666"),
		SPADES("\u2660");

		private final String emoji;

		Suit(String emoji) {
			this.emoji = emoji;
		}

		public String getEmoji() {
			return this.emoji;
		}

	}

	@Nonnull
	private static synchronized Queue<Card> createDeck() {
		var deck = new ArrayDeque<Card>(DECK.size());
		for (int i = 0; i < DECK_COUNT; i++) {
			shuffle(DECK, ThreadLocalRandom.current());
			deck.addAll(DECK);
		}
		return deck;
	}

	@Override
	public GameResult play(BettableGameContext c) {
		var deck = createDeck();
		var player = new ArrayList<Card>(7);
		var dealer = new ArrayList<Card>(7);

		for (int i = 0; i < 2; i++) {
			draw(deck, player);
			draw(deck, dealer);
		}
		report(c, player, dealer, false, false);
		boolean playerHit = true;
		do {
			playerHit = doesPlayerHit(c);
			if (playerHit)
				draw(deck, player);
			checkBusted(c, player, dealer);
			report(c, player, dealer, playerHit, false);
		} while (playerHit);

		boolean dealerHit = true;
		do {
			dealerHit = doesDealerHit(player, dealer);
			if (dealerHit)
				draw(deck, dealer);
			checkBusted(c, player, dealer);
		} while (dealerHit);

		return finish(c, player, dealer);
	}

	public static boolean doesPlayerHit(@Nonnull BettableGameContext c) {
		while (true) {
			switch (c.ask().toLowerCase()) {
				case "hit" -> {
					return true;
				}
				case "stand" -> {
					return false;
				}
				case "exit" -> {
					if (c.confirmf("Are you sure that you want to exit this Blackjack game%s?",
								   c.hasBet() ? " **(you'll lose your full bet)**" : "")) {
						throw c.gquit();
					}
				}
				default -> c.reply(CHEATSHEET, DISABLED);
			}
		}
	}

	public static boolean doesDealerHit(@Nonnull List<Card> player, @Nonnull List<Card> dealer) {
		int knownPlayerCount = player.get(0).value() + player.get(1).value() + (player.size() - 2) * 2;
		// the dealer knows your first two cards and your amount of cards drawn (each of
		// which can be no less than 2)
		return countValues(dealer) < max(DEALER_SAFE_VALUE, knownPlayerCount);
	}

	@Nonnull
	private static GameResult finish(@Nonnull BettableGameContext c, @Nonnull List<Card> player,
									 @Nonnull List<Card> dealer) {
		report(c, player, dealer, false, true);
		int playerCount = countValues(player);
		int dealerCount = countValues(dealer);
		if (playerCount > dealerCount) {
			c.reply("You win!", "You are closer to 21 than the dealer.", SUCCESS);
			return WIN;

		} else if (dealerCount > playerCount) {
			c.reply("You lose", "The dealer is closer to 21 than you are.", FAILURE);
			return LOSE;

		} else {
			c.reply("Draw", "Both hands are equal in value.", DISABLED);
			return REFUND;
		}
	}

	private static void checkBusted(@Nonnull BettableGameContext c, @Nonnull List<Card> player,
									@Nonnull List<Card> dealer) {
		if (countValues(player) > BLACKJACK) {
			report(c, player, dealer, true, true);
			c.replyf("Busted!", "Your hand exceeds 21. You lose.", FAILURE);
			throw c.glose();

		} else if (countValues(dealer) > BLACKJACK) {
			report(c, player, dealer, false, true);
			c.replyf("You win!", "The dealer is busted because their hand exceeds 21. You win.", SUCCESS);
			throw c.gwin();
		}
	}

	private static void report(@Nonnull BettableGameContext c, @Nonnull List<Card> player, @Nonnull List<Card> dealer,
							   boolean playerHit, boolean end) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setTitle("Blackjack");
		e.addField(listHand(c.getEffectiveName(), player, playerHit, false));
		e.addField(listHand("Dealer", dealer, false, !end));
		e.setFooter(CHEATSHEET);
		c.reply(e);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Field listHand(@Nonnull String name, @Nonnull List<Card> hand, boolean hit, boolean hide) {
		var b = new StringBuilder();
		if (hide) {
			b.append(hand.get(0).emoji());
			b.append("**`");
			b.append(hand.get(0).display());
			b.append("`** + **");
			b.append(hand.size() - 1);
			b.append("x** hidden");
		} else {
			for (int i = 0; i < hand.size(); i++) {
				var card = hand.get(i);
				String format;
				if (i == hand.size() - 1 && hit)
					format = "[%s%s]";
				else
					format = " %s%s";
				b.append(format.formatted(card.emoji(), bold(monospace(card.display()))));
			}
		}

		var t = new StringBuilder(name.length() + 22);
		t.append(name);
		t.append("'s cards");
		if (hand.size() > 2) {
			t.append(" [hit ");
			t.append(hand.size() - 2);
			t.append(" time");
			if (hand.size() - 2 != 1)
				t.append('s');
			t.append(']');
		}
		return new Field(t.toString(), b.toString(), false);
	}

	private static void draw(@Nonnull Queue<Card> deck, @Nonnull List<Card> hand) {
		hand.add(deck.remove());
	}

	private static int countValues(@Nonnull List<Card> hand) {
		return hand.stream().mapToInt(Card::value).sum();
	}

}
