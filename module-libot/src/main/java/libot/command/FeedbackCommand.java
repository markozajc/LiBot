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

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.command.CommandCategory.LIBOT;
import static org.apache.commons.text.WordUtils.wrap;

import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;

public class FeedbackCommand extends Command {

	public FeedbackCommand() {
		super(CommandMetadata.builder(LIBOT, "feedback")
			.ratelimit(60, TimeUnit.SECONDS)
			.description("Sends feedback to LiBot's developers."));
	}

	private static final int MAIL_WRAP = 70;
	private static final int MESSAGE_CAP = 1500;
	private static final int SUBJECT_CAP = 30;

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM).setTitle("LiBot feedback system")
			.setAuthor("From " + c.getUserTag(), null, c.getAvatarUrl())
			.setFooter("Type in a subject or EXIT to abort")
			.setDescription(generateMail("", ""));
		c.reply(b);
		var subject = ask(c, SUBJECT_CAP, "The subject field can only contain up to %d characters");

		b.setFooter("Type in a message or EXIT to abort").setDescription(generateMail(subject, ""));
		c.reply(b);
		var message = ask(c, MESSAGE_CAP, "The message can only contain up to %d characters");

		b.setFooter("Are you sure you want to send this mail?").setDescription(generateMail(subject, message));
		if (!c.confirm(b))
			throw c.cancel();

		b.setFooter(format("Sender ID: %s", c.getUserId()));
		c.reply("Thank you for your feedback!", SUCCESS);

		var mail = b.build();
		c.messageSysadmins(mail);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String generateMail(@Nonnull String subject, @Nonnull String message) {
		return wrap("""
			To: LiBot developers
			Subject: %s
			────────────────────────────
			%s
			""".formatted(subject, message), MAIL_WRAP);
	}

	@Nonnull
	private static String ask(@Nonnull CommandContext c, int lengthCap, @Nonnull String tooLongFormat) {
		while (true) {
			var resp = c.ask();
			if (resp.equalsIgnoreCase("exit")) {
				throw c.cancel();

			} else if (resp.length() > lengthCap) {
				c.replyf(tooLongFormat, WARN, lengthCap);

			} else {
				return resp;
			}
		}
	}

}
