package libot.core.commands.exceptions;

import javax.annotation.*;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.*;

public class CommandException extends RuntimeException {

	@Nullable
	private final transient Message errorMessage;
	private final boolean registerRatelimit;

	public CommandException(@Nullable MessageBuilder builder, boolean registerRatelimit) {
		this.errorMessage = builder == null ? null : builder.build();
		this.registerRatelimit = registerRatelimit;
	}

	public CommandException(boolean registerRatelimit) {
		this.errorMessage = null;
		this.registerRatelimit = registerRatelimit;
	}

	public boolean doesRegisterRatelimit() {
		return this.registerRatelimit;
	}

	public void sendMessage(@Nonnull TextChannel channel) {
		var message = this.errorMessage;
		if (message != null && channel.canTalk())
			channel.sendMessage(message).queue();
	}
}
