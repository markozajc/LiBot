package libot.core.commands.exceptions;

import javax.annotation.*;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class CommandException extends RuntimeException {

	@Nullable private final transient MessageCreateData errorMessage;
	private final boolean registerRatelimit;

	public CommandException(@Nullable MessageCreateData message, boolean registerRatelimit) {
		this.errorMessage = message;
		this.registerRatelimit = registerRatelimit;
	}

	public CommandException(boolean registerRatelimit) {
		this.errorMessage = null;
		this.registerRatelimit = registerRatelimit;
	}

	public boolean doesRegisterRatelimit() {
		return this.registerRatelimit;
	}

	public void sendMessage(@Nonnull MessageChannelUnion channel) {
		var message = this.errorMessage;
		if (message != null && channel.canTalk())
			channel.sendMessage(message).queue();
	}
}
