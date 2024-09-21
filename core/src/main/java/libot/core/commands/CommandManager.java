package libot.core.commands;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.*;
import static libot.utils.ReflectionUtils.scanClasspath;

import java.util.*;

import javax.annotation.Nonnull;

public class CommandManager implements Iterable<Command> {

	@Nonnull private final Set<Command> commands;

	@Nonnull
	@SuppressWarnings("null")
	public static CommandManager fromClasspath() {
		return new CommandManager(scanClasspath(Command.class, libot.commands.Anchor.class)
			.collect(toCollection(HashSet::new)));
	}

	private CommandManager(@Nonnull Set<Command> commands) {
		this.commands = commands;
	}

	@Nonnull
	@SuppressWarnings("null")
	public Optional<Command> get(@Nonnull String name) {
		var namel = name.toLowerCase();
		return this.commands.stream()
			.filter(cmd -> cmd.getName().equals(namel) || cmd.getAliases().contains(namel))
			.findAny();
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
	public Set<Command> getAllDirect() {
		return this.commands;
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

	public int size() {
		return this.commands.size();
	}

}
