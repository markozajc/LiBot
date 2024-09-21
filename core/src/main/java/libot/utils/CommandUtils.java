package libot.utils;

import static libot.core.Constants.FAILURE;
import static net.dv8tion.jda.api.utils.MarkdownSanitizer.escape;

import java.util.Optional;

import javax.annotation.Nonnull;

import libot.core.FinderUtils;
import libot.core.argument.ArgumentList.Argument;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.entities.*;

public final class CommandUtils {

	@Nonnull
	public static User findUserOrAuthor(@Nonnull CommandContext c, @Nonnull Optional<Argument> arg) {
		return findMemberOrAuthor(c, arg).getUser();
	}

	@Nonnull
	public static User findUserOrAuthor(@Nonnull CommandContext c, @Nonnull Argument arg) {
		return findMemberOrAuthor(c, arg).getUser();
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Member findMemberOrAuthor(@Nonnull CommandContext c, @Nonnull Optional<Argument> arg) {
		return arg.map(u -> findMemberOrAuthor(c, u)).orElse(c.getMember());
	}

	@Nonnull
	@SuppressWarnings("null")
	public static Member findMemberOrAuthor(@Nonnull CommandContext c, @Nonnull Argument arg) {
		return FinderUtils.findMembers(c, arg.value())
			.stream()
			.findFirst()
			.orElseThrow(() -> c.errorf("Could not find user \"%s\".", FAILURE, escape(arg.value())));
	}

	private CommandUtils() {}
}
