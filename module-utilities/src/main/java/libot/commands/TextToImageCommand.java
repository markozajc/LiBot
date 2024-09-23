package libot.commands;

import static java.awt.Font.PLAIN;
import static java.awt.RenderingHints.*;
import static java.awt.image.BufferedImage.TYPE_INT_RGB;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.SECONDS;
import static libot.core.Constants.FAILURE;
import static libot.core.argument.ParameterList.Parameter.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.*;
import static libot.core.commands.CommandCategory.UTILITIES;
import static net.dv8tion.jda.api.utils.FileUpload.fromData;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;

import javax.annotation.Nonnull;
import javax.imageio.ImageIO;

import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.*;
import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class TextToImageCommand extends Command {

	private static final int DEFAULT_SIZE = 16;

	@Nonnull private static final MandatoryParameter TEXT = mandatory(POSITIONAL, "text");
	@SuppressWarnings("null") @Nonnull private static final Parameter SIZE =
		optional(NAMED, "size", "text size (in points), %d by default".formatted(DEFAULT_SIZE));

	public TextToImageCommand() {
		super(CommandMetadata.builder(UTILITIES, "texttoimage")
			.aliases("tti")
			.ratelimit(10, SECONDS)
			.parameters(TEXT, SIZE)
			.description("""
				Converts given text into an image. Beware that this might not render certain special characters and \
				emoji correctly."""));
	}

	private static final String FORMAT_SIZE_TOO_LARGE = """
		The font size may not exceed **%d**.""";
	private static final String FORMAT_SIZE_NEGATIVE = """
		The font size must be positive.""";
	private static final String FORMAT_TEXT_TOO_LONG = """
		The text length may not exceed **%s characters**.""";

	private static final String FONT_NAME = "Whitney";
	private static final Color COLOR_FONT = new Color(220, 221, 222);
	private static final Color COLOR_BACKGROUND = new Color(54, 57, 63);
	private static final Canvas CANVAS = new Canvas();
	private static final int SIZE_CAP = 100;
	private static final int LENGTH_CAP = 300;

	@Override
	@SuppressWarnings({ "null", "resource" })
	public void execute(CommandContext c) throws IOException {
		int size = c.arg(SIZE).map(Argument::valueAsInt).orElse(DEFAULT_SIZE);

		if (size > SIZE_CAP)
			throw c.errorf(FORMAT_SIZE_TOO_LARGE, FAILURE, SIZE_CAP);

		if (size <= 0)
			throw c.error(FORMAT_SIZE_NEGATIVE, FAILURE);

		String text = c.arg(TEXT).value();
		if (text.length() >= LENGTH_CAP)
			throw c.errorf(FORMAT_TEXT_TOO_LONG, FAILURE, LENGTH_CAP);

		c.typing();

		String[] lines = text.split("\n");
		var font = new Font(FONT_NAME, PLAIN, size); // make sure the font is installed
		var fm = CANVAS.getFontMetrics(font); // seems safe enough to reuse canvas
		int fontHeight = fm.getHeight();
		var img = getImage(lines, fm, fontHeight);
		var g = getGraphics(font, img);

		g.setColor(COLOR_BACKGROUND);
		g.fillRect(0, 0, img.getWidth(), img.getHeight());

		g.setColor(COLOR_FONT);
		for (int i = 0; i < lines.length; i++)
			g.drawString(lines[i], 0, i * fontHeight + size + font.getBaselineFor('x'));

		try (var baos = new ByteArrayOutputStream()) {
			ImageIO.write(img, "png", baos);
			c.replyFiles(fromData(baos.toByteArray(), "image.png"));
		}
		g.dispose();
	}

	@Nonnull
	private static BufferedImage getImage(@Nonnull String[] lines, @Nonnull FontMetrics fm, int fontHeight) {
		int width = stream(lines).mapToInt(fm::stringWidth).filter(i -> i > 0).max().orElse(1);
		return new BufferedImage(width, fontHeight * lines.length, TYPE_INT_RGB);
	}

	@Nonnull
	private static Graphics2D getGraphics(@Nonnull Font font, @Nonnull BufferedImage img) {
		var g = img.createGraphics();
		g.setFont(font);
		g.setRenderingHint(KEY_TEXT_ANTIALIASING, VALUE_TEXT_ANTIALIAS_GASP);
		return g;
	}

}
