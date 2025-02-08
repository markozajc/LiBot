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
package libot.core.entity;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.util.Utilities.asUnchecked;
import static net.dv8tion.jda.api.Permission.MESSAGE_ADD_REACTION;

import java.util.Optional;
import java.util.concurrent.*;

import javax.annotation.*;

import libot.core.argument.ArgumentList;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.*;
import libot.core.command.Command;
import libot.util.EventWaiter;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.MessageEmbed.Footer;

public class CommandContext extends EventContext {

	private static final String FORMAT_FALLBACK_ASK = format("React with %s or %s", ACCEPT_EMOJI, DECLINE_EMOJI);

	@Nonnull private final Command command;
	@Nonnull private final ArgumentList arguments;

	@Nullable private Message reference;
	@Nullable private EventWaiter waiter;
	private final Object waiterMutex = new Object();

	public CommandContext(@Nonnull EventContext eventContext, @Nonnull Command command,
						  @Nonnull ArgumentList arguments) {
		super(eventContext);
		this.command = command;
		this.arguments = arguments;
	}

	// ===============* Getters *===============

	@Nonnull
	public Command getCommand() {
		return this.command;
	}

	@Nonnull
	public ArgumentList getArgumentList() {
		return this.arguments;
	}

	// ===============* Shortcut getters *===============

	@Nonnull
	public String getCommandName() {
		return this.command.getName();
	}

	@Nonnull
	public String getCommandUsage() {
		return this.command.getUsage(this);
	}

	public long getCommandRatelimit() {
		return this.command.getRatelimit();
	}

	@Nonnull
	public Argument arg(@Nonnull MandatoryParameter param) {
		return this.arguments.get(param);
	}

	@Nonnull
	public Optional<Argument> arg(@Nonnull Parameter param) {
		return this.arguments.get(param);
	}

	// ===============* Utilities *===============

	@Nonnull
	public String getCommandWithPrefix() {
		return getEffectivePrefix() + this.command.getName();
	}

	public boolean isCommandRatelimited() {
		return this.command.getRatelimit() != 0;
	}

	// ===============* EventWaiter *===============

	@Nonnull
	public EventWaiter getWaiter() {
		synchronized (this.waiterMutex) {
			if (this.waiter != null)
				return this.waiter;
			else
				return this.waiter = new EventWaiter(getEventWaiterListener(), getUser(), getChannel());
		}
	}

	public void setWaiterTimeout(@Nonnull TimeUnit unit, long timeout) {
		synchronized (this.waiterMutex) {
			if (this.waiter != null)
				this.waiter.setTimeout(unit, timeout);
			else
				this.waiter = new EventWaiter(getEventWaiterListener(), getUser(), getChannel(), unit, timeout);
		}
	}

	// ===============* confirm *===============

	public boolean confirm(@Nonnull String message) {
		return confirm(false, message);
	}

	@SuppressWarnings("null")
	public boolean confirm(boolean keepPrompt, @Nonnull String message) {
		String useMessage = message;
		if (!hasChannelPermission(MESSAGE_ADD_REACTION))
			useMessage += "\n" + FORMAT_FALLBACK_ASK;
		// fallback in case we don't have MESSAGE_ADD_REACTION
		// because the stupid user denied/forgot to grant it

		try {
			return getConfirmation(reply(useMessage).get(), keepPrompt);
		} catch (ExecutionException | InterruptedException e) { // NOSONAR
			throw asUnchecked(e);
		}
	}

	public boolean confirm(@Nonnull MessageEmbed embed) {
		return confirm(false, embed);
	}

	@SuppressWarnings("null")
	public boolean confirm(boolean keepPrompt, @Nonnull MessageEmbed embed) {
		MessageEmbed useEmbed;
		if (!canReact()) { // fallback ditto
			var eb = new EmbedBuilder(embed);
			Footer footer;
			String footerText;
			if ((footer = embed.getFooter()) == null || (footerText = footer.getText()) == null)
				eb.setFooter(FORMAT_FALLBACK_ASK);
			else
				eb.setFooter(footerText + " | " + FORMAT_FALLBACK_ASK);
			useEmbed = eb.build();
		} else {
			useEmbed = embed;
		}

		try {
			return getConfirmation(reply(useEmbed).get(), keepPrompt);
		} catch (ExecutionException | InterruptedException e) { // NOSONAR
			throw asUnchecked(e);
		}
	}

	public boolean confirm(@Nonnull EmbedBuilder builder) {
		return confirm(false, builder.build());
	}

	public boolean confirm(boolean keepPrompt, @Nonnull EmbedBuilder builder) {
		return confirm(keepPrompt, builder.build());
	}

	public boolean confirm(@Nullable String title, @Nonnull String message, @Nullable String footer,
						   @Nullable Color color) {
		return confirm(false, title, message, footer, color);
	}

	public boolean confirm(boolean keepPrompt, @Nullable String title, @Nonnull String message, @Nullable String footer,
						   @Nullable Color color) {
		return confirm(keepPrompt, createEmbedBuilder(title, message, footer, color).build());
	}

	public boolean confirm(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return confirm(false, title, message, null, color);
	}

	public boolean confirm(boolean keepPrompt, @Nullable String title, @Nonnull String message, @Nullable Color color) {
		return confirm(keepPrompt, title, message, null, color);
	}

	public boolean confirm(@Nonnull String message, @Nullable Color color) {
		return confirm(false, null, message, null, color);
	}

	public boolean confirm(boolean keepPrompt, @Nonnull String message, @Nullable Color color) {
		return confirm(keepPrompt, null, message, null, color);
	}

	// ===============* confirmf *===============

	@SuppressWarnings("null")
	public boolean confirmf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return confirm(format(messageFormat, args));
	}

	@SuppressWarnings("null")
	public boolean confirmf(boolean keepPrompt, @Nonnull String messageFormat, @Nonnull Object... args) {
		return confirm(keepPrompt, format(messageFormat, args));
	}

	@SuppressWarnings("null")
	public boolean confirmf(@Nullable String title, @Nonnull String messageFormat, @Nullable String footer,
							@Nullable Color color, @Nonnull Object... args) {
		return confirm(title, format(messageFormat, args), footer, color);

	}

	@SuppressWarnings("null")
	public boolean confirmf(boolean keepPrompt, @Nullable String title, @Nonnull String messageFormat,
							@Nullable String footer, @Nullable Color color, @Nonnull Object... args) {
		return confirm(keepPrompt, title, format(messageFormat, args), footer, color);
	}

	public boolean confirmf(@Nullable String title, @Nonnull String messageFormat, @Nullable Color color,
							@Nonnull Object... args) {
		return confirmf(false, title, messageFormat, null, color, args);
	}

	public boolean confirmf(boolean keepPrompt, @Nullable String title, @Nonnull String messageFormat,
							@Nullable Color color, @Nonnull Object... args) {
		return confirmf(keepPrompt, title, messageFormat, null, color, args);
	}

	public boolean confirmf(@Nonnull String messageFormat, @Nullable Color color, @Nonnull Object... args) {
		return confirmf(false, null, messageFormat, null, color, args);
	}

	public boolean confirmf(boolean keepPrompt, @Nonnull String messageFormat, @Nullable Color color,
							@Nonnull Object... args) {
		return confirmf(keepPrompt, null, messageFormat, null, color, args);
	}

	// ===============* askraw *===============

	@Nonnull
	public Message askraw() {
		try {
			var message = getWaiter().awaitMessage(true);
			this.reference = message;
			return message; // ignoreBlank must be configurable if we ever need to receive files
							// (or embeds I guess?)
		} catch (InterruptedException e) { // NOSONAR
			throw asUnchecked(e);
		}
	}

	@Nonnull
	public Message askraw(@Nonnull String message) {
		reply(message);
		return askraw();
	}

	@Nonnull
	public Message askraw(@Nonnull MessageEmbed embed) {
		reply(embed);
		return askraw();
	}

	@Nonnull
	public Message askraw(@Nonnull EmbedBuilder builder) {
		return askraw(builder.build());
	}

	@Nonnull
	public Message askraw(@Nullable String title, @Nonnull String message, @Nullable String footer,
						  @Nullable Color color) {
		return askraw(createEmbedBuilder(title, message, footer, color).build());
	}

	@Nonnull
	public Message askraw(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return askraw(title, message, null, color);
	}

	@Nonnull
	public Message askraw(@Nonnull String message, @Nullable Color color) {
		return askraw(null, message, null, color);
	}

	// ===============* askrawf *===============

	@SuppressWarnings("null")
	@Nonnull
	public Message askrawf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return askraw(format(messageFormat, args));
	}

	@SuppressWarnings("null")
	@Nonnull
	public Message askrawf(@Nullable String title, @Nonnull String messageFormat, @Nullable String footer,
						   @Nullable Color color, @Nonnull Object... args) {
		return askraw(title, format(messageFormat, args), footer, color);
	}

	@Nonnull
	public Message askrawf(@Nullable String title, @Nonnull String messageFormat, @Nullable Color color,
						   @Nonnull Object... args) {
		return askrawf(title, messageFormat, null, color, args);
	}

	@Nonnull
	public Message askrawf(@Nonnull String messageFormat, @Nullable Color color, @Nonnull Object... args) {
		return askrawf(null, messageFormat, null, color, args);
	}

	// ===============* ask *===============

	@Nonnull
	public String ask() {
		return askraw().getContentDisplay();
	}

	@Nonnull
	public String ask(@Nonnull String message) {
		reply(message);
		return ask();
	}

	@Nonnull
	public String ask(@Nonnull MessageEmbed embed) {
		reply(embed);
		return ask();
	}

	@Nonnull
	public String ask(@Nonnull EmbedBuilder builder) {
		return ask(builder.build());
	}

	@Nonnull
	public String ask(@Nullable String title, @Nonnull String message, @Nullable String footer, @Nullable Color color) {
		return ask(createEmbedBuilder(title, message, footer, color).build());
	}

	@Nonnull
	public String ask(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return ask(title, message, null, color);
	}

	@Nonnull
	public String ask(@Nonnull String message, @Nullable Color color) {
		return ask(null, message, null, color);
	}

	// ===============* askf *===============

	@SuppressWarnings("null")
	@Nonnull
	public String askf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return ask(format(messageFormat, args));
	}

	@SuppressWarnings("null")
	@Nonnull
	public String askf(@Nullable String title, @Nonnull String messageFormat, @Nullable String footer,
					   @Nullable Color color, @Nonnull Object... args) {
		return ask(title, format(messageFormat, args), footer, color);
	}

	@Nonnull
	public String askf(@Nullable String title, @Nonnull String messageFormat, @Nullable Color color,
					   @Nonnull Object... args) {
		return askf(title, messageFormat, null, color, args);
	}

	@Nonnull
	public String askf(@Nonnull String messageFormat, @Nullable Color color, @Nonnull Object... args) {
		return askf(null, messageFormat, null, color, args);
	}

	// ===============* Internal *===============

	protected CommandContext(@Nonnull CommandContext commandContext) {
		super(commandContext);
		this.command = commandContext.command;
		this.arguments = commandContext.arguments;
		this.reference = commandContext.reference;
		this.waiter = commandContext.waiter;
	}

	private boolean getConfirmation(@Nonnull Message m, boolean keepPrompt) {
		if (canReact()) {
			m.addReaction(ACCEPT_EMOJI).queue();
			m.addReaction(DECLINE_EMOJI).queue();
		}
		try {
			return getWaiter().awaitBoolean(m, keepPrompt);
		} catch (InterruptedException e) { // NOSONAR
			throw asUnchecked(e);
		}
	}

	@Override
	protected Message getReference() {
		return this.reference == null ? getMessage() : this.reference;
	}

}
