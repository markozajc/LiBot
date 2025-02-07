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
package libot.provider;

import static java.util.Collections.synchronizedSet;
import static net.dv8tion.jda.api.Permission.ADMINISTRATOR;

import java.util.*;

import javax.annotation.*;

import com.google.gson.reflect.TypeToken;

import libot.core.command.Command;
import libot.core.data.DataManager;
import libot.core.data.provider.SnowflakeProvider;
import libot.core.entity.CommandContext;
import libot.core.shred.Shredder;
import libot.provider.CustomizationsProvider.Customization;
import net.dv8tion.jda.api.entities.*;

public class CustomizationsProvider extends SnowflakeProvider<Customization> {

	public static class Customization {

		private CustomizationsProvider provider;
		private Set<String> disabledCommands;
		private String commandPrefix;
		private long djRoleId;

		public Customization() {
			this.disabledCommands = synchronizedSet(new HashSet<>());
			this.djRoleId = -1;
		}

		public boolean disable(@Nonnull Command command) {
			boolean added = this.disabledCommands.add(command.getId());
			if (added)
				this.provider.markDirty();
			return added;
		}

		public boolean enable(@Nonnull Command command) {
			boolean removed = this.disabledCommands.remove(command.getId());
			if (removed)
				this.provider.markDirty();
			return removed;
		}

		public boolean isDisabled(@Nonnull Command command) {
			return this.disabledCommands.contains(command.getId());
		}

		@Nonnull
		@SuppressWarnings("null")
		public Optional<String> getCustomPrefix() {
			return Optional.ofNullable(this.commandPrefix);
		}

		@Nonnull
		public Customization setCommandPrefix(@Nullable String commandPrefix) {
			this.commandPrefix = commandPrefix;
			this.provider.markDirty();
			return this;
		}

		@Nonnull
		@SuppressWarnings("null")
		public OptionalLong getDjRoleId() {
			if (this.djRoleId == -1)
				return OptionalLong.empty();
			else
				return OptionalLong.of(this.djRoleId);
		}

		public boolean isDj(@Nonnull Member member) {
			var id = this.getDjRoleId();
			if (id.isPresent()) {
				return member.isOwner() || member.hasPermission(ADMINISTRATOR)
					|| member.getRoles().stream().anyMatch(r -> r.getIdLong() == id.getAsLong());
			} else {
				return true;
			}
		}

		@Nonnull
		public Customization setDjRole(@Nullable Role djRole) {
			if (djRole == null)
				this.djRoleId = -1;
			else
				this.djRoleId = djRole.getIdLong();
			this.provider.markDirty();
			return this;
		}

		@Nonnull
		private Customization setProvider(@Nonnull CustomizationsProvider provider) {
			this.provider = provider;
			return this;
		}

	}

	public CustomizationsProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "custconfig");
	}

	@Nonnull
	public Customization get(long guildId) {
		return this.data.computeIfAbsent(guildId, i -> new Customization()).setProvider(this);
	}

	@Nonnull
	public Customization get(CommandContext c) {
		return get(c.getGuildIdLong());
	}

}
