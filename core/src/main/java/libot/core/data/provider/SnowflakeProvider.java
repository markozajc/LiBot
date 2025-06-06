//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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
package libot.core.data.provider;

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public abstract class SnowflakeProvider<V> extends MapProvider<Long, V> {

	protected SnowflakeProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager,
								@Nonnull TypeToken<Map<Long, V>> typeToken, @Nonnull String dataKey) {
		super(shredder, dataManager, typeToken, dataKey);
	}

}
