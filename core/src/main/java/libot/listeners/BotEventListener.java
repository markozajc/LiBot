package libot.listeners;

import javax.annotation.Nonnull;

import libot.core.entities.BotContext;

public interface BotEventListener {

	void onStartup(@Nonnull BotContext bot);

}
