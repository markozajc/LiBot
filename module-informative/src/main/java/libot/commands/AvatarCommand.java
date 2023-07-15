package libot.commands;

import static javax.imageio.ImageIO.*;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.INFORMATIVE;
import static libot.core.commands.exceptions.ExceptionHandler.reportException;
import static libot.utils.CommandUtils.findUserOrAuthor;
import static libot.utils.ParseUtils.parseLong;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.requests.ErrorResponse.UNKNOWN_USER;
import static net.dv8tion.jda.internal.requests.RestActionImpl.getDefaultFailure;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.*;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import kong.unirest.*;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.entities.Message.MentionType;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;

public class AvatarCommand extends Command {

	private static final Logger LOG = getLogger(AvatarCommand.class);

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		if (c.params().check(0) && !MentionType.USER.getPattern().matcher(c.params().get(0)).matches()) {
			long id = parseLong(c.params().get(0));
			var user = c.shredder().getUserById(id);
			if (user != null) {
				downloadAndSendAvatar(c, user);
			} else {
				c.jda().retrieveUserById(id).queue(u -> downloadAndSendAvatar(c, u), e -> {
					if (e instanceof ErrorResponseException ere && ere.getErrorResponse() == UNKNOWN_USER) {
						c.reply("Couldn't find a user with that ID.");
					} else {
						getDefaultFailure().accept(e);
					}
				});
			}

		} else {
			downloadAndSendAvatar(c, findUserOrAuthor(c));
		}
	}

	@SuppressWarnings("null")
	private static void downloadAndSendAvatar(@Nonnull CommandContext c, @Nonnull User user) {
		var url = user.getEffectiveAvatarUrl();
		Unirest.get(url + "?size=4096").asBytesAsync().thenAccept(b -> sendAvatar(c, url, b)).exceptionally(t -> {
			reportException(c, t);
			c.error("Couldn't download the avatar due to an unknown error");
			return null;
		});
	}

	@SuppressWarnings("null")
	private static void sendAvatar(@Nonnull CommandContext c, @Nonnull String url, @Nonnull HttpResponse<byte[]> b) {
		if (!b.isSuccess())
			throw new RuntimeException("Non-zero status code when downloading '%s?size=4096':%d %s"
				.formatted(url, b.getStatus(), b.getStatusText()));

		sendAvatar(c, url, b.getBody());
	}

	@SuppressWarnings("null")
	private static void sendAvatar(@Nonnull CommandContext c, @Nonnull String url, @Nonnull byte[] data) {
		var extension = url.substring(url.lastIndexOf('.') + 1);
		var e = new EmbedPrebuilder(LITHIUM);
		e.setImage("attachment://avatar." + extension);
		e.setDescriptionf("[View original](%s?size=4096) (%d KiB%s)", url, data.length / 1024,
						  getImageResolutionString(data, extension));
		c.getChannel().sendFile(data, "avatar." + extension).setEmbeds(e.build()).queue();
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
