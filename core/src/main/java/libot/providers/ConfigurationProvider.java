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
package libot.providers;

import static java.util.Collections.synchronizedSet;

import java.util.*;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.commands.Command;
import libot.core.data.DataManager;
import libot.core.data.providers.Provider;
import libot.core.shred.Shredder;
import libot.providers.ConfigurationProvider.BotConfiguration;

public class ConfigurationProvider extends Provider<BotConfiguration> {

	public static class BotConfiguration {

		private final Set<String> disabledCommands;

		public BotConfiguration() {
			this(new HashSet<>());
		}

		public BotConfiguration(Set<String> disabledCommands) {
			this.disabledCommands = synchronizedSet(disabledCommands);
		}

		public Set<String> getDisabledCommands() {
			return this.disabledCommands;
		}

	}

	public ConfigurationProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "configuration");
	}

	public boolean isDisabled(Command command) {
		return this.data.getDisabledCommands().contains(command.getId());
	}

	public boolean disable(Command command) {
		if (isDisabled(command))
			return false;
		this.data.getDisabledCommands().add(command.getId());
		return true;
	}

	public boolean enable(Command command) {
		if (!isDisabled(command))
			return false;
		this.data.getDisabledCommands().remove(command.getId());
		return true;
	}

	@Override
	protected BotConfiguration createEmptyData() {
		return new BotConfiguration();
	}

}
