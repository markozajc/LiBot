package libot.core.commands.exceptions.startup;

public class RatelimitedException extends CommandStartupException {

	private final long remaining;

	public RatelimitedException(long remaining) {
		this.remaining = remaining;
	}

	public long getRemaining() {
		return this.remaining;
	}

}
