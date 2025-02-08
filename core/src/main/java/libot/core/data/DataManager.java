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
package libot.core.data;

import java.io.IOException;

import javax.annotation.*;

public interface DataManager {

	void set(@Nonnull String key, @Nullable String value) throws IOException;

	@Nullable
	String get(@Nonnull String key) throws IOException;

	default String getOrDefault(@Nonnull String key, @Nullable String value) throws IOException {
		var returned = get(key);
		if (returned == null)
			return value;
		return returned;
	}

}
