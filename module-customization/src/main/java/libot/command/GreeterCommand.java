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

import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import libot.listener.GreeterListener;
import libot.provider.GreeterProvider;
import libot.provider.GreeterProvider.GreeterConfiguration;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;

public class GreeterCommand extends Command {

	@Nonnull private static final MandatoryParameter SUBCOMMAND =
		mandatory(POSITIONAL, "subcommand", "set/remove/test");
	@Nonnull private static final MandatoryParameter TYPE = mandatory(POSITIONAL, "type", "welcome/goodbye");

	public GreeterCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "greeter")
			.aliases("welcome", "goodbye", "farewell", "wgmo", "welcomegoodbyemessageoptions", "greet")
			.permissions(MANAGE_SERVER)
			.parameters(SUBCOMMAND, TYPE)
			.description("""
				Allows you to create, test, or remove a goodbye/welcome message for members joining/leaving your guild.
				Subcommands:
				- `set   ` sets the message that will be sent for the welcome/goodbye event
				- `remove` removes the message
				- `test  ` tests the event message by sending it as if you were joining/leaving"""));
	}

	private static final String FORMAT_NOT_SET = "A %s message has not yet been configured for this guild.";

	@Override
	public void execute(CommandContext c) {
		var type = switch (c.arg(TYPE).value().toLowerCase()) {
			case "welcome" -> EventType.WELCOME;
			case "goodbye" -> EventType.GOODBYE;
			default -> throw c.errorf("%s is not a valid event type. Please use `WELCOME` or `GOODBYE`.", FAILURE,
									  c.arg(TYPE).value());
		};

		var greeter = c.getProvider(GreeterProvider.class).get(c.getGuildIdLong());
		switch (c.arg(SUBCOMMAND).value().toLowerCase()) {
			case "set" -> set(c, type, greeter);
			case "remove" -> remove(c, type, greeter);
			case "test" -> test(c, type, greeter);
			default -> throw c.errorf("%s is not a valid subcommand. Please use `set`, `remove`, or `test`.", FAILURE,
									  c.arg(SUBCOMMAND).value());
		}
	}

	public static void set(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setDescriptionf("""
			You're setting the **%s** message in %s.
			Please type in your desired message.""", type.toString().toLowerCase(), c.getChannelMention());
		e.addFieldf("Available variables", """
			`{name}   ` Username - %s
			`{discrim}` User's discriminator - %s
			`{ping}   ` User mention - %s
			`{guild}  ` Guild name - %s""", c.getUsername(), c.getUserDiscriminator(), c.getUserMention(),
					c.getGuildName());
		e.setFooter(EXIT_FOOTER);

		var input = c.askraw(e).getContentRaw();
		if (input.toLowerCase().endsWith("exit"))
			return;
		if (input.length() > MAX_GREETER_MESSAGE_LENGTH)
			throw c.errorf("Message length may not exceed %d characters.", FAILURE, MAX_GREETER_MESSAGE_LENGTH);

		conf.setChannel(c.getChannelIdLong(), c.getChannelType());

		switch (type) {
			case WELCOME -> conf.setWelcomeMessage(input);
			case GOODBYE -> conf.setGoodbyeMessage(input);
		}

		c.replyf("Successfully set the **%s** message in %s. You may now test it with `%s test %s`.", SUCCESS,
				 type.name().toLowerCase(), c.getChannelMention(), c.getCommandWithPrefix(),
				 type.toString().toLowerCase());
	}

	public static void remove(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		if (!c.confirmf("Are you sure you want to remove the %s message?", type.toString().toLowerCase()))
			return;

		String message = switch (type) {
			case WELCOME -> conf.getWelcomeMessage();
			case GOODBYE -> conf.getGoodbyeMessage();
		};

		if (message == null)
			throw c.errorf(FORMAT_NOT_SET, DISABLED, type.toString().toLowerCase());

		switch (type) {
			case WELCOME -> conf.setWelcomeMessage(null);
			case GOODBYE -> conf.setGoodbyeMessage(null);
		}

		c.reply("Message removed successfully.", SUCCESS);
	}

	public static void test(@Nonnull CommandContext c, @Nonnull EventType type, @Nonnull GreeterConfiguration conf) {
		var message = switch (type) {
			case WELCOME -> conf.getWelcomeMessage();
			case GOODBYE -> conf.getGoodbyeMessage();
		};

		if (message == null)
			throw c.errorf(FORMAT_NOT_SET, DISABLED, type.toString().toLowerCase());

		var ch = (MessageChannelUnion) c.getGuild()
			.getChannelCache()
			.getElementById(conf.getChannelType(), conf.getChannelId());

		if (ch == null) {
			if (!c.confirmf("// WARNING //", """
				The channel LiBot would send the welcome/goodbye message to does no longer exist.
				Do you want to set %s as the welcome/goodbye channel?""", WARN, c.getChannel().getAsMention())) {

				throw c.cancel();
			}

			ch = c.getChannel();
			conf.setChannel(ch.getIdLong(), ch.getType());
		}

		ch.sendMessage(GreeterListener.parseMessage(message, c.getUser(), c.getGuild())).queue();
		c.replyf("A test %s message has been sent to %s.", SUCCESS, type.toString().toLowerCase(), ch.getAsMention());
	}

	private enum EventType {
		WELCOME,
		GOODBYE
	}

}
