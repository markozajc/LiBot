package libot.utils;

import static libot.core.Constants.FAILURE;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.Nonnull;

import libot.core.FinderUtils;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.entities.*;

public final class CommandUtils {

	@Nonnull
	public static User findUserOrAuthor(@Nonnull CommandContext c) {
		return findMemberOrAuthor(c).getUser();
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Member findMemberOrAuthor(@Nonnull CommandContext c) {
		Member target;
		if (c.params().check(0))
			target = FinderUtils.findMembers(c, c.params().get(0))
				.stream()
				.findFirst()
				.orElseThrow(() -> c.errorf("Could not find user \"%s\".", FAILURE, escape(c.params().get(0))));
		else
			target = c.getMember();
		return target;
	}

	private CommandUtils() {}
}
