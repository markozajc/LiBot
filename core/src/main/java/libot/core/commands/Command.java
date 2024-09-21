package libot.core.commands;

import static java.util.stream.Collectors.joining;
import static libot.core.argument.ParameterList.Parameter.ParameterType.NAMED;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.*;

import javax.annotation.*;

import libot.core.argument.ParameterList;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.exceptions.ContinuumException;
import libot.core.commands.exceptions.startup.*;
import libot.core.entities.*;
import libot.core.processes.ProcessManager;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;

public abstract class Command {

	@Nonnull private final CommandMetadata meta;

	protected Command(@Nonnull CommandMetadata.Builder meta) {
		this.meta = meta.build();
	}

	public abstract void execute(@Nonnull CommandContext c) throws Exception;

	public final void run(@Nonnull EventContext eventContext, @Nullable String input) {
		ProcessManager.run(this, eventContext, input);
	}

	@Nonnull
	@SuppressWarnings("null")
	public final CommandCategory getCategory() {
		return this.meta.category();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final String getName() {
		return this.meta.name();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final Optional<String> getDescription() {
		return this.meta.description();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final Set<String> getAliases() {
		return this.meta.aliases();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final Set<Permission> getPermissions() {
		return this.meta.permissions();
	}

	public final long getRatelimit() {
		return this.meta.ratelimitMillis();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final String getRatelimitBucket() {
		return this.meta.ratelimitBucket();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final ParameterList getParameters() {
		return this.meta.parameters();
	}

	@Nonnull
	@SuppressWarnings("null")
	public final String getId() {
		return this.meta.id();
	}

	public void startupCheck(@Nonnull EventContext ec) throws CommandStartupException {
		checkPermissions(ec.getMember());
	}

	public final void checkPermissions(@Nullable IPermissionHolder permissionHolder) {
		if (permissionHolder == null)
			throw new ContinuumException();

		if (getPermissions().isEmpty())
			return;

		var missingPermissions = getPermissions().stream().filter(p -> !permissionHolder.hasPermission(p)).toList();

		if (!missingPermissions.isEmpty())
			throw new CommandPermissionsException(missingPermissions);
	}

	@Nonnull
	@SuppressWarnings("null")
	public final String getUsage(@Nonnull EventContext ec) {
		var u = new StringBuilder();
		u.append(escape(ec.getEffectivePrefix(), true));
		u.append(getName());

		var parameters = getParameters().parameters().toList();

		if (!parameters.isEmpty()) {
			u.append(parameters.stream().map(Parameter::toString).collect(joining("__ __", " __", "__")));
			int maxLength = parameters.stream()
				.mapToInt(p -> p.getName().length() + (p.getType() == NAMED ? 2 : 0))
				.max()
				.orElse(0);

			parameters.forEach(p -> {
				u.append("\n` ");
				if (p.getType() == NAMED)
					u.append("--");
				u.append(rightPad(p.getName(), maxLength) + " `");

				p.getDescription().ifPresent(description -> {
					u.append(" ");
					u.append(description);
				});
			});
		}

		return u.toString();
	}

}
