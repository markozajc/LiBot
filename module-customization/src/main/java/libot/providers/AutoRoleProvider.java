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

import java.util.OptionalLong;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;

public class AutoRoleProvider extends SnowflakeProvider<Long> {

	public AutoRoleProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "autorole");
	}

	public void set(long guildId, long roleId) {
		this.data.put(guildId, roleId);
	}

	public void remove(long guildId) {
		this.data.remove(guildId);
	}

	@Nonnull
	@SuppressWarnings("null")
	public OptionalLong get(long guildId) {
		var value = this.data.get(guildId);
		if (value != null)
			return OptionalLong.of(value);
		else
			return OptionalLong.empty();
	}

}
