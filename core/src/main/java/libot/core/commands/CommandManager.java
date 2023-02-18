package libot.core.commands;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static libot.utils.ReflectionUtils.scanClasspath;

import java.util.*;

import javax.annotation.*;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.*;

public class CommandManager implements Iterable<Command> {

	private static final Logger LOG = LoggerFactory.getLogger(CommandManager.class);

	@Nonnull private final Set<Command> commands;

	@Nonnull
	public static CommandManager fromClasspath() {
		var commands = scanClasspath(Command.class, libot.commands.Anchor.class);
		LOG.info("Loaded {} commands", commands.size());
		return new CommandManager(commands);
	}

	private CommandManager(@Nonnull Set<Command> commands) {
		this.commands = commands;
	}

	@Nullable
	public Command get(@Nonnull String name) {
		var namel = name.toLowerCase();
		return this.commands.stream()
			.filter(cmd -> cmd.getName().equals(namel) || ArrayUtils.contains(cmd.getAliases(), namel))
			.findAny()
			.orElse(null);
	}

	@Nonnull
	@SuppressWarnings("null")
	public <T extends Command> Command get(Class<T> clazz) {
		return this.commands.stream()
			.filter(c -> c.getClass() == clazz)
			.findAny()
			.orElseThrow(() -> new IllegalStateException(clazz.getCanonicalName() + " is not registered"));
	}

	@Nonnull
	@SuppressWarnings("null")
	public Set<Command> getAll() {
		return unmodifiableSet(this.commands);
	}

	@Nonnull
	@SuppressWarnings("null")
	public Set<Command> getInCategory(CommandCategory category) {
		return this.commands.stream().filter(c -> c.getCategory() == category).collect(toUnmodifiableSet());
	}

	@Override
	public Iterator<Command> iterator() {
		return this.commands.iterator();
	}

}
