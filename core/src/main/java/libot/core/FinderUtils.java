package libot.core;

import static java.util.Collections.*;
import static net.dv8tion.jda.api.entities.Message.MentionType.ROLE;
import static net.dv8tion.jda.api.utils.MiscUtil.parseSnowflake;

import java.util.*;
import java.util.regex.Matcher;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import org.apache.commons.collections4.list.SetUniqueList;

import libot.core.entities.CommandContext;
import libot.core.shred.Shredder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public final class FinderUtils {

	//////////////////////////////////////////////////////////////////////////////////////
	// Users
	//////////////////////////////////////////////////////////////////////////////////////

	@Nonnull
	@SuppressWarnings("null")
	public static List<User> preferFromGuild(@Nonnull CommandContext c, @Nonnull List<User> candidates) {
		if (candidates.isEmpty())
			return emptyList();

		var result = new ArrayList<>(candidates);
		sort(result, (u1, u2) -> {
			if (!(c.getGuild().isMember(u1) ^ c.getGuild().isMember(u2)))
				return 0;
			else if (c.getGuild().isMember(u1))
				return -1;
			else if (c.getGuild().isMember(u2))
				return 1;
			else
				return 0;
		});
		return result;
	}

	/**
	 * Searches all visible {@link User}s for matching/similar name and mention.
	 *
	 * @param c
	 * @param query
	 *
	 * @return list of found users, sorted by similarity to the query, with direct
	 *         mentions first
	 */
	@Nonnull
	@SuppressWarnings("null")
	public static List<User> findUsers(@Nonnull CommandContext c, @Nonnull String query) {
		String text = query.toLowerCase().trim();

		List<User> found = SetUniqueList.setUniqueList(new ArrayList<>());
		findUserById(c.shredder(), query).ifPresent(found::add);
		findUsersFromMentions(c.shredder(), found, text);
		findUsersFromText(c.shredder(), found, text);

		return unmodifiableList(found);
	}

	@Nonnull
	@SuppressWarnings("null")
	private static Optional<User> findUserById(@Nonnull Shredder shredder, @Nonnull String query) {
		try {
			long id = parseSnowflake(query);
			return Optional.ofNullable(shredder.getUserById(id));
		} catch (NumberFormatException e) {
			return Optional.empty();
		}
	}

	@SuppressWarnings("null")
	private static void findUsersFromMentions(@Nonnull Shredder shredder, @Nonnull List<User> destination,
											  @Nonnull String query) {
		Matcher matcher = MentionType.USER.getPattern().matcher(query);
		while (matcher.find()) {
			try {
				findUserById(shredder, matcher.group(1)).ifPresent(destination::add);
			} catch (NumberFormatException e) {
				// Can be ignored
			}
		}
	}

	@SuppressWarnings("null")
	private static void findUsersFromText(@Nonnull Shredder shredder, @Nonnull List<User> destination,
										  @Nonnull String query) {
		shredder.getJoinedUserCache()
			.filter(u -> getPriority(query, u.getName().toLowerCase().trim()) > 0)
			.sorted((u1, u2) -> compare(u2.getName().toLowerCase().trim(), u1.getName().toLowerCase().trim(), query))
			.forEachOrdered(destination::add);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// Roles
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Searches all {@link Role}s in the current {@link Guild} for matching/similar name
	 * and mention.
	 *
	 * @param c
	 * @param query
	 *
	 * @return list of found roles, sorted by similarity to the query, with direct
	 *         mentions first (can be empty)
	 */
	@SuppressWarnings("null")
	public static List<Role> findRoles(@Nonnull CommandContext c, @Nonnull String query) {
		String text = query.toLowerCase().trim();

		List<Role> found = SetUniqueList.setUniqueList(new ArrayList<>());
		var cache = c.getGuild().getRoleCache();
		findSnowflakeById(cache, query).ifPresent(found::add);
		findSnowflakesFromMentions(cache, ROLE, found, text);
		findRolesFromText(cache.stream(), found, text);

		return unmodifiableList(found);
	}

	@SuppressWarnings("null")
	private static void findRolesFromText(@Nonnull Stream<Role> roles, @Nonnull List<Role> destination,
										  @Nonnull String query) {
		roles.filter(r -> getPriority(query, r.getName().toLowerCase().trim()) > 0)
			.sorted((r1, r2) -> compare(r2.getName().toLowerCase().trim(), r1.getName().toLowerCase().trim(), query))
			.forEachOrdered(destination::add);
	}

	//////////////////////////////////////////////////////////////////////////////////////
	// Members
	//////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Searches all {@link Member}s in the current {@link Guild} for matching/similar
	 * name and mention.
	 *
	 * @param c
	 * @param query
	 *
	 * @return list of found members, sorted by similarity to the query, with direct
	 *         mentions first (can be empty)
	 */
	@SuppressWarnings("null")
	public static List<Member> findMembers(@Nonnull CommandContext c, @Nonnull String query) {
		String text = query.toLowerCase().trim();

		List<Member> found = SetUniqueList.setUniqueList(new ArrayList<>());
		var cache = c.getGuild().getMemberCache();
		findSnowflakeById(cache, query).ifPresent(found::add);
		findSnowflakesFromMentions(cache, MentionType.USER, found, text);
		findMembersFromText(cache.stream(), found, text);

		return unmodifiableList(found);
	}

	@SuppressWarnings("null")
	private static void findMembersFromText(@Nonnull Stream<Member> members, @Nonnull List<Member> destination,
											@Nonnull String query) {
		members.filter(m -> {
			int priorityNick = 0;
			int priorityName;
			String nickname = m.getNickname();
			if (nickname != null)
				priorityNick = getPriority(query, nickname.toLowerCase().trim());
			priorityName = getPriority(query, m.getUser().getName().toLowerCase().trim());
			return priorityName > 0 || priorityNick > 0;
		})
			.sorted((m1, m2) -> compare(m2.getEffectiveName().toLowerCase().trim(),
										m1.getEffectiveName().toLowerCase().trim(), query))
			.forEachOrdered(destination::add);
	}

	@SuppressWarnings("null")
	private static <T extends ISnowflake> void findSnowflakesFromMentions(@Nonnull SnowflakeCacheView<T> cache,
																		  @Nonnull MentionType type,
																		  @Nonnull List<T> destination,
																		  @Nonnull String query) {
		var matcher = type.getPattern().matcher(query);
		while (matcher.find()) {
			try {
				findSnowflakeById(cache, matcher.group(1)).ifPresent(destination::add);
			} catch (NumberFormatException e) {
				// Can be ignored
			}
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private static <T extends ISnowflake> Optional<T> findSnowflakeById(@Nonnull SnowflakeCacheView<T> index,
																		@Nonnull String query) {
		try {
			long id = parseSnowflake(query);
			return Optional.ofNullable(index.getElementById(id));

		} catch (NumberFormatException e) {
			// Query is not an ID, we can ignore this exception
			return Optional.empty();
		}
	}

	@SuppressWarnings("null")
	private static int compare(@Nonnull String name1, @Nonnull String name2, @Nonnull String query) {
		return Integer.compare(getPriority(query, name1.toLowerCase().trim()),
							   getPriority(query, name2.toLowerCase().trim()));
	}

	private static int getPriority(@Nonnull String actual, @Nonnull String expected) {
		if (expected.equals(actual))
			return 4;
		else if (expected.startsWith(actual))
			return 3;
		else if (expected.endsWith(actual))
			return 2;
		else if (expected.contains(actual))
			return 1;
		else
			return 0;
	}

	private FinderUtils() {}

}
