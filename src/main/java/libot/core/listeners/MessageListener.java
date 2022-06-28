package libot.core.listeners;

import static libot.utils.ParseUtils.parseCommandName;

import javax.annotation.Nonnull;

import libot.core.commands.Command;
import libot.core.data.providers.impl.CustomizationsProvider;
import libot.core.entities.*;
import libot.core.processes.ProcessManager;
import libot.utils.ParseUtils.Prefix;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class MessageListener extends ListenerAdapter {

	@Nonnull
	private final BotContext bot;

	public MessageListener(@Nonnull BotContext bot) {
		this.bot = bot;
	}

	@Override
	public void onGuildMessageReceived(GuildMessageReceivedEvent event) {
		if (event.getAuthor().isBot())
			return;

		var guild = event.getGuild();
		var raw = event.getMessage().getContentRaw();
		var prefixString = this.bot.provider(CustomizationsProvider.class)
			.get(guild.getIdLong())
			.getCustomPrefix()
			.orElse(this.bot.config().defaultPrefix());

		@SuppressWarnings("null")
		Prefix prefix = new Prefix(prefixString, event.getJDA().getSelfUser().getIdLong());
		String commandName = parseCommandName(raw, prefix);
		if (commandName == null)
			return;

		Command command = this.bot.commands().get(commandName);
		if (command == null)
			return;

		ProcessManager.run(new CommandContext(event, command, this.bot, prefix));
	}

}
