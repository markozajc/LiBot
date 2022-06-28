package libot.commands.moderation;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MODERATION;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.Role;

public class GetRolesCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM);
		b.setTitlef("List of roles in %s:", c.getGuildName());
		b.setDescription(c.getGuild().getRoles().stream().map(Role::getAsMention).collect(joining(", ")));
		c.reply(b);
	}

	@Override
	public String getName() {
		return "getroles";
	}

	@Override
	public String[] getAliases() {
		return array("roles");
	}

	@Override
	public String getInfo() {
		return "Lists all roles in the guild.";
	}

	@Override
	public CommandCategory getCategory() {
		return MODERATION;
	}

}
