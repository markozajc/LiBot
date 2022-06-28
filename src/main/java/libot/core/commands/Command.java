package libot.core.commands;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;
import static org.apache.commons.lang3.StringUtils.rightPad;

import java.util.Arrays;

import javax.annotation.*;

import libot.core.commands.exceptions.ContinuumException;
import libot.core.commands.exceptions.startup.*;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IPermissionHolder;

public abstract class Command {

	private static final int DEFAULT_RATELIMIT = 0;

	public abstract void execute(@Nonnull CommandContext c) throws Exception;

	@Nonnull
	public abstract String getName();

	@Nonnull
	public String[] getAliases() {
		return new String[0];
	}

	@Nonnull
	public abstract String getInfo();

	@Nonnull
	public Permission[] getPermissions() {
		return new Permission[0];
	}

	public int getRatelimit() {
		return DEFAULT_RATELIMIT;
	}

	@Nonnull
	public String getRatelimitId() {
		return getName();
	}

	@Nonnull
	public String[] getParameters() {
		return new String[0];
	}

	@Nonnull
	public String[] getParameterInfo() {
		return new String[0];
	}

	@Nonnegative
	public int getMaxParameters() {
		return getParameters().length;
	}

	@Nonnegative
	public int getMinParameters() {
		return getMaxParameters();
	}

	@Nonnull
	public abstract CommandCategory getCategory();

	public void startupCheck(@Nonnull CommandContext c) throws CommandStartupException {
		checkPermissions(c.getMember());
	}

	public final void checkPermissions(@Nullable IPermissionHolder permissionHolder) {
		if (permissionHolder == null)
			throw new ContinuumException();

		Permission[] permissions = this.getPermissions();

		if (permissionHolder.hasPermission(permissions))
			return;

		throw new CommandPermissionsException(Arrays.asList(permissions)
			.stream()
			.filter(p -> !permissionHolder.hasPermission(p))
			.toList());

	}

	@Nonnull
	@SuppressWarnings("null")
	public final String getUsage(@Nonnull CommandContext c) {
		var u = new StringBuilder();
		u.append(escape(c.getEffectivePrefix(), true));
		u.append(getName());
		var parameters = getParameters();
		if (parameters.length != 0) {
			var additional = getParameterInfo();
			u.append(" __");
			u.append(join("__ __", parameters));
			u.append("__");
			int maxLength = stream(parameters).mapToInt(String::length).max().orElse(0);
			for (int i = 0; i < parameters.length && i < additional.length; i++) {
				u.append("\n` ");
				u.append(rightPad(parameters[i], maxLength));
				u.append(" ` ");
				u.append(additional[i]);
			}
		}
		return u.toString();
	}

	@Nonnegative
	public final int getId() {
		return getName().hashCode();
	}

}
