package libot.core.commands;

import static java.util.stream.Stream.concat;
import static libot.utils.ReflectionUtils.scanClasspath;

import java.util.*;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.slf4j.*;

public class CommandManager {

	private static final Logger LOG = LoggerFactory.getLogger(CommandManager.class);

	@Nonnull private final Map<String, Command> commands;

	@Nonnull
	@SuppressWarnings("null")
	public static CommandManager fromClasspath() {
		var commands = new HashMap<String, Command>();
		scanClasspath(Command.class, libot.commands.Anchor.class).forEach(c -> insertCommand(commands, c));
		return new CommandManager(new HashMap<>(commands)); // recreate map to improve search performance
	}

	private static void insertCommand(@Nonnull Map<String, Command> commands, @Nonnull Command c) {
		concat(Stream.of(c.getName()), c.getAliases().stream()).forEach(name -> {
			var old = commands.put(name, c);
			if (old != null)
				LOG.warn("Commands '{}' and '{}' have a colliding name/alias", c.getClass(), old.getClass());
		});
	}

	private CommandManager(@Nonnull Map<String, Command> commands) {
		this.commands = commands;
	}

	@Nonnull
	@SuppressWarnings("null")
	public Optional<Command> get(@Nonnull String name) {
		return Optional.ofNullable(this.commands.get(name.toLowerCase()));
	}

	@Nonnull
	@SuppressWarnings("null")
	public <T extends Command> Command get(Class<T> clazz) {
		return this.commands.values()
			.stream()
			.filter(c -> c.getClass() == clazz)
			.findAny()
			.orElseThrow(() -> new IllegalStateException(clazz.getCanonicalName() + " is not registered"));
	}

	@Nonnull
	@SuppressWarnings("null")
	public Stream<Command> commands() {
		return this.commands.values().stream().distinct();
	}

	public int size() {
		return this.commands.size();
	}

}
