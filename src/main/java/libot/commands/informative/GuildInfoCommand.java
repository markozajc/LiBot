package libot.commands.informative;

import static java.lang.String.format;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.Utilities.array;
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

	private static final String FORMAT_MEMBERS_WITH_BOTS = " humans + %d bots";
	private static final String FORMAT_MEMBERS = "%d%s ";
	private static final String FORMAT_MEMBERS_ONLINE = "(%d online)";

	@Nonnull
	@SuppressWarnings("null")
	private static String parseVerificationLevel(@Nonnull VerificationLevel vl) {
		return codeblock(switch (vl) {
			case NONE -> "None";
			case LOW -> "Low";
			case MEDIUM -> "Medium";
			case HIGH -> "\u256F\u00B0\u25A1\u00B0\uFF09\u256F\uFE35 \u253B\u2501\u253B (high)";
			case VERY_HIGH -> """
				\u253B\u2501\u253B \uFF90\u30FD(\u0CA0\u76CA\u0CA0)\u30CE\u5F61\u253B\u2501\u253B (very high)""";
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
		return guild.retrieveOwner(false)
			.submit()
			.thenAccept(owner -> builder.addField("Owner", owner.getUser().getAsMention(), false));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static CompletableFuture<Void> appendMembers(@Nonnull Guild guild, @Nonnull EmbedBuilder builder) {
		long bots = guild.getMembers().stream().map(Member::getUser).filter(User::isBot).count();
		long humans = guild.getMembers().size() - bots;

		String members = format(FORMAT_MEMBERS, humans, bots > 0 ? format(FORMAT_MEMBERS_WITH_BOTS, bots) : "");

		return guild.retrieveMetaData()
			.submit()
			.thenApply(MetaData::getApproximatePresences)
			.thenApply(online -> members + format(FORMAT_MEMBERS_ONLINE, online))
			.thenAccept(data -> builder.addField("Members", codeblock(data), false));
	}

	@Override
	public String getName() {
		return "guildinfo";
	}

	@Override
	public String[] getAliases() {
		return array("server", "serverinfo", "guild", "gi", "si");
	}

	@Override
	public String getInfo() {
		return "Displays some information and statistics for the current guild.";
	}

	@Override
	public CommandCategory getCategory() {
		return INFORMATIVE;
	}

}
