//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
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
package libot.command;

import static javax.imageio.ImageIO.*;
import static libot.core.Constants.LITHIUM;
import static libot.core.argument.ParameterList.Parameter.optional;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.command.CommandCategory.INFORMATIVE;
import static libot.util.CommandUtils.findUserOrAuthor;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_USER;
import static net.dv8tion.jda.api.utils.FileUpload.fromData;
import static org.apache.commons.lang3.StringUtils.isNumeric;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.*;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import kong.unirest.core.Unirest;
import libot.core.argument.ParameterList.Parameter;
import libot.core.command.*;
import libot.core.entity.CommandContext;
import libot.core.extension.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.internal.requests.RestActionImpl;

public class AvatarCommand extends Command {

	@Nonnull private static final Parameter USER = optional(POSITIONAL, "user or ID", "user to get avatar of");

	public AvatarCommand() {
		super(CommandMetadata.builder(INFORMATIVE, "avatar").parameters(USER).description("""
			Displays a link to the mentioned user's avatar (profile picture). \
			If no user is mentioned and no user ID is provided, your avatar will be displayed."""));
	}

	private static final Logger LOG = getLogger(AvatarCommand.class);

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.arg(USER).ifPresentOrElse(param -> {
			if (isNumeric(param.value())) {
				var user = c.getShredder().getUserById(param.valueAsLong());
				if (user != null) {
					downloadAndSendAvatar(c, user);

				} else {
					c.getJda().retrieveUserById(param.valueAsLong()).queue(u -> downloadAndSendAvatar(c, u), e -> {
						if (e instanceof ErrorResponseException ere && ere.getErrorResponse() == UNKNOWN_USER)
							c.reply("Couldn't find a user with that ID.");
						else
							RestActionImpl.getDefaultFailure().accept(e);
					});
				}

			} else {
				downloadAndSendAvatar(c, findUserOrAuthor(c, param));
			}

		}, () -> {
			downloadAndSendAvatar(c, c.getUser());
		});

	}

	@SuppressWarnings({ "null", "resource" })
	private static void downloadAndSendAvatar(@Nonnull CommandContext c, @Nonnull User user) {
		var url = user.getEffectiveAvatarUrl();
		var resp = Unirest.get(url + "?size=4096").asBytes();

		if (!resp.isSuccess()) {
			throw c.errorf("Non-zero status code when downloading '%s?size=4096':%d %s", url, resp.getStatus(),
						   resp.getStatusText());
		}

		var extension = url.substring(url.lastIndexOf('.') + 1);
		var e = new EmbedPrebuilder(LITHIUM);

		e.setTitlef("%s's avatar", user.getEffectiveName());
		e.setImage("attachment://avatar." + extension);
		e.setDescriptionf("[View original](%s?size=4096) (%d KiB%s)", url, resp.getBody().length / 1024,
						  getImageResolutionString(resp.getBody(), extension));
		c.getChannel().sendFiles(fromData(resp.getBody(), "avatar." + extension)).setEmbeds(e.build()).queue();
	}

	@Nonnull
	private static String getImageResolutionString(@Nonnull byte[] data, @Nonnull String extension) {
		var resolution = getImageDimension(data, extension);
		return resolution == -1 ? "" : ", " + resolution + " × " + resolution + " pixels";
	}

	private static int getImageDimension(@Nonnull byte[] data, @Nonnull String extension) {
		var readers = getImageReadersBySuffix(extension);
		while (readers.hasNext()) {
			var reader = readers.next();
			try (var is = createImageInputStream(new ByteArrayInputStream(data))) {
				reader.setInput(is);
				return reader.getWidth(reader.getMinIndex());

			} catch (IOException e) {
				LOG.warn("Failed to decode the image resolution for {}", extension);
				LOG.warn("", e);

			} finally {
				reader.dispose();
			}
		}
		return -1;
	}

}
