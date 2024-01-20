package libot.core.entities;

import static java.lang.String.format;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyList;
import static libot.core.Constants.*;
import static libot.utils.ParseUtils.parseParameters;
import static libot.utils.Utilities.*;
import static net.dv8tion.jda.api.Permission.*;
import static org.apache.commons.lang3.ArrayUtils.contains;

import java.awt.Color;
import java.util.concurrent.*;

import javax.annotation.*;

import libot.core.BotConfiguration;
import libot.core.commands.*;
import libot.core.commands.exceptions.*;
import libot.core.commands.exceptions.runtime.CanceledException;
import libot.core.commands.exceptions.startup.*;
import libot.core.data.DataManager;
import libot.core.data.providers.Provider;
import libot.core.shred.Shredder;
import libot.providers.CustomizationsProvider;
import libot.providers.CustomizationsProvider.Customization;
import libot.utils.EventUtils;
import libot.utils.ParseUtils.Prefix;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.MessageEmbed.Footer;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.*;
import net.dv8tion.jda.api.managers.AudioManager;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import net.dv8tion.jda.api.utils.AttachmentOption;

public class CommandContext {

	private static final String FORMAT_FALLBACK_ASK = format("React with %s or %s", ACCEPT_EMOJI, DENY_EMOJI);

	@Nonnull private static final CanceledException CANCELED = new CanceledException();

	@Nonnull private final GuildMessageReceivedEvent event;
	@Nullable private Message reference;
	@Nonnull private final Parameters parameters;
	@Nonnull private final Command command;
	@Nonnull private final BotContext botContext;
	@Nullable private EventUtils waiter;

	public CommandContext(@Nonnull GuildMessageReceivedEvent event, @Nonnull Command command,
						  @Nonnull BotContext botContext, @Nonnull Prefix prefix) {
		this.event = event;
		this.command = command;
		this.botContext = botContext;
		var rawParameters = parseParameters(event.getMessage().getContentRaw(), prefix, command.getMaxParameters());
		this.parameters = new Parameters(rawParameters);
	}

	// ===============* Getters *===============

	@Nonnull
	public Message getMessage() {
		return this.event.getMessage();
	}

	@Nonnull
	public TextChannel getChannel() {
		return this.event.getChannel();
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
	public JDA jda() {
		return this.event.getJDA();
	}

	@Nonnull
	public GuildMessageReceivedEvent getEvent() {
		return this.event;
	}

	@Nonnull
	public Command getCommand() {
		return this.command;
	}

	@Nonnull
	public Parameters params() {
		return this.parameters;
	}

	@Nonnull
	public BotContext getBotContext() {
		return this.botContext;
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
	public VoiceChannel getConnectedVChannel() {
		return getAudioManager().getConnectedChannel();
	}

	@Nonnull
	public Customization getGuildCustomization() {
		return provider(CustomizationsProvider.class).get(getGuildIdLong());
	}

	@Nonnull
	public Shredder shredder() {
		return getBotContext().shredder();
	}

	@Nonnull
	public BotConfiguration getConfig() {
		return getBotContext().config();
	}

	@Nonnull
	public DataManager getData() {
		return getBotContext().data();
	}

	@Nonnull
	public CommandManager getCommands() {
		return getBotContext().commands();
	}

	@Nonnull
	public <T extends Command> String getCommandName(@Nonnull Class<T> clazz) {
		return getCommands().get(clazz).getName();
	}

	@Nonnull
	public <T extends Provider<?>> T provider(@Nonnull Class<T> clazz) {
		return getBotContext().provider(clazz);
	}

	@Nonnull
	public String getCommandName() {
		return getCommand().getName();
	}

	@Nonnull
	public String getCommandUsage() {
		return getCommand().getUsage(this);
	}

	public int getCommandRatelimit() {
		return getCommand().getRatelimit();
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
		return jda().getSelfUser();
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
		return getChannel().isNSFW();
	}

	@Nonnull
	public Role getPublicRole() {
		return getGuild().getPublicRole();
	}

	// ===============* Utilities *===============

	@Nonnull
	public String getCommandWithPrefix() {
		return getEffectivePrefix() + getCommandName();
	}

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
		return provider(CustomizationsProvider.class).get(getGuildIdLong()).isDj(getMember());
	}

	@Nonnull
	@SuppressWarnings("null")
	public String getEffectivePrefix() {
		return provider(CustomizationsProvider.class).get(getGuildIdLong())
			.getCustomPrefix()
			.orElse(getConfig().defaultPrefix());
	}

	public void messageSysadmins(@Nonnull Message message) {
		stream(getConfig().sysadminIds()).forEach(i -> shredder().sendPrivateMessage(i, message));
	}

	public void messageSysadmins(@Nonnull String message) {
		stream(getConfig().sysadminIds()).forEach(i -> shredder().sendPrivateMessage(i, message));
	}

	public void messageSysadmins(@Nonnull MessageEmbed embed, @Nonnull MessageEmbed... other) {
		stream(getConfig().sysadminIds()).forEach(i -> shredder().sendPrivateMessageEmbeds(i, embed, other));
	}

	// ===============* Shortcuts *===============

	public void requireSysadmin() {
		if (!isUserSysadmin())
			throw new NotSysadminException();
	}

	public void requireDj() {
		if (!isUserDj())
			throw new NotDjException(provider(CustomizationsProvider.class).get(getGuildIdLong())
				.getDjRoleId()
				.getAsLong());
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

	@CheckReturnValue
	@Nonnull
	public ContinuumException continuum(@Nonnull Object... realityAnchor) {
		return new ContinuumException(realityAnchor);
	}

	public boolean hasChannelPermission(@Nonnull Permission... permissions) {
		return getSelfMember().hasPermission(getChannel(), permissions);
	}

	public boolean hasGuildPermission(@Nonnull Permission... permissions) {
		return getSelfMember().hasPermission(permissions);
	}

	public boolean hasAuthorChannelPermission(@Nonnull Permission... permissions) {
		return getMember().hasPermission(getChannel(), permissions);
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
				throw new InsufficientPermissionException(getChannel(), perm);
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
	public CompletableFuture<Void> react(@Nonnull Emote emote) {
		if (canReact())
			return getMessage().addReaction(emote).submit();
		else
			return permissionExceptionFuture(MESSAGE_ADD_REACTION);
	}

	@Nonnull
	public CompletableFuture<Void> react(@Nonnull String unicode) {
		if (canReact())
			return getMessage().addReaction(unicode).submit();
		else
			return permissionExceptionFuture(MESSAGE_ADD_REACTION);
	}

	// ===============* reply *===============

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull Message message) {
		if (canTalk())
			return replyraw(message).submit();
		else {
			return permissionExceptionFuture(MESSAGE_WRITE);
		}
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull MessageBuilder builder) {
		return reply(builder.build());
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull String message) {
		if (canTalk())
			return replyMessage(getChannel().sendMessage(message)).submit();
		else {
			return permissionExceptionFuture(MESSAGE_WRITE);
		}
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull MessageEmbed embed) {
		if (canTalk())
			return replyMessage(getChannel().sendMessageEmbeds(embed)).submit();
		else {
			return permissionExceptionFuture(MESSAGE_WRITE);
		}
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nonnull EmbedBuilder builder) {
		if (canTalk())
			return replyMessage(getChannel().sendMessageEmbeds(builder.build())).submit();
		else
			return permissionExceptionFuture(MESSAGE_WRITE);
	}

	@Nonnull
	public CompletableFuture<Message> reply(@Nullable String title, @Nonnull String message, @Nullable String footer,
											@Nullable Color color) {
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

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
	public MessageAction replyraw(@Nonnull Message message) {
		return replyMessage(getChannel().sendMessage(message));
	}

	@Nonnull
	public MessageAction replyraw(@Nonnull MessageBuilder builder) {
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
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

		return direct(builder.build());
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		return direct(title, message, null, color);
	}

	@Nonnull
	public CompletableFuture<Message> direct(@Nonnull String message, @Nullable Color color) {
		return direct(null, message, null, color);
	}

	// ===============* replyfile *===============

	@Nonnull
	public CompletableFuture<Message> replyFile(@Nonnull byte[] data, @Nonnull String fileName,
												@Nonnull AttachmentOption... options) {
		return replyMessage(getChannel().sendFile(data, fileName, options)).submit();
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
	public CommandException error(boolean ratelimit, @Nonnull String message) {
		return new CommandException(new MessageBuilder(message), ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull String message) {
		return error(false, message);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(boolean ratelimit, @Nonnull MessageEmbed embed) {
		return new CommandException(new MessageBuilder(embed), ratelimit);
	}

	@Nonnull
	@CheckReturnValue
	public CommandException error(@Nonnull MessageEmbed embed) {
		return error(false, embed);
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
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

		return error(ratelimit, builder.build());
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

	// ===============* EventWaiter *===============

	public boolean isWaiterInited() {
		return this.waiter != null;
	}

	public EventUtils getWaiter() {
		if (!isWaiterInited())
			this.waiter = new EventUtils(getBotContext().ewl(), getUser(), getChannel());
		return this.waiter;
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
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

		return confirm(keepPrompt, builder.build());
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
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

		return askraw(builder.build());
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
		EmbedBuilder builder =
			new EmbedBuilder().setTitle(title).appendDescription(message).setFooter(footer, null).setColor(color);

		return ask(builder.build());
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

	protected CommandContext(@Nonnull CommandContext c) {
		this.event = c.event;
		this.command = c.command;
		this.botContext = c.botContext;
		this.parameters = c.parameters;
		this.waiter = c.waiter;
	}

	// ===============* Internal *===============

	private boolean getConfirmation(@Nonnull Message m, boolean keepPrompt) {
		if (canReact()) {
			m.addReaction(ACCEPT_EMOJI).queue();
			m.addReaction(DENY_EMOJI).queue();
		}
		try {
			return getWaiter().awaitBoolean(m, keepPrompt);
		} catch (InterruptedException e) { // NOSONAR
			throw asUnchecked(e);
		}
	}

	@Nonnull
	private <T> CompletableFuture<T> permissionExceptionFuture(@Nonnull Permission permission) {
		return exceptionFuture(new InsufficientPermissionException(getGuild(), permission));
	}

	@Nonnull
	private MessageAction replyMessage(MessageAction message) {
		return message.allowedMentions(emptyList())
			.reference(this.reference != null ? this.reference : getMessage())
			.mentionRepliedUser(false);
	}

}
