package libot.commands;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.LIBOT;
import static org.apache.commons.text.WordUtils.wrap;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;

public class FeedbackCommand extends Command {

	private static final String FORMAT_FOOTER_ASK_SUBJECT = """
		Type in a subject or EXIT to abort""";
	private static final String FORMAT_FOOTER_CONFIRM = """
		Are you sure you want to send this mail?""";
	private static final String FORMAT_FOOTER_ASK_MESSAGE = """
		Type in a message or EXIT to abort""";
	private static final String FORMAT_SUBJECT_TOO_LONG = """
		The subject field can only contain up to %d characters""";
	private static final String FORMAT_MESSAGE_TOO_LONG = """
		The message can only contain up to %d characters""";

	private static final int MAIL_WRAP = 70;
	private static final int MESSAGE_CAP = 1500;
	private static final int SUBJECT_CAP = 30;

	private static final String TITLE = "LiBot feedback system";
	private static final String MAIL_TEMPLATE = """
		To: LiBot developers
		Subject: %s
		────────────────────────────
		%s
		""";

	private static String generateMail(String subject, String message) {
		return wrap(format(MAIL_TEMPLATE, subject, message), MAIL_WRAP);
	}

	@Override
	public void execute(CommandContext c) {
		var b = new EmbedPrebuilder(LITHIUM).setTitle(TITLE)
			.setAuthor("From " + c.getUserTag(), null, c.getAvatarUrl())
			.setFooter(FORMAT_FOOTER_ASK_SUBJECT)
			.setDescription(generateMail("", ""));
		c.reply(b);
		var subject = ask(c, SUBJECT_CAP, FORMAT_SUBJECT_TOO_LONG);

		b.setFooter(FORMAT_FOOTER_ASK_MESSAGE).setDescription(generateMail(subject, ""));
		c.reply(b);
		var message = ask(c, MESSAGE_CAP, FORMAT_MESSAGE_TOO_LONG);

		b.setFooter(FORMAT_FOOTER_CONFIRM).setDescription(generateMail(subject, message));
		if (!c.confirm(b))
			throw c.cancel();

		b.setFooter(format("Sender ID: %s", c.getUserId()));
		c.reply("Thank you for your feedback!", SUCCESS);

		var mail = b.build();
		c.messageSysadmins(p -> p.sendMessageEmbeds(mail));
	}

	@Nonnull
	private static String ask(@Nonnull CommandContext c, int lengthCap, @Nonnull String tooLongFormat) {
		while (true) {
			var resp = c.askraw();
			if ("exit".equalsIgnoreCase(resp.getContentStripped())) {
				resp.addReaction(ACCEPT_EMOJI).queue();
				throw c.cancel();

			} else if (resp.getContentDisplay().length() > lengthCap) {
				c.replyf(tooLongFormat, WARN, lengthCap);

			} else {
				return resp.getContentDisplay();
			}
		}
	}

	@Override
	public String getName() {
		return "feedback";
	}

	@Override
	public String getInfo() {
		return "Sends feedback to LiBot's developers.";
	}

	@Override
	public int getRatelimit() {
		return 60;
	}

	@Override
	public CommandCategory getCategory() {
		return LIBOT;
	}

}
