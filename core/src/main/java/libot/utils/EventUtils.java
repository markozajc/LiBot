package libot.utils;

import static java.util.concurrent.TimeUnit.SECONDS;
import static libot.core.Constants.*;

import javax.annotation.Nonnull;

import libot.listeners.EventWaiterListener;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.*;

public class EventUtils {

	private int timeout;
	private final User user;
	private final MessageChannel channel;

	public EventUtils(User user, MessageChannel channel) {
		this.user = user;
		this.channel = channel;
	}

	public EventUtils(User user, MessageChannel channel, int timeout) {
		this(user, channel);
		this.timeout = timeout;
	}

	@Nonnull
	public MessageReaction getReaction(Message message) throws InterruptedException {
		return EventWaiterListener.awaitEvent(p -> {
			GenericMessageReactionEvent e = (GenericMessageReactionEvent) p;
			return e.getUserIdLong() == this.user.getIdLong() && e.getMessageIdLong() == message.getIdLong();
		}, p -> {
			try {
				message.getChannel().retrieveMessageById(message.getIdLong()).complete();
				return false;
			} catch (RuntimeException e) {
				return true;
			}
		}, this.timeout, SECONDS, GenericMessageReactionEvent.class).getReaction();
	}

	@SuppressWarnings("null")
	@Nonnull
	public Message getMessage(String emoji) throws InterruptedException {
		GenericMessageReactionEvent event = EventWaiterListener.awaitEvent(p -> {

			GenericMessageReactionEvent e = (GenericMessageReactionEvent) p;

			return e.getUserIdLong() == this.user.getIdLong() && e.getChannel().getIdLong() == this.channel.getIdLong()
				&& e.getReactionEmote().getName().equals(emoji);

		}, null, this.timeout, SECONDS, GenericMessageReactionEvent.class);

		return event.getChannel().retrieveMessageById(event.getMessageIdLong()).complete();
	}

	@Nonnull
	public Message awaitMessage(boolean ignoreBlank) throws InterruptedException {
		return EventWaiterListener.awaitEvent(p -> {

			var e = (MessageReceivedEvent) p;

			return e.getAuthor().getIdLong() == this.user.getIdLong()
				&& e.getChannel().getIdLong() == this.channel.getIdLong()
				&& (!ignoreBlank || !e.getMessage().getContentRaw().isBlank());

		}, null, this.timeout, SECONDS, MessageReceivedEvent.class).getMessage();
	}

	public boolean awaitBoolean(@Nonnull Message question) throws InterruptedException {
		boolean result = ACCEPT_EMOJI.equals(EventWaiterListener.awaitEvent(p -> {
			MessageReactionAddEvent e = (MessageReactionAddEvent) p;
			String emote = e.getReactionEmote().getName();

			return e.getUserIdLong() == this.user.getIdLong() && e.getMessageIdLong() == question.getIdLong()
				&& (ACCEPT_EMOJI.equals(emote) || DENY_EMOJI.equals(emote));

		}, p -> {
			try {
				question.getChannel().retrieveMessageById(question.getIdLong()).complete();
				return false;
			} catch (RuntimeException e) {
				return true;
			}

		}, this.timeout, SECONDS, MessageReactionAddEvent.class).getReactionEmote().getName());

		question.delete().queue();
		return result;
	}

	public int getTimeout() {
		return this.timeout;
	}

	public void setTimeout(int timeout) {
		this.timeout = timeout;
	}

}
