package libot.core.commands;

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

	public static CommandCategory getCategory(String categoryName) {
		String categoryNameUpper = categoryName.toUpperCase();
		for (CommandCategory category : CommandCategory.values()) {
			if (category.name().equals(categoryNameUpper))
				return category;
		}

		return null;

	}

}
