package libot.core.extensions;

import static java.lang.String.format;

import java.awt.Color;

import javax.annotation.*;

import net.dv8tion.jda.api.EmbedBuilder;

public final class EmbedPrebuilder extends EmbedBuilder {

	public EmbedPrebuilder(@Nullable String title, @Nonnull String message, @Nullable String footer,
						   @Nullable String footerIconUrl, @Nullable Color color) {
		super.setTitle(title);
		super.appendDescription(message);
		super.setFooter(footer, footerIconUrl);
		super.setColor(color);
	}

	public EmbedPrebuilder() {}

	public EmbedPrebuilder(@Nullable Color color) {
		super.setColor(color);
	}

	public EmbedPrebuilder(@Nullable String title, @Nonnull String message, @Nullable String footer,
						   @Nullable Color color) {
		this(title, message, footer, null, color);
	}

	public EmbedPrebuilder(@Nullable String title, @Nonnull String message, @Nullable Color color) {
		this(title, message, null, null, color);
	}

	public EmbedPrebuilder(@Nonnull String message, @Nullable Color color) {
		this(null, message, null, null, color);
	}

	public EmbedPrebuilder(@Nonnull String message) {
		this(null, message, null, null, null);
	}

	@Nonnull
	public EmbedPrebuilder setDescriptionf(@Nonnull String description, @Nonnull Object... args) {
		super.setDescription(format(description, args));
		return this;
	}

	@Nonnull
	@SuppressWarnings("null")
	public EmbedPrebuilder appendDescriptionf(@Nonnull String description, @Nonnull Object... args) {
		super.appendDescription(format(description, args));
		return this;
	}

	@SuppressWarnings("null")
	@Nonnull
	public EmbedPrebuilder addFieldf(@Nonnull String title, @Nonnull String value, @Nonnull Object... args) {
		return addField(title, format(value, args));
	}

	@Nonnull
	public EmbedPrebuilder addFieldf(boolean inline, @Nonnull String title, @Nonnull String value,
									 @Nonnull Object... args) {
		super.addField(title, format(value, args), inline);
		return this;
	}

	@Nonnull
	public EmbedPrebuilder addField(@Nonnull String title, @Nonnull String value) {
		super.addField(title, value, false);
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setTitlef(@Nonnull String title, @Nonnull Object... args) {
		super.setTitle(format(title, args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setAuthorf(@Nonnull String author, @Nonnull Object... args) {
		super.setAuthor(format(author, args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setFooterf(@Nonnull String footer, @Nonnull Object... args) {
		super.setFooter(format(footer, args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setDescription(@Nonnull Object description) {
		super.setDescription(description.toString());
		return this;
	}

}
