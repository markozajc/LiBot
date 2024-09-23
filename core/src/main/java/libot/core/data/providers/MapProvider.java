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
package libot.core.data.providers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public abstract class MapProvider<K, V> extends Provider<Map<K, V>> {

	protected MapProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager,
						  @Nonnull TypeToken<Map<K, V>> typeToken, @Nonnull String dataKey) {
		super(shredder, dataManager, typeToken, dataKey);
	}

	@Override
	protected Map<K, V> createEmptyData() {
		return new ConcurrentHashMap<>();
	}

	@Override
	protected Map<K, V> constructData(String json) {
		return new ConcurrentHashMap<>(super.constructData(json));
	}

}
