package libot.commands.informative;

import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.utils.CommandUtils;

public class AvatarCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var target = CommandUtils.findUserOrAuthor(c);
		var e = new EmbedPrebuilder(LITHIUM);
		e.setImage(target.getEffectiveAvatarUrl());
		e.setDescriptionf("[Download](%s)", target.getEffectiveAvatarUrl());
		c.reply(e);
	}

	@Override
	public String getName() {
		return "avatar";
	}

	@Override
	public String getInfo() {
		return """
			Displays a link to the mentioned user's avatar (profile picture). \
			If no user is mentioned, your avatar will be displayed.""";
	}

	@Override
	public String[] getParameters() {
		return array("[user]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("user to get avatar of");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return INFORMATIVE;
	}

}
