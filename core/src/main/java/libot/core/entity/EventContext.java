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

import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static libot.util.Utilities.*;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.entities.Role.DEFAULT_COLOR_RAW;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

import javax.annotation.*;

import libot.core.command.Command;
import libot.core.command.exception.CommandException;
import libot.core.command.exception.runtime.CanceledException;
import libot.core.command.exception.startup.NotSysadminException;
import libot.provider.CustomizationsProvider;
import libot.provider.CustomizationsProvider.Customization;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildChannel;
import net.dv8tion.jda.api.entities.channel.unions.*;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.*;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.MessageCreateAction;
import net.dv8tion.jda.api.utils.FileUpload;
import net.dv8tion.jda.api.utils.messages.*;

public class EventContext extends BotContext {

	@Nonnull private static final CanceledException CANCELED = new CanceledException();

	@Nonnull private final MessageReceivedEvent event;

	public EventContext(@Nonnull BotContext bot, @Nonnull MessageReceivedEvent event) {
		super(bot);
		this.event = event;
	}

	// ===============* Getters *===============

	@Nonnull
	public Message getMessage() {
		return this.event.getMessage();
	}

	@Nonnull
	public MessageChannelUnion getChannel() {
		return this.event.getChannel();
	}

	@Nonnull
	public ChannelType getChannelType() {
		return this.event.getChannelType();
	}

	@Nonnull
	public Guild getGuild() {
		return this.event.getGuild();
	}

	@Nonnull
	public Member getMember() {
		var member = this.event.getMember();
		if (member == null)
			throw new IllegalStateException("Accepted message from a bot");
		return member;
	}

	@Nonnull
	public User getUser() {
		return this.event.getAuthor();
	}

	@Nonnull
	public JDA getJda() {
		return this.event.getJDA();
	}

	@Nonnull
	public MessageReceivedEvent getEvent() {
		return this.event;
	}

	// ===============* Shortcut getters *===============

	@Nonnull
	public String getSelfAvatar() {
		return getSelfUser().getEffectiveAvatarUrl();
	}

	public boolean canMemberInteract(@Nonnull Member member) {
		return getMember().canInteract(member);
	}

	public boolean canMemberInteract(@Nonnull Role role) {
		return getMember().canInteract(role);
	}

	public boolean canSelfInteract(@Nonnull Member member) {
		return getSelfMember().canInteract(member);
	}

	public boolean canSelfInteract(@Nonnull Role role) {
		return getSelfMember().canInteract(role);
	}

	@Nonnull
	public GuildVoiceState getMemberVoiceState() {
		var vs = getMember().getVoiceState();
		if (vs == null)
			throw new IllegalStateException();
		return vs;
	}

	@Nonnull
	public AudioManager getAudioManager() {
		return getGuild().getAudioManager();
	}

	@Nullable
	public AudioChannelUnion getConnectedAChannel() {
		return getAudioManager().getConnectedChannel();
	}

	@Nonnull
	public Customization getGuildCustomization() {
		return getProvider(CustomizationsProvider.class).get(getGuildIdLong());
	}

	public long getGuildIdLong() {
		return getGuild().getIdLong();
	}

	public long getChannelIdLong() {
		return getChannel().getIdLong();
	}

	public long getMessageIdLong() {
		return getMessage().getIdLong();
	}

	public String getUserId() {
		return getUser().getId();
	}

	public long getUserIdLong() {
		return getUser().getIdLong();
	}

	@Nonnull
	public String getGuildName() {
		return getGuild().getName();
	}

	@Nonnull
	public Member getSelfMember() {
		return getGuild().getSelfMember();
	}

	@Nonnull
	public String getSelfMention() {
		return getSelfUser().getAsMention();
	}

	@Nonnull
	public String getSelfEName() {
		return getSelfMember().getEffectiveName();
	}

	@Nonnull
	public String getSelfId() {
		return getSelfUser().getId();
	}

	public long getSelfIdLong() {
		return getSelfUser().getIdLong();
	}

	@Nonnull
	public User getSelfUser() {
		return getJda().getSelfUser();
	}

	@Nonnull
	public String getUsername() {
		return getUser().getName();
	}

	@Nonnull
	public String getUserDiscriminator() {
		return getUser().getDiscriminator();
	}

	@Nonnull
	public String getEffectiveName() {
		return getMember().getEffectiveName();
	}

	@Nonnull
	public String getUserMention() {
		return getUser().getAsMention();
	}

	@Nonnull
	public String getUserTag() {
		return getUser().getAsTag();
	}

	@Nonnull
	public String getAvatarUrl() {
		return getUser().getEffectiveAvatarUrl();
	}

	@Nonnull
	public String getChannelname() {
		return getChannel().getName();
	}

	@Nonnull
	public String getChannelMention() {
		return getChannel().getAsMention();
	}

	public boolean isChannelNSFW() {
		if (getChannel() instanceof TextChannel textChannel)
			return textChannel.isNSFW();
		else
			return false;
	}

	@Nonnull
	public Role getPublicRole() {
		return getGuild().getPublicRole();
	}

	// ===============* Utilities *===============

	@Nonnull
	public <T extends Command> String getCommandWithPrefix(@Nonnull Class<T> clazz) {
		return getEffectivePrefix() + getCommandName(clazz);
	}

	public boolean canReact() {
		return hasChannelPermission(MESSAGE_ADD_REACTION);
	}

	public boolean isUserSysadmin() {
		return contains(getConfig().sysadminIds(), getUserIdLong());
	}

	public boolean isUserDj() {
		return getProvider(CustomizationsProvider.class).get(getGuildIdLong()).isDj(getMember());
	}

	@Nonnull
	@SuppressWarnings("null")
	public String getEffectivePrefix() {
		return getProvider(CustomizationsProvider.class).get(getGuildIdLong())
			.getCustomPrefix()
			.orElse(getConfig().defaultPrefix());
	}

	// ===============* Shortcuts *===============

	public void requireSysadmin() {
		if (!isUserSysadmin())
			throw new NotSysadminException();
	}

	@Nonnull
	public CompletableFuture<Void> typing() {
		return getChannel().sendTyping().submit();
	}

	@CheckReturnValue
	@Nonnull
	public CanceledException exit() {
		return new CanceledException();
	}

	public boolean hasChannelPermission(@Nonnull Permission... permissions) {
		if (getChannel() instanceof GuildChannel guildChannel)
			return getSelfMember().hasPermission(guildChannel, permissions);
		else
			return true;
	}

	public boolean hasGuildPermission(@Nonnull Permission... permissions) {
		return getSelfMember().hasPermission(permissions);
	}

	public boolean hasAuthorChannelPermission(@Nonnull Permission... permissions) {
		if (getChannel() instanceof GuildChannel guildChannel)
			return getMember().hasPermission(guildChannel, permissions);
		else
			return true;
	}

	public boolean hasAuthorGuildPermission(@Nonnull Permission... permissions) {
		return getMember().hasPermission(permissions);
	}

	public boolean canTalk() {
		return getChannel().canTalk();
	}

	// ===============* Checks *===============

	@SuppressWarnings("null")
	public void ensureChannelPermission(@Nonnull Permission... permissions) {
		for (var perm : permissions) {
			if (!hasChannelPermission(perm)) {
				throw new InsufficientPermissionException((GuildChannel) getChannel(), perm);
			}
		}
	}

	@SuppressWarnings("null")
	public void ensureGuildPermission(@Nonnull Permission... permissions) {
		for (var perm : permissions) {
			if (!hasGuildPermission(perm)) {
				throw new InsufficientPermissionException(getGuild(), perm);
			}
		}
	}

	public void ensureSelfInteract(@Nonnull Member member) {
		if (!canSelfInteract(member))
			throw new HierarchyException("Can't interact with a member with a higher role.");
	}

	public void ensureSelfInteract(@Nonnull Role role) {
		if (!canSelfInteract(role))
			throw new HierarchyException("Can't interact with a higher role.");
	}

	// ===============* react *===============

	@Nonnull
	public CompletableFuture<Void> react(@Nonnull Emoji emoji) {
		if (canReact())
			return getMessage().addReaction(emoji).submit();
		else
			return permissionExceptionFuture(MESSAGE_ADD_REACTION);
	}

	// ===============* reply *===============

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull MessageCreateData message) {
		if (canTalk())
			return replyraw(message).submit();
		else {
			return permissionExceptionFuture(MESSAGE_SEND);
		}
	}

	@Nonnull
	@SuppressWarnings("resource")
	public CompletableFuture<Message> reply(@Nonnull MessageCreateBuilder builder) {
		return reply(builder.build());
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull String message) {
		if (canTalk())
			return replyMessage(getChannel().sendMessage(message)).submit();
		else {
			return permissionExceptionFuture(MESSAGE_SEND);
		}
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull MessageEmbed embed) {
		if (canTalk())
			return replyMessage(getChannel().sendMessageEmbeds(embed)).submit();
		else {
			return permissionExceptionFuture(MESSAGE_SEND);
		}
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull EmbedBuilder builder) {
		if (canTalk())
			return replyMessage(getChannel().sendMessageEmbeds(builder.build())).submit();
		else
			return permissionExceptionFuture(MESSAGE_SEND);
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nullable String title, @Nonnull String message, @Nullable String footer,
											@Nullable Color color) {
		EmbedBuilder builder = new EmbedBuilder().setTitle(title)
			.appendDescription(message)
			.setFooter(footer, null)
			.setColor(color == null ? DEFAULT_COLOR_RAW : color.rgb());

		return reply(builder.build());
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return reply(title, message, null, color);
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull String message, @Nullable Color color) {
		return reply(null, message, null, color);
	}

	// ===============* replyraw *===============

	@Nonnull
	public MessageCreateAction replyraw(@Nonnull MessageCreateData message) {
		return replyMessage(getChannel().sendMessage(message));
	}

	@Nonnull
	@SuppressWarnings("resource")
	public MessageCreateAction replyraw(@Nonnull MessageCreateBuilder builder) {
		return replyraw(builder.build());
	}

	// ===============* replyf *===============

	@Nonnull
	@SuppressWarnings("null")
	public CompletableFuture<Message> replyf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return reply(format(messageFormat, args));
	}

	@Nonnull
	@SuppressWarnings("null")
	public CompletableFuture<Message> replyf(@Nullable String title, @Nonnull String messageFormat,
											 @Nullable String footer, @Nullable Color color, @Nonnull Object... args) {
		return reply(title, format(messageFormat, args), footer, color);
	}

	@Nonnull
	public CompletableFuture<Message> replyf(@Nullable String title, @Nonnull String messageFormat,
											 @Nullable Color color, @Nonnull Object... args) {
		return replyf(title, messageFormat, null, color, args);
	}

	@Nonnull
	public CompletableFuture<Message> replyf(@Nonnull String messageFormat, @Nullable Color color,
											 @Nonnull Object... args) {
		return replyf(null, messageFormat, null, color, args);
	}

	// ===============* direct *===============

	@Nonnull
	public CompletableFuture<Message> direct(@Nonnull String message) {
		return getUser().openPrivateChannel().flatMap(pc -> pc.sendMessage(message)).submit();
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nonnull MessageEmbed embed) {
		return getUser().openPrivateChannel().flatMap(pc -> pc.sendMessageEmbeds(embed)).submit();
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nonnull EmbedBuilder builder) {
		return direct(builder.build());
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nullable String title, @Nonnull String message, @Nullable String footer,
											 @Nullable Color color) {
		return direct(createEmbedBuilder(title, message, footer, color).build());
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return direct(title, message, null, color);
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nonnull String message, @Nullable Color color) {
		return direct(null, message, null, color);
	}

	// ===============* replyFiles *===============

	@Nonnull
	public CompletableFuture<Message> replyFiles(@Nonnull FileUpload file, @Nonnull FileUpload... more) {
		return replyFiles(concat(file, more));
	}

	@Nonnull
	public CompletableFuture<Message> replyFiles(@Nonnull Collection<? extends FileUpload> files) {
		return replyMessage(getChannel().sendFiles(files)).submit();
	}

	// ===============* directf *===============

	@SuppressWarnings("null")
	@Nonnull
	public CompletableFuture<Message> directf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return direct(String.format(messageFormat, args));
	}

	@SuppressWarnings("null")
	@Nonnull
	public CompletableFuture<Message> directf(@Nullable String title, @Nonnull String messageFormat,
											  @Nullable String footer, @Nullable Color color, @Nonnull Object... args) {
		return direct(title, format(messageFormat, args), footer, color);
	}

	@Nonnull
	public CompletableFuture<Message> directf(@Nullable String title, @Nonnull String messageFormat,
											  @Nullable Color color, @Nonnull Object... args) {
		return directf(title, messageFormat, null, color, args);
	}

	@Nonnull
	public CompletableFuture<Message> directf(@Nonnull String messageFormat, @Nullable Color color,
											  @Nonnull Object... args) {
		return directf(null, messageFormat, null, color, args);
	}

	// ===============* cancel *===============

	@Nonnull
	@CheckReturnValue
	public CanceledException cancel() {
		return CANCELED;
	}

	// ===============* error *===============

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit) {
		return new CommandException(null, ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error() {
		return error(false);
	}

	@Nonnull
	@CheckReturnValue
	@SuppressWarnings("resource")
	public CommandException error(boolean ratelimit, @Nonnull String message) {
		return new CommandException(new MessageCreateBuilder().setContent(message).build(), ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull String message) {
		return error(false, message);
	}

	@Nonnull
	@CheckReturnValue
	@SuppressWarnings("resource")
	public CommandException error(boolean ratelimit, @Nonnull Collection<MessageEmbed> embeds) {
		return new CommandException(new MessageCreateBuilder().addEmbeds(embeds).build(), ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit, @Nonnull MessageEmbed embed, @Nonnull MessageEmbed... more) {
		return error(ratelimit, concat(embed, more));
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull Collection<MessageEmbed> embeds) {
		return error(false, embeds);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... more) {
		return error(false, embed, more);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit, @Nonnull EmbedBuilder builder) {
		return error(ratelimit, builder.build());
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull EmbedBuilder builder) {
		return error(false, builder.build());
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit, @Nullable String title, @Nonnull String message,
								  @Nullable String footer, @Nullable Color color) {
		return error(ratelimit, createEmbedBuilder(title, message, footer, color).build());
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nullable String title, @Nonnull String message, @Nullable String footer,
								  @Nullable Color color) {
		return error(false, title, message, footer, color);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nullable String title, @Nonnull String message, @Nullable Color color,
								  boolean ratelimit) {
		return error(ratelimit, title, message, null, color);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return error(false, title, message, null, color);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit, @Nonnull String message, @Nullable Color color) {
		return error(ratelimit, null, message, null, color);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull String message, @Nullable Color color) {
		return error(false, null, message, null, color);
	}

	// ===============* errorf *===============

	@SuppressWarnings("null")
	@Nonnull
	@CheckReturnValue
	public CommandException errorf(boolean ratelimit, @Nonnull String messageFormat, @Nonnull Object... args) {
		return error(ratelimit, format(messageFormat, args));
	}

	@SuppressWarnings("null")
	@Nonnull
	@CheckReturnValue
	public CommandException errorf(@Nonnull String messageFormat, @Nonnull Object... args) {
		return error(false, format(messageFormat, args));
	}

	@SuppressWarnings("null")
	@Nonnull
	public CommandException errorf(boolean ratelimit, @Nullable String title, @Nonnull String messageFormat,
								   @Nullable String footer, @Nullable Color color, @Nonnull Object... args) {
		return error(ratelimit, title, format(messageFormat, args), footer, color);
	}

	@SuppressWarnings("null")
	@Nonnull
	@CheckReturnValue
	public CommandException errorf(@Nullable String title, @Nonnull String messageFormat, @Nullable String footer,
								   @Nullable Color color, @Nonnull Object... args) {
		return error(false, title, format(messageFormat, args), footer, color);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException errorf(boolean ratelimit, @Nullable String title, @Nonnull String messageFormat,
								   @Nullable Color color, @Nonnull Object... args) {
		return errorf(ratelimit, title, messageFormat, null, color, args);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException errorf(@Nullable String title, @Nonnull String messageFormat, @Nullable Color color,
								   @Nonnull Object... args) {
		return errorf(false, title, messageFormat, null, color, args);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException errorf(boolean ratelimit, @Nonnull String messageFormat, @Nullable Color color,
								   @Nonnull Object... args) {
		return errorf(ratelimit, null, messageFormat, null, color, args);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException errorf(@Nonnull String messageFormat, @Nullable Color color, @Nonnull Object... args) {
		return errorf(false, null, messageFormat, null, color, args);
	}

	// ===============* Internal *===============

	protected EventContext(@Nonnull EventContext eventContext) {
		super(eventContext);
		this.event = eventContext.event;
	}

	@Nonnull
	protected EmbedBuilder createEmbedBuilder(@Nullable String title, @Nonnull String message, @Nullable String footer,
											  @Nullable Color color) {
		return new EmbedBuilder().setTitle(title)
			.appendDescription(message)
			.setFooter(footer, null)
			.setColor(color == null ? DEFAULT_COLOR_RAW : color.rgb());
	}

	@Nonnull
	private <T> CompletableFuture<T> permissionExceptionFuture(@Nonnull Permission permission) {
		return exceptionFuture(new InsufficientPermissionException(getGuild(), permission));
	}

	@Nonnull
	private MessageCreateAction replyMessage(MessageCreateAction message) {
		return message.setAllowedMentions(emptyList()).setMessageReference(getReference()).mentionRepliedUser(false);
	}

	@Nonnull
	protected Message getReference() {
		return getMessage();
	}

}
