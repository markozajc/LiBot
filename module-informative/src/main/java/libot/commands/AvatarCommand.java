package libot.commands;

import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.utils.CommandUtils.findUserOrAuthor;
import static libot.utils.ParseUtils.parseLong;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_USER;
import static net.dv8tion.jda.internal.requests.RestActionImpl.getDefaultFailure;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class AvatarCommand extends Command {

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		if (c.params().check(0) && !MentionType.USER.getPattern().matcher(c.params().get(0)).matches()) {
			long id = parseLong(c.params().get(0));
			var user = c.shredder().getUserById(id);
			if (user != null) {
				sendAvatar(c, user);
			} else {
				c.jda().retrieveUserById(id).queue(u -> sendAvatar(c, u), e -> {
					if (e instanceof ErrorResponseException ere && ere.getErrorResponse() == UNKNOWN_USER) {
						c.reply("Couldn't find a user with that ID.");
					} else {
						getDefaultFailure().accept(e);
					}
				});
			}

		} else {
			sendAvatar(c, findUserOrAuthor(c));
		}
	}

	private static void sendAvatar(@Nonnull CommandContext c, @Nonnull User user) {
		var e = new EmbedPrebuilder(LITHIUM);
		e.setImage(user.getEffectiveAvatarUrl());
		e.setDescriptionf("[Download](%s)", user.getEffectiveAvatarUrl());
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
			If no user is mentioned and no user ID is provided, your avatar will be displayed.""";
	}

	@Override
	public String[] getParameters() {
		return array("[user or ID]");
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
