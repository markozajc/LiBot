//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.core.command.exception.startup;

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
