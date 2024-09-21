package libot.commands;

import static java.lang.String.format;
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
		long humans = guild.getMembers().size() - bots;

		String members = format(FORMAT_MEMBERS, humans, bots > 0 ? format(FORMAT_MEMBERS_WITH_BOTS, bots) : "");

		return guild.retrieveMetaData()
			.submit()
			.thenApply(MetaData::getApproximatePresences)
			.thenApply(online -> members + format(FORMAT_MEMBERS_ONLINE, online))
			.thenAccept(data -> builder.addField("Members", codeblock(data), false));
	}

}
