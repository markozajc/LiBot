package libot.core.commands.exceptions.startup;

public class CommandDisabledException extends CommandStartupException {

	private final boolean global;

	public CommandDisabledException(boolean global) {
		this.global = global;
	}

	public boolean isGlobal() {
		return this.global;
	}
}
