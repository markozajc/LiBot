package libot.core.commands;

import java.util.Optional;

import javax.annotation.Nonnull;

public enum CommandCategory {

	ADMINISTRATIVE,
	CUSTOMIZATION,
	GAMES,
	INFORMATIVE,
	LIBOT,
	MODERATION,
	MONEY,
	MUSIC,
	SEARCH,
	UTILITIES;

	public static Optional<CommandCategory> getCategory(@Nonnull String categoryName) {
		String categoryNameUpper = categoryName.toUpperCase();
		for (CommandCategory category : CommandCategory.values()) {
			if (category.name().equals(categoryNameUpper))
				Optional.of(category);
		}

		return Optional.empty();

	}

}
