package libot.core.commands;

import static java.util.Collections.*;
import static java.util.Optional.empty;

import java.util.*;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList;
import libot.core.argument.ParameterList.Parameter;
import net.dv8tion.jda.api.Permission;

public record CommandMetadata(CommandCategory category, String name, String id, Optional<String> description,
							  Set<String> aliases, Set<Permission> permissions, long ratelimit, String ratelimitBucket,
							  ParameterList parameters) {

	public static class Builder {

		@Nonnull private final CommandCategory category;
		@Nonnull private final String name;
		@Nonnull private String id;
		@SuppressWarnings("null") @Nonnull private Optional<String> description = empty();
		@SuppressWarnings("null") @Nonnull private Set<String> aliases = emptySet();
		@SuppressWarnings("null") @Nonnull private Set<Permission> permissions = emptySet();
		private long ratelimit = 0;
		@Nonnull private String ratelimitBucket;
		@Nonnull private ParameterList parameters = ParameterList.empty();

		public Builder(@Nonnull CommandCategory category, @Nonnull String name) {
			this.category = category;
			this.name = this.id = this.ratelimitBucket = name;
		}

		@Nonnull
		public Builder id(@Nonnull String id) {
			this.id = id;
			return this;
		}

		@Nonnull
		@SuppressWarnings("null")
		public Builder description(@Nonnull String description) {
			this.description = Optional.of(description);
			return this;
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
		@SuppressWarnings("null")
		public Builder permissions(@Nonnull Permission... permissions) {
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
		@SuppressWarnings("null")
		public Builder permissions(@Nonnull Collection<Permission> permissions) {
			if (permissions.isEmpty())
				this.permissions = emptySet();
			else
				this.permissions = unmodifiableSet(EnumSet.copyOf(permissions));
			return this;
		}

		@Nonnull
		public Builder ratelimit(long ratelimit) {
			this.ratelimit = ratelimit;
			return this;
		}

		@Nonnull
		public Builder ratelimit(long ratelimit, @Nonnull String ratelimitBucket) {
			this.ratelimit = ratelimit;
			this.ratelimitBucket = ratelimitBucket;
			return this;
		}

		@Nonnull
		public Builder parameters(@Nonnull Parameter... parameters) {
			this.parameters = ParameterList.of(parameters);
			return this;
		}

		@Nonnull
		public CommandMetadata build() {
			return new CommandMetadata(this.category, this.name, this.id, this.description, this.aliases,
									   this.permissions, this.ratelimit, this.ratelimitBucket, this.parameters);
		}

	}

}
