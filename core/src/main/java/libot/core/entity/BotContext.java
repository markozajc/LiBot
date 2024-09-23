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
package libot.core.entity;

import static java.util.Arrays.stream;
import static java.util.concurrent.Executors.newScheduledThreadPool;

import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.Nonnull;

import libot.core.BotConfiguration;
import libot.core.command.*;
import libot.core.data.DataManager;
import libot.core.data.provider.*;
import libot.core.listener.EventWaiterListener;
import libot.core.shred.Shredder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

public class BotContext {

	@Nonnull private final ScheduledExecutorService cron;
	@Nonnull private final BotConfiguration config;
	@Nonnull private final CommandManager commands;
	@Nonnull private final DataManager data;
	@Nonnull private final Shredder shredder;
	@Nonnull private final ProviderManager providers;
	@Nonnull private final EventWaiterListener ewl;

	@SuppressWarnings("null")
	public BotContext(@Nonnull BotConfiguration config, @Nonnull CommandManager commands, @Nonnull DataManager data,
					  @Nonnull Shredder shredder, @Nonnull ProviderManager providers,
					  @Nonnull EventWaiterListener ewl) {
		this.cron = newScheduledThreadPool(1);
		this.config = config;
		this.commands = commands;
		this.data = data;
		this.shredder = shredder;
		this.providers = providers;
		this.ewl = ewl;
	}

	@Nonnull
	public BotConfiguration getConfig() {
		return this.config;
	}

	@Nonnull
	public CommandManager getCommands() {
		return this.commands;
	}

	@Nonnull
	public DataManager getData() {
		return this.data;
	}

	@Nonnull
	public Shredder getShredder() {
		return this.shredder;
	}

	@Nonnull
	public ProviderManager getProviders() {
		return this.providers;
	}

	@Nonnull
	public ScheduledExecutorService getCron() {
		return this.cron;
	}

	@Nonnull
	public EventWaiterListener getEventWaiterListener() {
		return this.ewl;
	}

	@Nonnull
	public <T extends Provider<?>> T getProvider(@Nonnull Class<T> clazz) {
		return this.providers.get(clazz);
	}

	@Nonnull
	public <T extends Command> String getCommandName(@Nonnull Class<T> clazz) {
		return getCommands().get(clazz).getName();
	}

	public void messageSysadmins(@Nonnull MessageCreateData message) {
		stream(getConfig().sysadminIds()).forEach(i -> getShredder().sendPrivateMessage(i, message));
	}

	public void messageSysadmins(@Nonnull String message) {
		stream(getConfig().sysadminIds()).forEach(i -> getShredder().sendPrivateMessage(i, message));
	}

	public void messageSysadmins(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... other) {
		stream(getConfig().sysadminIds()).forEach(i -> getShredder().sendPrivateMessageEmbeds(i, embed, other));
	}

	protected BotContext(@Nonnull BotContext bot) {
		this.cron = bot.cron;
		this.config = bot.config;
		this.commands = bot.commands;
		this.data = bot.data;
		this.shredder = bot.shredder;
		this.providers = bot.providers;
		this.ewl = bot.ewl;
	}

}
