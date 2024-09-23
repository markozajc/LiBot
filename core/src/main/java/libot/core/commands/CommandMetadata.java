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
package libot.core.commands;

import static java.util.Collections.*;
import static java.util.Optional.empty;

import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList;
import libot.core.argument.ParameterList.Parameter;
import net.dv8tion.jda.api.Permission;

public record CommandMetadata(CommandCategory category, String name, String id, Optional<String> description,
							  Set<String> aliases, Set<Permission> permissions, boolean checkPermissionsAtStartup,
							  boolean requireDjRole, long ratelimitMillis, String ratelimitBucket,
							  ParameterList parameters) {

	@Nonnull
	public static CommandMetadata.Builder builder(@Nonnull CommandCategory category, @Nonnull String name) {
		return new Builder(category, name);
	}

	public static class Builder {

		@Nonnull private final CommandCategory category;
		@Nonnull private final String name;
		@Nonnull private String id;
		@SuppressWarnings("null") @Nonnull private Optional<String> description = empty();
		@SuppressWarnings("null") @Nonnull private Set<String> aliases = emptySet();
		@SuppressWarnings("null") @Nonnull private Set<Permission> permissions = emptySet();
		private boolean checkPermissionsAtStartup = true;
		private boolean requireDjRole = false;
		private long ratelimitMillis = 0;
		@Nonnull private String ratelimitBucket;
		@Nonnull private ParameterList parameters = ParameterList.empty();

		private Builder(@Nonnull CommandCategory category, @Nonnull String name) {
			this.category = category;
			this.name = this.id = this.ratelimitBucket = name;
		}

		@Nonnull
		public CommandCategory getCategory() {
			return this.category;
		}

		@Nonnull
		public String getName() {
			return this.name;
		}

		@Nonnull
		public Builder id(@Nonnull String id) {
			this.id = id;
			return this;
		}

		@Nonnull
		public String getId() {
			return this.id;
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder description(@Nonnull String description) {
			this.description = Optional.of(description);
			return this;
		}

		@Nonnull
		public Optional<String> getDescription() {
			return this.description;
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder aliases(@Nonnull String... aliases) {
			this.aliases = Set.of(aliases);
			return this;
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder aliases(@Nonnull Collection<String> aliases) {
			this.aliases = Set.copyOf(aliases);
			return this;
		}

		@Nonnull
		public Set<String> getAliases() {
			return this.aliases;
		}

		@Nonnull
		public Builder permissions(@Nonnull Permission... permissions) {
			return permissions(this.checkPermissionsAtStartup, permissions);
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder permissions(boolean checkPermissionsAtStartup, @Nonnull Permission... permissions) {
			this.checkPermissionsAtStartup = checkPermissionsAtStartup;

			if (permissions.length == 0) {
				this.permissions = emptySet();

			} else {
				var newPermissions = EnumSet.noneOf(Permission.class);
				for (var permission : permissions)
					newPermissions.add(permission); // NOSONAR destination is not an array
				this.permissions = unmodifiableSet(newPermissions);
			}

			return this;
		}

		@Nonnull
		public Builder permissions(@Nonnull Collection<Permission> permissions) {
			return permissions(this.checkPermissionsAtStartup, permissions);
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder permissions(boolean checkPermissionsAtStartup, @Nonnull Collection<Permission> permissions) {
			this.checkPermissionsAtStartup = checkPermissionsAtStartup;

			if (permissions.isEmpty())
				this.permissions = emptySet();
			else
				this.permissions = unmodifiableSet(EnumSet.copyOf(permissions));
			return this;
		}

		@Nonnull
		public Set<Permission> getPermissions() {
			return this.permissions;
		}

		public boolean doesCheckPermissionsAtStartup() {
			return this.checkPermissionsAtStartup;
		}

		@Nonnull
		public Builder requireDjRole(boolean requireDjRole) {
			this.requireDjRole = requireDjRole;
			return this;
		}

		public boolean doesRequireDjRole() {
			return this.requireDjRole;
		}

		@Nonnull
		public Builder ratelimitMillis(long ratelimitMillis) {
			if (ratelimitMillis < 0)
				throw new IllegalArgumentException("Negative ratelimit value");
			this.ratelimitMillis = ratelimitMillis;
			return this;
		}

		@Nonnull
		public Builder ratelimitMillis(long ratelimitMillis, @Nonnull String ratelimitBucket) {
			this.ratelimitBucket = ratelimitBucket;
			return ratelimitMillis(ratelimitMillis);
		}

		@Nonnull
		public Builder ratelimit(long ratelimit, TimeUnit unit) {
			ratelimitMillis(unit.toMillis(ratelimit));
			return this;
		}

		@Nonnull
		public Builder ratelimit(long ratelimit, TimeUnit unit, @Nonnull String ratelimitBucket) {
			this.ratelimitBucket = ratelimitBucket;
			return ratelimit(ratelimit, unit);
		}

		public long getRatelimitMillis() {
			return this.ratelimitMillis;
		}

		@Nonnull
		public String getRatelimitBucket() {
			return this.ratelimitBucket;
		}

		@Nonnull
		public Builder parameters(@Nonnull Parameter... parameters) {
			this.parameters = ParameterList.of(parameters);
			return this;
		}

		@Nonnull
		public Builder parameters(@Nonnull List<Parameter> parameters) {
			this.parameters = ParameterList.of(parameters);
			return this;
		}

		@Nonnull
		public ParameterList getParameters() {
			return this.parameters;
		}

		@Nonnull
		public CommandMetadata build() {
			return new CommandMetadata(this.category, this.name, this.id, this.description, this.aliases,
									   this.permissions, this.checkPermissionsAtStartup, this.requireDjRole,
									   this.ratelimitMillis, this.ratelimitBucket, this.parameters);
		}

	}

}
