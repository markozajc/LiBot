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
package libot.command;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.reverse;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.FAILURE;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.UTILITIES;
import static net.dv8tion.jda.api.Permission.MESSAGE_HISTORY;
import static net.dv8tion.jda.api.utils.FileUpload.fromData;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.Attachment;
import net.dv8tion.jda.api.entities.MessageEmbed.*;

public class DumpMessagesCommand extends Command {

	@Nonnull private static final MandatoryParameter COUNT =
		mandatory(POSITIONAL, "count", "number of messages to dump");

	public DumpMessagesCommand() {
		super(CommandMetadata.builder(UTILITIES, "dumpmessages")
			.aliases("dump")
			.permissions(MESSAGE_HISTORY)
			.ratelimit(1, MINUTES)
			.parameters(COUNT)
			.description("Dumps messages from the channel into a file and uploads it."));
	}

	private static final ZoneId UTC = ZoneId.of("UTC");
	private static final DateTimeFormatter DTF = ofPattern("dd/MM/yyyy HH:mm");

	private static final String L1 = "\n\t";
	private static final String L2 = "\n\t\t";
	private static final String L3 = "\n\t\t\t";
	private static final int MESSAGES_CAP = 100_000;

	@SuppressWarnings({ "null", "resource" })
	@Override
	public void execute(CommandContext c) {
		int limit = c.arg(COUNT).valueAsInt();

		if (limit > MESSAGES_CAP)
			throw c.errorf("You can only dump up to %d messages", FAILURE, MESSAGES_CAP);
		c.typing();

		StringBuilder b = new StringBuilder();
		var messages = new ArrayList<>(c.getChannel()
			.getIterableHistory()
			.stream()
			.limit(limit + 1L)
			.filter(m -> m.getIdLong() != c.getMessage().getIdLong())
			.map(msg -> parseMessage(b, msg))
			.toList());

		reverse(messages);

		c.getChannel()
			.sendMessage(format("Dumped %d messages:", messages.size()))
			.addFiles(fromData(messages.stream().collect(joining("\n")).getBytes(UTF_8), "messagedump.txt"))
			.queue();
	}

	@SuppressWarnings("null")
	private static String parseMessage(@Nonnull StringBuilder b, @Nonnull Message m) {
		b.setLength(0);
		dumpDateTime(b, m.getTimeCreated());
		dumpAuthor(b, m);
		dumpContent(b, m);
		m.getEmbeds().forEach(e -> dumpEmbed(b, e));
		m.getAttachments().forEach(a -> dumpAttachment(b, a));
		return b.toString();
	}

	private static void dumpDateTime(@Nonnull StringBuilder b, @Nonnull OffsetDateTime dateTime) {
		b.append(DTF.format(dateTime.atZoneSameInstant(UTC)));
		b.append(" ");
	}

	private static void dumpAuthor(@Nonnull StringBuilder b, @Nonnull Message message) {
		if (message.getAuthor().isBot())
			b.append("[BOT] ");
		var member = message.getMember();
		if (member != null)
			b.append(member.getEffectiveName());
		else
			b.append(message.getAuthor().getAsTag());
		b.append(": ");
	}

	private static void dumpContent(@Nonnull StringBuilder b, @Nonnull Message m) {
		var content = m.getContentDisplay().strip();
		if (content.length() > 0)
			b.append(m.getContentDisplay());
		if (m.isEdited())
			b.append(" (edited)");
	}

	@SuppressWarnings("null")
	private static void dumpEmbed(@Nonnull StringBuilder b, @Nonnull MessageEmbed embed) {
		var title = embed.getTitle();
		if (title != null) {
			b.append(L1);
			b.append(title.replace("\n", L1));
		}

		var description = embed.getDescription();
		if (description != null) {
			b.append(L2);
			b.append(description.replace("\n", L2));
		}

		embed.getFields().forEach(f -> dumpField(b, f));
		b.append("\n");

		var footer = embed.getFooter();
		if (footer != null)
			dumpFooter(b, footer);
	}

	private static void dumpField(@Nonnull StringBuilder b, @Nonnull Field field) {
		var name = field.getName();
		if (name != null) {
			b.append("\n\n\t\t");
			b.append(name.replace("\n", L2));
		}

		var value = field.getValue();
		if (value != null) {
			b.append(L3);
			b.append(value.replace("\n", L3));
		}
	}

	private static void dumpFooter(@Nonnull StringBuilder b, @Nonnull Footer footer) {
		var text = footer.getText();
		if (text != null) {
			b.append(L1);
			b.append(text.replace("\n", L1));
		}
	}

	private static void dumpAttachment(@Nonnull StringBuilder b, @Nonnull Attachment attachment) {
		b.append(L2);
		b.append("Attachment: ");
		b.append(attachment.getFileName());
		b.append(" (");
		b.append(attachment.getUrl());
		b.append(")");
	}

}
