package libot.utils;

import static libot.core.Constants.FAILURE;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import javax.annotation.*;

import libot.core.FinderUtils;
import libot.core.argument.ArgumentList.Argument;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.entities.*;

public final class CommandUtils {

	@Nonnull
	public static User findUserOrAuthor(@Nonnull CommandContext c, @Nullable Argument arg) {
		return findMemberOrAuthor(c, arg).getUser();
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Member findMemberOrAuthor(@Nonnull CommandContext c, @Nullable Argument arg) {
		Member target;
		if (arg != null)
			target = FinderUtils.findMembers(c, arg.value())
				.stream()
				.findFirst()
				.orElseThrow(() -> c.errorf("Could not find user \"%s\".", FAILURE, escape(arg.value())));
		else
			target = c.getMember();
		return target;
	}

	private CommandUtils() {}
}
