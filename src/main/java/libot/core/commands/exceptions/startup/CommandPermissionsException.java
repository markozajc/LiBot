package libot.core.commands.exceptions.startup;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

import java.util.List;

import net.dv8tion.jda.api.Permission;

public class CommandPermissionsException extends CommandStartupException {

	private final List<Permission> permissions;

	public CommandPermissionsException(Permission... missingPermissions) {
		this.permissions = unmodifiableList(asList(missingPermissions));
	}

	public CommandPermissionsException(List<Permission> missingPermissions) {
		this.permissions = unmodifiableList(missingPermissions);
	}

	public List<Permission> getPermissions() {
		return this.permissions;
	}

}
