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
package libot.command;

import static java.util.regex.Pattern.compile;
import static libot.command.AboutCommand.LINKS;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.*;
import static libot.core.listener.DeletionRequestListener.DELETION_REACTION;
import static libot.module.ModuleLibotShared.sendUsage;
import static net.dv8tion.jda.api.utils.MarkdownUtil.monospace;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Random;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;

public class HelpCommand extends Command {

	@Nonnull private static final Parameter COMMAND = optional(POSITIONAL, "command", "The command to describe");

	public HelpCommand() {
		super(CommandMetadata.builder(LIBOT, "help").aliases("man", "manual").parameters(COMMAND).description("""
			Direct-messages you a list of all commands. \
			To get detailed information about a command, use help along with the command's name as a parameter."""));
	}

	private static final int LIST_MAX_WIDTH = 70;
	private static final Pattern HYPERLINK_REGEX = compile("\\[(.*?)\\]\\([^\\)]+\\)");

	private static final String FORMAT_NONEXISTANT =
		"`%s` does not (yet) exist! Please try again in approximately `%d` years!";
	private static final String FORMAT_DESCRIPTION = """
		To get detailed information about a command, use `%%s [command]`.
		You can also use %%s as a command prefix.
		LiBot's messages can be deleted by adding a %s reaction.
		%s""".formatted(DELETION_REACTION, LINKS);

	@Override
	public void execute(CommandContext c) throws Exception {
		c.arg(COMMAND).ifPresentOrElse(arg -> about(c, arg), () -> list(c));
	}

	@SuppressWarnings("null")
	private static void list(@Nonnull CommandContext c) {
		var e = new EmbedPrebuilder("LiBot manual", LITHIUM);
		e.setFooter("LiBot v" + VERSION, c.getSelfAvatar());

		int maxLength = c.getCommands().commands().map(Command::getName).mapToInt(String::length).max().orElse(0);
		var b = new StringBuilder();
		for (var category : CommandCategory.values()) {
			if (category == ADMINISTRATIVE && !c.isUserSysadmin())
				continue;

			b.setLength(0);
			c.getCommands().commands().filter(cmd -> cmd.getCategory() == category).forEach(cmd -> {
				b.append(monospace(rightPad(cmd.getName(), maxLength)));

				cmd.getDescription().map(desc -> {
					return abbreviate(HYPERLINK_REGEX.matcher(desc.replace("\n", "")).replaceAll("$1"),
									  LIST_MAX_WIDTH - maxLength);
				}).ifPresent(b::append);

				b.append("\n");
			});
			e.addField(category.toString(), b.toString(), false);
		}

		e.setDescriptionf(FORMAT_DESCRIPTION, c.getCommandWithPrefix(), c.getSelfMention());

		c.direct(e).thenAcceptAsync(m -> c.react(ACCEPT_EMOJI)).exceptionally(t -> {
			c.reply(e);
			return null;
		});
	}

	@SuppressWarnings("null")
	@SuppressFBWarnings(value = "DMI_RANDOM_USED_ONLY_ONCE", justification = "Deterministic output is needed")
	private static void about(@Nonnull CommandContext c, Argument command) {
		c.getCommands().get(command.value()).ifPresentOrElse(cmd -> {
			sendUsage(c, cmd);
		}, () -> {
			int random = new Random(command.value().hashCode()).nextInt(1000) + 300;
			throw c.errorf(FORMAT_NONEXISTANT, DISABLED, command.value().replace('`', '\''), random);
		});
	}

}
