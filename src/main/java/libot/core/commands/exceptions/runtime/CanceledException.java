package libot.core.commands.exceptions.runtime;

import libot.core.commands.exceptions.CommandException;

public class CanceledException extends CommandException {

	public CanceledException() {
		super(false);
	}
}
