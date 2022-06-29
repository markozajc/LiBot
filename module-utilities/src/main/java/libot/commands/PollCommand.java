package libot.commands;

import static java.lang.Math.round;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.time.Instant.now;
import static java.util.Collections.unmodifiableList;
import static libot.commands.PollCommand.Poll.createPoll;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.ParseUtils.parseTime;
import static libot.utils.Utilities.*;
import static net.dv8tion.jda.api.Permission.*;
import static net.dv8tion.jda.api.entities.MessageEmbed.TITLE_MAX_LENGTH;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import java.time.Instant;
import java.util.*;

import javax.annotation.Nonnull;

import org.eclipse.collections.api.list.primitive.IntList;
import org.eclipse.collections.impl.factory.primitive.*;

import libot.commands.PollCommand.Poll.PollException;
import libot.core.commands.*;
import libot.core.commands.exceptions.ContinuumException;
import libot.core.commands.exceptions.runtime.TimeParseException;
import libot.core.data.providers.TimedTaskProvider.TimedTask;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.shred.Shredder;
import libot.module.ModuleLibotShared;
import libot.providers.PollProvider;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.*;
import net.dv8tion.jda.api.requests.*;
import net.dv8tion.jda.api.utils.TimeFormat;

public class PollCommand extends Command {

	private static final String SWITCH_DELETE = "delete";
	private static final String SWITCH_END = "end";
	private static final String SWITCH_LIST = "list";
	private static final String SWITCH_HELP = "help";

	private static final String FORMAT_JUMPLINK = "https://discord.com/channels/%d/%d/%d";

	private static final String FORMAT_CONFIRM_POLL_ACTION = format("""
		Are you sure you want to %%s **[%%s](%s)**?""", FORMAT_JUMPLINK);
	private static final String FORMAT_SET_TITLE = """
		Please type in the title for your poll (eg. "The food poll").""";
	private static final String FORMAT_SET_DESCRIPTION = """
		Please type in the description (the main question if you will) of your poll (eg. "What's your favorite \
		food?")""";
	private static final String FORMAT_SET_OPTIONS = """
		Please provide the different options for your poll (you can add up to 10).
		Start by typing in option names (eg. `Broccoli`, `Pizza`, `Burger`), one per message.""";
	private static final String FORMAT_SET_OPTIONS_FOOTER = """
		When you're done, type in DONE. You can also type in UNDO to remove the last option.
		""";
	private static final String FORMAT_SET_CHANNEL = """
		Please mention the channel (for example: %s) you want to run the poll in.""";
	private static final String FORMAT_SET_DURATION = """
		Please choose a [time](https://libot.eu.org/doc/commands/parameter-types.html#time) until the poll ends.""";
	private static final String FORMAT_SET_MULTIPLE_CHOICES = format("""
		Do you want users to be able to cast a vote on multiple choices? Recommended: %s.""", DENY_EMOJI);
	private static final String FORMAT_MULTIPLE_CHOICES_PERMISSION_NOTE = format("""

		Note: LiBot needs the `%s` permission in %%s if you want to set this to %s.""", MESSAGE_MANAGE.getName(),
																				 DENY_EMOJI);
	private static final String FORMAT_SET_MENTION_EVERYONE = """
		Do you want LiBot to mention @everyone when the poll starts?""";
	private static final String FORMAT_SET_PUBLIC = """
		LiBot will directly message you the results of this poll. Do you want it to send a carbon copy of the results \
		to %s?""";

	private static final String FORMAT_TITLE_TOO_LONG = """
		The title length may not exceed **%d characters**.""";
	private static final String FORMAT_DESCRIPTION_TOO_LONG = """
		The description length may not exceed **%d characters**.""";
	private static final String FORMAT_CHOICE_TOO_LONG =
		"The length of the choice description may not exceed **%d characters**.";
	private static final String FORMAT_CHOICES_CAP_HIT = """
		The poll may not have more than **%d choices**. \
		Input `DONE` to proceed or `UNDO` to remove a choice.""";
	private static final String FORMAT_POLL_CAP_EXCEEDED = """
		You can only have **%d polls** running at a time. Find out what polls are running with `%s list`.""";

	private static final String FORMAT_DONE_LACKING_CHOICES = """
		You must add at least two choices.""";
	private static final String FORMAT_UNDO_LACKING_CHOICES = """
		Nothing to undo.""";
	private static final String FORMAT_CROSS_GUILD = """
		Cross-guild polls are not allowed.""";
	private static final String FORMAT_CANT_TALK = format("""
		LiBot lacks the `%s` & `%s` permissions in %%s.""", MESSAGE_WRITE.getName(), MESSAGE_READ.getName());
	private static final String FORMAT_MISSING_PERMISSION = """
		LiBot lacks the `%s` permission in %s.""";
	private static final String FORMAT_MULTIPLE_CHANNELS = """
		You've provided multiple channels. Please mention only one.""";
	private static final String FORMAT_NO_CHANNELS = """
		You have not mentioned a channel. Please mention a channel like this: %s""";
	private static final String FORMAT_INVALID_TIMESTAMP = """
		Please enter a **[valid timestamp](https://libot.eu.org/doc/commands/parameter-types.html#time)**!""";
	private static final String FORMAT_NEGATIVE_DURATION = """
		LiBot's time travel module is currently unavailable. Please try again in `-4` days!""";
	private static final String FORMAT_DISCLOSE_NO_PERMISSION = format("""
		LiBot was configured to send a copy of the results to %%s, but it could not do so because it does not \
		have the `%s` permission in %%s anymore.""", MESSAGE_WRITE.getName());
	private static final String FORMAT_CHANNEL_PERMISSIONS = format("""
		LiBot does not have the permission to read the [poll message](%s) in <#%%d>.""", FORMAT_JUMPLINK);
	private static final String FORMAT_UNKNOWN_ERROR = """
		This action could not be carried out due to an unknown error.""";
	private static final String FORMAT_POLL_GONE = """
		The poll message or the channel it was in was deleted.""";
	private static final String FORMAT_UNKNOWN_SWITCH = """
		Unknown subcommand `%s`! Please choose between `%s`, `%s`, `%s`, and `%s`.""";
	private static final String FORMAT_POLL_INDEX_OUT_OF_RANGE = """
		Poll **#%d** does not exist. Consult `%s list` for a list of running polls.""";
	private static final String FORMAT_POLL_EXPIRED = """
		The poll has expired.""";

	private static final String FORMAT_EXIT_FOOTER = """
		Type in EXIT to abort""";
	private static final String FORMAT_CHOICES_FOOTER = """
		Type in DONE to proceed, UNDO to undo, or EXIT to abort""";
	private static final String FORMAT_LIST_FOOTER = """
		%d / %d running polls • '%s end <index>' to end a poll, '%s delete <index>' to delete a poll
		""";

	private static final String FORMAT_LIST_FIELD_VALUE = format("""
		%%s [[view]](%s)
		""", FORMAT_JUMPLINK);

	private static final int POLL_COUNT_CAP = 20;
	private static final int DESCRIPTION_CAP = 500;
	private static final int CHOICE_DESCRIPTION_LENGTH_CAP = 300;

	public static final List<String> EMOJI;
	static {
		var emojis = new ArrayList<String>(10);
		emojis.add(0, "\u0031\u20E3");
		emojis.add(1, "\u0032\u20E3");
		emojis.add(2, "\u0033\u20E3");
		emojis.add(3, "\u0034\u20E3");
		emojis.add(4, "\u0035\u20E3");
		emojis.add(5, "\u0036\u20E3");
		emojis.add(6, "\u0037\u20E3");
		emojis.add(7, "\u0038\u20E3");
		emojis.add(8, "\u0039\u20E3");
		emojis.add(9, "\uD83D\uDD1F");
		EMOJI = unmodifiableList(emojis);
	}
	private static final int CHOICE_COUNT_CAP = EMOJI.size();

	public static class Poll implements TimedTask {

		public static class PollException extends Exception {

			public enum Reason {
				MISSING_CHANNEL,
				CHANNEL_PERMISSIONS,
				MISSING_MESSAGE,
				ALREADY_DONE,
				UNKNOWN;
			}

			@Nonnull
			private final Reason reason;

			public PollException(@Nonnull Reason reason) {
				this.reason = reason;
			}

			@Nonnull
			public Reason getReason() {
				return this.reason;
			}

		}

		private String title;
		private transient String description;
		private List<String> choices;
		private long authorId;
		private transient String authorName;
		private transient String authorAvatarUrl;
		private long guildId;
		private long channelId;
		private long messageId = -1;
		private long endTime;
		private boolean allowMultipleVotes;
		private boolean disclosePublicly;
		private boolean done;
		private transient Object mutex = new Object();

		@Nonnull
		public static Poll createPoll(@Nonnull String title, @Nonnull String description, @Nonnull List<String> choices,
									  boolean allowMoreVotes, boolean mentionEveryone, boolean disclosePublicly,
									  long endsOn, @Nonnull CommandContext c, @Nonnull TextChannel target) {
			var poll = new Poll(title, description, choices, c.getUserIdLong(), c.getUsername(),
								c.getUser().getEffectiveAvatarUrl(), target.getGuild().getIdLong(), target.getIdLong(),
								endsOn, allowMoreVotes, disclosePublicly);
			poll.publishPoll(target, mentionEveryone);
			return poll;
		}

		public Poll(String title, String description, List<String> choices, long authorId, String authorName,
					String authorAvatarUrl, long guildId, long channelId, long endsOn, boolean allowMoreVotes,
					boolean disclosePublicly) {
			this.title = title;
			this.choices = choices;
			this.endTime = endsOn;
			this.description = description;
			this.authorId = authorId;
			this.authorName = authorName;
			this.authorAvatarUrl = authorAvatarUrl;
			this.guildId = guildId;
			this.channelId = channelId;
			this.messageId = -1;
			this.allowMultipleVotes = allowMoreVotes;
			this.disclosePublicly = disclosePublicly;
		}

		@SuppressWarnings("null")
		private void publishPoll(@Nonnull TextChannel target, boolean mentionEveryone) {
			if (this.messageId != -1)
				throw new IllegalStateException("This poll has already been published.");

			var action = target.sendMessageEmbeds(createEmbed());
			if (mentionEveryone)
				action = action.append(target.getGuild().getPublicRole().getAsMention());
			var message = action.complete();
			if (target.getGuild().getSelfMember().hasPermission(target, MESSAGE_ADD_REACTION)) {
				for (int i = 0; i < this.choices.size(); i++)
					message.addReaction(EMOJI.get(i)).complete();
			}
			this.messageId = message.getIdLong();
		}

		// An empty constructor to please GSON
		public Poll() {}

		public String getTitle() {
			return this.title;
		}

		public long getGuildId() {
			return this.guildId;
		}

		public long getChannelId() {
			return this.channelId;
		}

		public long getMessageId() {
			return this.messageId;
		}

		public boolean allowsMultipleVotes() {
			return this.allowMultipleVotes;
		}

		@Override
		public long endTime() {
			return this.endTime;
		}

		public boolean isDone() {
			return this.done;
		}

		@SuppressWarnings("null")
		public void end(@Nonnull Shredder shredder, boolean requested) throws PollException {
			synchronized (this.mutex) {
				if (this.done)
					throw new PollException(PollException.Reason.ALREADY_DONE);
				this.done = true;
				var message = getMessage(shredder);
				var reactions = new MessageReaction[10];
				message.getReactions().forEach(r -> {
					int i = EMOJI.indexOf(r.getReactionEmote().getName());
					if (i != -1)
						reactions[i] = r;
				});

				var results = IntLists.mutable.withInitialCapacity(10);
				if (this.allowMultipleVotes) {
					for (int i = 0; i < reactions.length; i++) {
						if (reactions[i] != null)
							results.addAtIndex(i, reactions[i].getCount() - 1);
						else
							results.addAtIndex(i, 0);
					}

				} else {
					var users = LongSets.mutable.empty();
					for (int i = 0; i < reactions.length; i++) {
						if (reactions[i] != null) {
							var voters = reactions[i].retrieveUsers()
								.stream()
								.filter(u -> !u.isBot())
								.mapToLong(User::getIdLong)
								.filter(u -> !users.contains(u))
								.toArray();
							users.addAll(voters);
							results.addAtIndex(i, voters.length);
						} else {
							results.addAtIndex(i, 0);
						}
					}
				}

				sendResults(shredder, results, message);
				markEnded(message, requested);
			}
		}

		private void delete(@Nonnull Shredder shredder) throws PollException {
			synchronized (this.mutex) {
				this.done = true;
				try {
					getMessage(shredder).delete().complete();
				} catch (InsufficientPermissionException e) {
					throw new PollException(PollException.Reason.CHANNEL_PERMISSIONS);
				}
			}
		}

		private Message getMessage(@Nonnull Shredder shredder) throws PollException {
			var channel = shredder.getTextChannelById(this.channelId);
			if (channel == null)
				throw new PollException(PollException.Reason.MISSING_CHANNEL);

			Message m;
			try {
				m = channel.retrieveMessageById(this.messageId).complete();
			} catch (InsufficientPermissionException e) {
				throw new PollException(PollException.Reason.CHANNEL_PERMISSIONS);

			} catch (ErrorResponseException e) {
				if (e.getErrorResponse() == ErrorResponse.UNKNOWN_MESSAGE)
					throw new PollException(PollException.Reason.MISSING_MESSAGE);
				else
					throw new PollException(PollException.Reason.UNKNOWN);
			}

			return m;
		}

		@SuppressWarnings("null")
		public void sendResults(@Nonnull Shredder shredder, @Nonnull IntList results, @Nonnull Message message) {
			double total = results.sum();
			var r = new StringBuilder();
			r.append("[View poll](");
			r.append(format(FORMAT_JUMPLINK, getGuildId(), getChannelId(), getMessageId()));
			r.append(")\n");
			for (int i = 0; i < this.choices.size(); i++) {
				int count = i >= results.size() ? 0 : results.get(i);
				r.append(format("**`%3d%%`** (%d vote%s) - %s", round(count / total * 100), count, plural(count),
								escape(this.choices.get(i), true)));
				if (i < this.choices.size() - 1)
					r.append('\n');
			}

			var eb = new EmbedBuilder();
			eb.setTitle("Results for **" + escape(this.title, true) + "**");
			eb.setDescription(r.toString());
			eb.setColor(SUCCESS);
			var embed = eb.build();

			var pca = shredder.openPrivateChannelById(this.authorId);
			if (pca != null)
				pca.flatMap(pc -> pc.sendMessageEmbeds(embed)).queue(null, PRIVATE_MESSAGE_ERROR_HANDLER);

			if (this.disclosePublicly) {
				var channel = (TextChannel) message.getChannel();
				if (channel.canTalk()) {
					channel.sendMessageEmbeds(embed).queue();

				} else if (pca != null) {
					pca.flatMap(pc -> pc.sendMessage(format(FORMAT_DISCLOSE_NO_PERMISSION, channel.getAsMention(),
															channel.getAsMention())))
						.queue(null, PRIVATE_MESSAGE_ERROR_HANDLER);
				}
			}
		}

		@Nonnull
		private MessageEmbed createEmbed() {
			return new EmbedBuilder().setTitle(this.title)
				.setDescription(this.description + "\n**Choices:**\n" + parseChoices(this.choices))
				.setColor(LITHIUM)
				.setAuthor("A poll by " + this.authorName, this.authorAvatarUrl)
				.setFooter("This poll ends", null)
				.setTimestamp(Instant.ofEpochMilli(this.endTime))
				.build();
		}

		private void markEnded(@Nonnull Message message, boolean requested) {
			var embed = message.getEmbeds().get(0);
			var eb = new EmbedBuilder(embed).setColor(DISABLED)
				.setFooter("This poll has ended" + (requested ? " (prematurely)" : ""));

			if (requested)
				eb.setTimestamp(now());

			message.editMessageEmbeds(eb.build()).queue();
			boolean hideReactions = !this.disclosePublicly
				&& message.getGuild().getSelfMember().hasPermission(message.getTextChannel(), MESSAGE_MANAGE);
			if (hideReactions)
				message.clearReactions().queue();
			else {
				message.getReactions()
					.stream()
					.map(r -> r.removeReaction(message.getAuthor()))
					.forEach(RestAction::queue);
			}
		}

	}

	@Override
	public void execute(@Nonnull CommandContext c) throws Exception {
		var provider = c.provider(PollProvider.class);
		if (c.params().check(0))
			managePolls(c, provider);
		else
			createNewPoll(c, provider);
	}

	@SuppressWarnings("null")
	private void managePolls(@Nonnull CommandContext c, @Nonnull PollProvider prov) {
		String mode = c.params().get(0).toLowerCase();
		var polls = prov.getPolls(c.getGuildIdLong());
		switch (mode) {
			case SWITCH_HELP -> ModuleLibotShared.sendUsage(c, this);
			case SWITCH_DELETE, SWITCH_END -> managePoll(c, prov, mode, polls);
			case SWITCH_LIST -> listPolls(c, polls);
			default -> throw c.errorf(FORMAT_UNKNOWN_SWITCH, FAILURE, mode, SWITCH_DELETE, SWITCH_END, SWITCH_LIST,
									  SWITCH_HELP);
		}
	}

	@SuppressWarnings("null")
	private static void listPolls(@Nonnull CommandContext c, @Nonnull List<Poll> polls) {
		var b = new EmbedPrebuilder(LITHIUM);
		b.setTitle("Active polls for " + c.getGuildName());
		b.setFooterf(FORMAT_LIST_FOOTER, polls.size(), POLL_COUNT_CAP, c.getCommandWithPrefix(),
					 c.getCommandWithPrefix());
		for (int i = 0; i < polls.size(); i++) {
			var p = polls.get(i);
			b.addFieldf("#" + Integer.toString(i + 1), FORMAT_LIST_FIELD_VALUE, p.getTitle(), p.getGuildId(),
						p.getChannelId(), p.getMessageId());
		}
		c.reply(b);
	}

	@SuppressWarnings("null")
	private static void managePoll(@Nonnull CommandContext c, @Nonnull PollProvider prov, @Nonnull String mode,
								   @Nonnull List<Poll> polls) throws ContinuumException {
		int index = c.params().getInt(1);
		if (index < 1 || index > polls.size())
			throw c.errorf(FORMAT_POLL_INDEX_OUT_OF_RANGE, FAILURE, index, c.getCommandWithPrefix());
		var poll = polls.get(index - 1);
		if (c.confirmf(FORMAT_CONFIRM_POLL_ACTION, LITHIUM, mode, escape(poll.getTitle(), true), poll.getGuildId(),
					   poll.getChannelId(), poll.getMessageId())) {
			try {
				switch (mode) {
					case SWITCH_DELETE -> poll.delete(c.shredder());
					case SWITCH_END -> poll.end(c.shredder(), true);
					default -> throw new ContinuumException();
				}
				prov.deregister(poll);
				c.react(ACCEPT_EMOJI);
			} catch (PollException e) {
				var message = switch (e.getReason()) {
					case ALREADY_DONE -> throw c.continuum(e); // getPolls() filters out completed polls
					case CHANNEL_PERMISSIONS -> format(FORMAT_CHANNEL_PERMISSIONS, poll.getGuildId(),
													   poll.getChannelId(), poll.getMessageId(), poll.getChannelId());
					case MISSING_CHANNEL, MISSING_MESSAGE -> FORMAT_POLL_GONE;
					case UNKNOWN -> FORMAT_UNKNOWN_ERROR;
				};
				throw c.error(message, FAILURE);
			}
		}
	}

	private static void createNewPoll(@Nonnull CommandContext c, @Nonnull PollProvider prov) {
		if (prov.getPolls(c.getGuildIdLong()).size() > POLL_COUNT_CAP)
			throw c.errorf(FORMAT_POLL_CAP_EXCEEDED, FAILURE, POLL_COUNT_CAP, c.getCommandWithPrefix());

		var title = getTitle(c);
		var description = getDescription(c);
		var choices = getChoices(c);
		var target = getTextChannel(c);
		var allowMultivote = shouldAllowMoreVotes(c, target);
		var discloseResultsPublicly = shouldDiscloseResultsPublicly(c, target);
		var mentionEveryone = shouldMentionEveryone(c, target);
		var time = getTime(c);

		var b = new EmbedBuilder().setTitle("Poll creation")
			.setColor(LITHIUM)
			.setDescription("Are you sure you want to publish this poll?.")
			.addField("Title", title, true)
			.addField("Description", description, true)
			.addField("Choices", parseChoices(choices), true)
			.addField("Ends", TimeFormat.DATE_TIME_LONG.format(time), true)
			.addField("Allows more votes per user?", allowMultivote ? "Yes (not recommended)" : "No", true)
			.addField("Will mention everyone on start?", mentionEveryone ? "Yes" : "No", true)
			.addField("Will the results be disclosed publicly?", discloseResultsPublicly ? "Yes" : "No", true);

		if (c.confirm(b)) {
			if (time < currentTimeMillis())
				throw c.error(FORMAT_POLL_EXPIRED, FAILURE);

			var poll = createPoll(title, description, choices, allowMultivote, mentionEveryone, discloseResultsPublicly,
								  time, c, target);
			prov.register(poll);
		}
	}

	private static boolean shouldMentionEveryone(@Nonnull CommandContext c, @Nonnull TextChannel target) {
		boolean botherAsking = c.getMember().hasPermission(target, MESSAGE_MENTION_EVERYONE)
			&& c.getSelfMember().hasPermission(target, MESSAGE_MENTION_EVERYONE);
		return botherAsking && c.confirm(FORMAT_SET_MENTION_EVERYONE, LITHIUM);
	}

	private static boolean shouldDiscloseResultsPublicly(@Nonnull CommandContext c, @Nonnull TextChannel target) {
		return c.confirmf(FORMAT_SET_PUBLIC, LITHIUM, target.getAsMention());
	}

	@SuppressWarnings("null")
	private static boolean shouldAllowMoreVotes(@Nonnull CommandContext c, @Nonnull TextChannel target) {
		var message = FORMAT_SET_MULTIPLE_CHOICES;
		if (!c.getSelfMember().hasPermission(target, MESSAGE_MANAGE))
			message += format(FORMAT_MULTIPLE_CHOICES_PERMISSION_NOTE, target.getAsMention());
		return c.confirm(message, LITHIUM);
	}

	private static long getTime(@Nonnull CommandContext c) {
		c.reply(FORMAT_SET_DURATION, LITHIUM);
		while (true) {
			var resp = c.askraw();
			checkExit(c, resp);
			try {
				long time = parseTime(resp.getContentDisplay());
				if (time < 0) // can't happen right now, keeping it in case parseTime is changed to
							  // support past timestamps
					c.reply(FORMAT_NEGATIVE_DURATION, FAILURE);
				else
					return time + currentTimeMillis();
			} catch (TimeParseException e) {
				c.reply(FORMAT_INVALID_TIMESTAMP, FAILURE);
			}
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static TextChannel getTextChannel(@Nonnull CommandContext c) {
		c.replyf(FORMAT_SET_CHANNEL, LITHIUM, c.getChannelMention());

		while (true) {
			var resp = c.askraw();
			TextChannel result;
			checkExit(c, resp);
			if (resp.getMentionedChannels().isEmpty()) {
				c.replyf(FORMAT_NO_CHANNELS, FAILURE, c.getChannelMention());

			} else if (resp.getMentionedChannels().size() > 1) {
				c.replyf(FORMAT_MULTIPLE_CHANNELS, FAILURE, c.getChannelMention());

			} else if ((result = resp.getMentionedChannels().get(0)).getGuild() != c.getGuild()) {
				c.reply(FORMAT_CROSS_GUILD, FAILURE);

			} else if (!result.canTalk()) {
				c.replyf(FORMAT_CANT_TALK, FAILURE, result.getAsMention());

			} else if (!c.getSelfMember().hasPermission(result, MESSAGE_ADD_REACTION)) {
				c.replyf(FORMAT_MISSING_PERMISSION, FAILURE, MESSAGE_ADD_REACTION.getName(), result.getAsMention());

			} else if (!c.getSelfMember().hasPermission(result, MESSAGE_EMBED_LINKS)) {
				c.replyf(FORMAT_MISSING_PERMISSION, FAILURE, MESSAGE_EMBED_LINKS.getName(), result.getAsMention());

			} else {
				return result;
			}
		}
	}

	@Nonnull
	private static List<String> getChoices(@Nonnull CommandContext c) {
		var choices = new ArrayList<String>(10);
		c.reply(null, FORMAT_SET_OPTIONS, FORMAT_SET_OPTIONS_FOOTER, LITHIUM);
		while (true) {
			var resp = c.askraw();
			checkExit(c, resp);
			var text = resp.getContentDisplay();
			boolean printChoices = false;

			if ("undo".equalsIgnoreCase(text)) {
				if (choices.isEmpty()) {
					c.reply(FORMAT_UNDO_LACKING_CHOICES, DISABLED);
				} else {
					choices.remove(choices.size() - 1);
					printChoices = true;
				}

			} else if ("done".equalsIgnoreCase(text)) {
				if (choices.size() >= 2)
					break;
				else
					c.reply(FORMAT_DONE_LACKING_CHOICES, DISABLED);

			} else if (choices.size() >= CHOICE_COUNT_CAP) {
				c.replyf(FORMAT_CHOICES_CAP_HIT, WARN, CHOICE_COUNT_CAP);

			} else if (text.length() > CHOICE_DESCRIPTION_LENGTH_CAP) {
				c.replyf(FORMAT_CHOICE_TOO_LONG, WARN, CHOICE_DESCRIPTION_LENGTH_CAP);

			} else {
				choices.add(text);
				printChoices = true;
			}
			if (printChoices)
				c.reply("Choices:", parseChoices(choices), FORMAT_CHOICES_FOOTER, LITHIUM);
		}

		return choices;
	}

	@Nonnull
	private static String getDescription(@Nonnull CommandContext c) {
		c.reply(null, FORMAT_SET_DESCRIPTION, FORMAT_EXIT_FOOTER, LITHIUM);
		while (true) {
			var resp = c.askraw();
			checkExit(c, resp);
			if (resp.getContentDisplay().length() > DESCRIPTION_CAP)
				c.replyf(FORMAT_DESCRIPTION_TOO_LONG, WARN, DESCRIPTION_CAP);
			else
				return resp.getContentDisplay();
		}
	}

	@Nonnull
	private static String getTitle(@Nonnull CommandContext c) {
		c.reply("Welcome to LiBot's poll-creator 2000!", FORMAT_SET_TITLE, FORMAT_EXIT_FOOTER, LITHIUM);

		while (true) {
			var resp = c.askraw();
			checkExit(c, resp);
			if (resp.getContentDisplay().length() > TITLE_MAX_LENGTH)
				c.replyf(FORMAT_TITLE_TOO_LONG, WARN, TITLE_MAX_LENGTH);
			else
				return resp.getContentDisplay();
		}
	}

	private static void checkExit(@Nonnull CommandContext c, @Nonnull Message resp) {
		if ("exit".equalsIgnoreCase(resp.getContentDisplay())) {
			if (c.canReact())
				resp.addReaction(ACCEPT_EMOJI).queue();
			throw c.cancel();
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String parseChoices(List<String> choices) {
		var r = new StringBuilder();
		for (int i = 0; i < choices.size(); i++) {
			r.append(EMOJI.get(i));
			r.append(": ");
			r.append(choices.get(i));
			if (i < choices.size() - 1)
				r.append(",\n");
		}
		return r.toString();
	}

	@Override
	public String getName() {
		return "poll";
	}

	@Override
	@SuppressWarnings("null")
	public String getInfo() {
		return format("""
			Creates a poll. When a poll ends, you receive the results via a direct message \
			(the results can optionally also be sent to a channel). You may have up to %d polls running at a time.
			Options for the `switch` parameter:
			`LIST  ` - displays all running polls in this guild
			`END   ` - ends current poll prematurely, requires `index`
			`DELETE` - cancels a poll, requires `index`
			`HELP  ` - display usage information""", POLL_COUNT_CAP);
	}

	@Override
	public Permission[] getPermissions() {
		return array(MANAGE_SERVER);
	}

	@Override
	public String[] getParameters() {
		return array("[switch]", "[index]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("list, end, delete, or help", "index of the poll for end and delete");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

}
