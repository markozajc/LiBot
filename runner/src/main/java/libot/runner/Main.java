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
package libot.runner;

public class Main {

	// Due to how Eclipse handles Maven projects, and because I can't simply import
	// modules with runtime scope in libot-core (it would still cause a cyclic dependency
	// in Maven), a top-level project importing all projects with runtime scope (except
	// libot-core, which the main function calls) is required for execution from within
	// the IDE itself. Deal with it.
	public static void main(String[] argv) throws Exception {
		libot.Main.main(argv);
	}

}
