package libot.commands;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;

public class GetRolesCommand extends Command {

	public GetRolesCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "getroles")
			.aliases("roles")
			.description("Lists all roles in the guild."));
	}

	@Override
	public void execute(CommandContext c) {
		c.getGuild().loadMembers().onSuccess(members -> {
			var b = new EmbedPrebuilder(LITHIUM);
			b.setDescription(c.getGuild().getRoles().stream().map(r -> {
				var count = members.stream().filter(m -> m.getRoles().contains(r)).count();
				return "- %s (%d member%s)".formatted(r.getAsMention(), count, count == 1 ? "" : "s");
			}).collect(joining("\n")));
			c.reply(b);
		});
	}

}
