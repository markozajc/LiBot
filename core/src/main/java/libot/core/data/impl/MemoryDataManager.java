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
package libot.core.data.impl;

import java.io.IOException;
import java.util.Properties;

import libot.core.data.DataManager;

public class MemoryDataManager implements DataManager {

	protected final Properties props;

	public MemoryDataManager() {
		this.props = new Properties(10);
	}

	@Override
	public void set(String key, String value) throws IOException {
		if (value != null)
			this.props.setProperty(key, value);
		else
			this.props.remove(key);
	}

	@Override
	public String get(String key) {
		return this.props.getProperty(key);
	}

}
