package libot.core.commands.exceptions.runtime;

import libot.core.commands.exceptions.CommandException;

public class TimeoutException extends CommandException {

	public TimeoutException() {
		super(false);
	}

}
