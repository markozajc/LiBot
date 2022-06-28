package libot.core.commands.exceptions.startup;

public class NotDjException extends CommandStartupException {

	private final long djRoleId;

	public NotDjException(long djRoleId) {
		this.djRoleId = djRoleId;
	}

	public long getDjRoleId() {
		return this.djRoleId;
	}

}
