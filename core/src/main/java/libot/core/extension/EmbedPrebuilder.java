//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.core.extension;

import javax.annotation.*;

import libot.core.entity.Color;
import net.dv8tion.jda.api.EmbedBuilder;

public final class EmbedPrebuilder extends EmbedBuilder {

	public EmbedPrebuilder(@Nullable String title, @Nonnull String message, @Nullable String footer,
						   @Nullable String footerIconUrl, @Nullable Color color) {
		super.setTitle(title);
		super.appendDescription(message);
		super.setFooter(footer, footerIconUrl);
		if (color != null)
			super.setColor(color.rgb());
	}

	public EmbedPrebuilder() {}

	public EmbedPrebuilder(@Nullable Color color) {
		if (color != null)
			super.setColor(color.rgb());
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
		super.setDescription(description.formatted(args));
		return this;
	}

	@Nonnull
	@SuppressWarnings("null")
	public EmbedPrebuilder appendDescriptionf(@Nonnull String description, @Nonnull Object... args) {
		super.appendDescription(description.formatted(args));
		return this;
	}

	@Nonnull
	@SuppressWarnings("null")
	public EmbedPrebuilder addFieldf(@Nonnull String title, @Nonnull String value, @Nonnull Object... args) {
		return addField(title, value.formatted(args));
	}

	@Nonnull
	@SuppressWarnings("null")
	public EmbedPrebuilder addFieldf(boolean inline, @Nonnull String title, @Nonnull String value,
									 @Nonnull Object... args) {
		super.addField(title, value.formatted(args), inline);
		return this;
	}

	@Nonnull
	public EmbedPrebuilder addField(@Nonnull String title, @Nonnull String value) {
		super.addField(title, value, false);
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setTitlef(@Nonnull String title, @Nonnull Object... args) {
		super.setTitle(title.formatted(args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setAuthorf(@Nonnull String author, @Nonnull Object... args) {
		super.setAuthor(author.formatted(args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setFooterf(@Nonnull String footer, @Nonnull Object... args) {
		super.setFooter(footer.formatted(args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setThumbnailf(@Nonnull String url, @Nonnull Object... args) {
		super.setThumbnail(url.formatted(args));
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setDescription(@Nonnull Object description) {
		super.setDescription(description.toString());
		return this;
	}

	@Nonnull
	public EmbedPrebuilder setColor(@Nonnull Color color) { // NOSONAR java:S2177 this is intentional
		super.setColor(color.rgb());
		return this;
	}

}
