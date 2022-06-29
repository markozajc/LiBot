package libot.core.commands.exceptions.runtime;

import libot.core.commands.exceptions.CommandException;

public class NumberOverflowException extends CommandException {

	public NumberOverflowException() {
		super(false);
	}

}
