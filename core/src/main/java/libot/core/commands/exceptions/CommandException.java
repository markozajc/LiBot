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
package libot.core.commands.exceptions;

import javax.annotation.*;

import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class CommandException extends RuntimeException {

	@Nullable private final transient MessageCreateData errorMessage;
	private final boolean registerRatelimit;

	public CommandException(@Nullable MessageCreateData message, boolean registerRatelimit) {
		this.errorMessage = message;
		this.registerRatelimit = registerRatelimit;
	}

	public CommandException(boolean registerRatelimit) {
		this.errorMessage = null;
		this.registerRatelimit = registerRatelimit;
	}

	public boolean doesRegisterRatelimit() {
		return this.registerRatelimit;
	}

	public void sendMessage(@Nonnull MessageChannelUnion channel) {
		var message = this.errorMessage;
		if (message != null && channel.canTalk())
			channel.sendMessage(message).queue();
	}
}
