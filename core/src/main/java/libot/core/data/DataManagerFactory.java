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
package libot.core.data;

import static libot.core.Constants.ENV_DATA_TYPE;
import static libot.utils.Utilities.getenvOrThrow;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class DataManagerFactory {

	public static DataManager fromEnvironment() throws ReflectiveOperationException {
		var className = getenvOrThrow(ENV_DATA_TYPE).toLowerCase() + "datamanager";
		var reflections = new Reflections(libot.core.data.impl.Anchor.class.getPackageName());

		var clazz = reflections.get(Scanners.SubTypes.of(DataManager.class).asClass())
			.stream()
			.filter(c -> className.equals(c.getSimpleName().toLowerCase()))
			.findAny()
			.orElseThrow(() -> new IllegalStateException("Nonexistent requested data manager: " + className));

		return (DataManager) clazz.getDeclaredConstructor().newInstance();
	}

	private DataManagerFactory() {}

}
