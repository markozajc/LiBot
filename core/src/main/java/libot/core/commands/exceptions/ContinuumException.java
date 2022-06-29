package libot.core.commands.exceptions;

// When it just can't get any worse
public class ContinuumException extends RuntimeException {

	private final transient Object[] debug;

	public ContinuumException(Object... debug) {
		this.debug = debug;
	}

	public Object[] getDebug() {
		return this.debug;
	}

}
