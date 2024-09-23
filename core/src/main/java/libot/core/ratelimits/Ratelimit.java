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
package libot.core.ratelimits;

import static java.lang.System.currentTimeMillis;

import org.eclipse.collections.api.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;

public class Ratelimit {

	private final long delay;
	private final MutableLongLongMap registered;

	public Ratelimit(long millis) {
		this.registered = LongLongMaps.mutable.empty();
		this.delay = millis;
	}

	public void register(long id) {
		this.registered.put(id, currentTimeMillis());
	}

	public long check(long id) {
		if (!this.registered.containsKey(id))
			return -1;

		long remaining = this.registered.get(id) + this.delay - currentTimeMillis();
		return remaining > 0 ? remaining : -1;
	}

}
