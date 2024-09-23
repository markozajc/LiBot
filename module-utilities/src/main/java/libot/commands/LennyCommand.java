package libot.commands;

import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.random;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class LennyCommand extends Command {

	public LennyCommand() {
		super(CommandMetadata.builder(UTILITIES, "lenny")
			.aliases("lennyface")
			.description("Prints out a random lenny face."));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var ears = random(EARS);
		var eyes = random(EYES);
		var face = random(FACES);
		var lenny = new StringBuilder();
		lenny.append(ears[0]);
		lenny.append(eyes[0]);
		lenny.append(face);
		lenny.append(eyes[eyes.length == 2 ? 1 : 0]);
		lenny.append(ears[ears.length == 2 ? 1 : 0]);
		c.reply(lenny.toString());
	}

	// @formatter:off
	private static final String[][] EARS = new String[][] {
		{"\u252C\u2500\u252C\u30CE( ", "\u30CE}"},
		{"(\u30CE", "}\u30CE\u5F61\u253B\u2501\u253B"},
		{"q", "p"},
		{"(\u0E07", "}\u0E07"},
		{"\u02A2", "\u02A1"},
		{"\u2E2E", "?"},
		{"\u0295", "\u0294"},
		{"\u1597", "\u1598"},
		{"\u1566", "\u1565"},
		{"\u1566(", "}\u1565"},
		{"\u1559(", "}\u1557"},
		{"\u1633", "\u1630"},
		{"\u156E", "\u156D"},
		{"\u1573", "\u1572"},
		{"(", "}"},
		{"[", "]"},
		{"\u00AF\\_", "\\_/\u00AF"},
		{"\u0B67", "\u0B68"},
		{"\u0B68", "\u0B67"},
		{"\u291C(", "}\u290F"},
		{"\u261E", "\u261E"},
		{"\u146B", "\u1477"},
		{"\u1474", "\u1477"},
		{"\u30FD(", "}\uFF89"},
		{"\\(", "}/"},
		{"\u4E41(", "}\u310F"},
		{"\u2514[", "]\u2518"},
		{"(\u3065", "}\u3065"},
		{"(\u0E07", "}\u0E07"},
		{"\u239D", "\u23A0"},
		{"\u10DA(", "\u10DA}"},
		{"\u10DA,\u1511", "\u1510.\u10DA"},
		{"\u1555(", "}\u1557"},
		{"(\u2229", "}\u2283\u2501\u2606\uFF9F.\\*"},
		{"\\|"}
	};
	private static final String[][] EYES = new String[][] {
		{"\u2310\u25A0", "\u25A0"},
		{" \u0360\u00B0", " \u00B0"},
		{"\u21C0", "\u21BC"},
		{"\u00B4\u2022 ", " \u2022`"},
		{"\u00B4", "\\`"},
		{"\\`", "\u00B4"},
		{"\u00F3", "\u00F2"},
		{"\u00F2", "\u00F3"},
		{"\u2E0C", "\u2E0D"},
		{"\\>", "<"},
		{"\u01B8\u0335\u0321", "\u01B7"},
		{"\u15D2", "\u15D5"},
		{"\u27C3", "\u27C4"},
		{"\u2AA7", "\u2AA6"},
		{"\u2AA6", "\u2AA7"},
		{"\u2AA9", "\u2AA8"},
		{"\u2AA8", "\u2AA9"},
		{"\u2AB0", "\u2AAF"},
		{"\u2AD1", "\u2AD2"},
		{"\u2A34", "\u2A35"},
		{"\u2A7F", "\u2A80"},
		{"\u2A7E", "\u2A7D"},
		{"\u2A7A", "\u2A79"},
		{"\u2A79", "\u2A7A"},
		{"\u25CD", "\u25CE"},
		{"/\u0360-", "\u2510\u0361-\\"},
		{"\u2323", "\u2323\u201D"},
		{" \u0361\u239A", " \u0361\u239A"},
		{"\u224B"},
		{"\u0AE6\u0A81"},
		{"  \u036F"},
		{"  \u034C"},
		{"\u0DC5"},
		{"\u25C9"},
		{"\u2609"},
		{"\u30FB"},
		{"\u25B0"},
		{"\u1D54"},
		{" \uFF9F"},
		{"\u25A1"},
		{"\u263C"},
		{"\\*"},
		{"\\`"},
		{"\u2686"},
		{"\u229C"},
		{"\\>"},
		{"\u274D"},
		{"\uFFE3"},
		{"\u2500"},
		{"\u273F"},
		{"\u2022"},
		{"T"},
		{"^"},
		{"\u2C7A"},
		{"@"},
		{"\u020D"},
		{"x"},
		{"-"},
		{"$"},
		{"\u020C"},
		{"\u0298"},
		{"\uA74A"},
		{"\u2E1F"},
		{"\u0E4F"},
		{"\u2D32"},
		{"\u25D5"},
		{"\u25D4"},
		{"\u2727"},
		{"\u25A0"},
		{"\u2665"},
		{" \u0361\u00B0"},
		{"\u00AC"},
		{" \u00BA "},
		{"\u2A36"},
		{"\u2A31"},
		{"\u23D3"},
		{"\u23D2"},
		{"\u235C"},
		{"\u2364"},
		{"\u1696"},
		{"\u1D17"},
		{"\u0CA0"},
		{"\u03C3"},
		{"\u262F"}
	};
	private static final String[] FACES = new String[] {
		"v",
		"\u1D25",
		"\u15DD",
		"\u0460",
		"\u15DC",
		"\u13B2",
		"\u1A13",
		"\u1A0E",
		"\u30EE",
		"\u256D\u035C\u0296\u256E",
		" \u035F\u0644\u035C",
		" \u035C\u0296",
		" \u035F\u0296",
		" \u0296\u032F",
		"\u03C9",
		" \u00B3",
		" \u03B5 ",
		"\uFE4F",
		"\u25A1",
		"\u0644\u035C",
		"\u203F",
		"\u256D\u256E",
		"\u203F\u203F",
		"\u25BE",
		"\u2038",
		"\u0414",
		"\u2200",
		"!",
		"\u4EBA",
		".",
		"\u30ED",
		"\\_",
		"\u0DF4",
		"\u047D",
		"\u0D0C",
		"\u23E0",
		"\u234A",
		"\u2358",
		"\u30C4",
		"\u76CA",
		"\u256D\u2229\u256E",
		"\u0139\u032F",
		"\u25E1",
		" \u035C\u3064",
		"\uFEAA\u035F\u0360"
	};
	// @formatter:on

}
