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
package libot.commands;

import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;
import static net.dv8tion.jda.api.utils.TimeFormat.RELATIVE;
import static org.apache.commons.lang3.StringUtils.capitalize;

import java.util.concurrent.CompletableFuture;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Guild.*;

public class GuildInfoCommand extends Command {

	public GuildInfoCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "guildinfo")
			.aliases("server", "serverinfo", "guild", "gi", "si")
			.description("Displays some information and statistics for the current guild."));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseVerificationLevel(@Nonnull VerificationLevel vl) {
		return codeblock(switch (vl) {
			case NONE -> "None";
			case LOW -> "Low";
			case MEDIUM -> "Medium";
			case HIGH -> "High";
			case VERY_HIGH -> "Highest";
			default -> capitalize(vl.name().toLowerCase());
		});
	}

	@Override
	public void execute(CommandContext c) {
		Guild guild = c.getGuild();

		var e = new EmbedPrebuilder(LITHIUM);
		e.setThumbnail(guild.getIconUrl());
		e.addField("Creation date", RELATIVE.format(guild.getTimeCreated()));
		e.addField("Verification level", parseVerificationLevel(guild.getVerificationLevel()));
		e.addField("ID", codeblock(guild.getId()));
		e.setTitle("Information about " + guild.getName());

		CompletableFuture.allOf(appendMembers(guild, e), appendOwner(guild, e)).thenRun(() -> c.reply(e));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static CompletableFuture<Void> appendOwner(@Nonnull Guild guild, @Nonnull EmbedBuilder builder) {
		return guild.retrieveOwner()
			.submit()
			.thenAccept(owner -> builder.addField("Owner", owner.getUser().getAsMention(), false));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static CompletableFuture<Void> appendMembers(@Nonnull Guild guild, @Nonnull EmbedBuilder builder) {
		long bots = guild.getMembers().stream().map(Member::getUser).filter(User::isBot).count();
		long users = guild.getMembers().size() - bots;

		var members = new StringBuilder();
		members.append(users);
		members.append(" user");
		if (users != 1)
			members.append('s');

		if (bots > 0) {
			members.append(", ");
			members.append(bots);
			members.append(" bot");
			if (bots != 1)
				members.append('s');
		}

		return guild.retrieveMetaData()
			.submit()
			.thenApply(MetaData::getApproximatePresences)
			.thenApply(online -> members + " (%d online)".formatted(online))
			.thenAccept(data -> builder.addField("Members", codeblock(data), false));
	}

}
