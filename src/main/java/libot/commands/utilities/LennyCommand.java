package libot.commands.utilities;

import static libot.core.commands.CommandCategory.UTILITIES;
import static libot.utils.Utilities.*;

import libot.core.commands.*;
import libot.core.entities.CommandContext;

public class LennyCommand extends Command {

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

	@Override
	public String getName() {
		return "lenny";
	}

	@Override
	public String[] getAliases() {
		return array("lennyface");
	}

	@Override
	public String getInfo() {
		return "Prints out a random lenny face.";
	}

	@Override
	public CommandCategory getCategory() {
		return UTILITIES;
	}

	// @formatter:off
	private static final String[][] EARS = new String[][] {
		array("\u252C\u2500\u252C\u30CE( ", "\u30CE)"),
		array("(\u30CE", ")\u30CE\u5F61\u253B\u2501\u253B"),
		array("q", "p"),
		array("(\u0E07", ")\u0E07"),
		array("\u02A2", "\u02A1"),
		array("\u2E2E", "?"),
		array("\u0295", "\u0294"),
		array("\u1597", "\u1598"),
		array("\u1566", "\u1565"),
		array("\u1566(", ")\u1565"),
		array("\u1559(", ")\u1557"),
		array("\u1633", "\u1630"),
		array("\u156E", "\u156D"),
		array("\u1573", "\u1572"),
		array("(", ")"),
		array("[", "]"),
		array("\u00AF\\_", "\\_/\u00AF"),
		array("\u0B67", "\u0B68"),
		array("\u0B68", "\u0B67"),
		array("\u291C(", ")\u290F"),
		array("\u261E", "\u261E"),
		array("\u146B", "\u1477"),
		array("\u1474", "\u1477"),
		array("\u30FD(", ")\uFF89"),
		array("\\(", ")/"),
		array("\u4E41(", ")\u310F"),
		array("\u2514[", "]\u2518"),
		array("(\u3065", ")\u3065"),
		array("(\u0E07", ")\u0E07"),
		array("\u239D", "\u23A0"),
		array("\u10DA(", "\u10DA)"),
		array("\u10DA,\u1511", "\u1510.\u10DA"),
		array("\u1555(", ")\u1557"),
		array("(\u2229", ")\u2283\u2501\u2606\uFF9F.\\*"),
		array("\\|")
	};
	private static final String[][] EYES = new String[][] {
		array("\u2310\u25A0", "\u25A0"),
		array(" \u0360\u00B0", " \u00B0"),
		array("\u21C0", "\u21BC"),
		array("\u00B4\u2022 ", " \u2022`"),
		array("\u00B4", "\\`"),
		array("\\`", "\u00B4"),
		array("\u00F3", "\u00F2"),
		array("\u00F2", "\u00F3"),
		array("\u2E0C", "\u2E0D"),
		array("\\>", "<"),
		array("\u01B8\u0335\u0321", "\u01B7"),
		array("\u15D2", "\u15D5"),
		array("\u27C3", "\u27C4"),
		array("\u2AA7", "\u2AA6"),
		array("\u2AA6", "\u2AA7"),
		array("\u2AA9", "\u2AA8"),
		array("\u2AA8", "\u2AA9"),
		array("\u2AB0", "\u2AAF"),
		array("\u2AD1", "\u2AD2"),
		array("\u2A34", "\u2A35"),
		array("\u2A7F", "\u2A80"),
		array("\u2A7E", "\u2A7D"),
		array("\u2A7A", "\u2A79"),
		array("\u2A79", "\u2A7A"),
		array("\u25CD", "\u25CE"),
		array("/\u0360-", "\u2510\u0361-\\"),
		array("\u2323", "\u2323\u201D"),
		array(" \u0361\u239A", " \u0361\u239A"),
		array("\u224B"),
		array("\u0AE6\u0A81"),
		array("  \u036F"),
		array("  \u034C"),
		array("\u0DC5"),
		array("\u25C9"),
		array("\u2609"),
		array("\u30FB"),
		array("\u25B0"),
		array("\u1D54"),
		array(" \uFF9F"),
		array("\u25A1"),
		array("\u263C"),
		array("\\*"),
		array("\\`"),
		array("\u2686"),
		array("\u229C"),
		array("\\>"),
		array("\u274D"),
		array("\uFFE3"),
		array("\u2500"),
		array("\u273F"),
		array("\u2022"),
		array("T"),
		array("^"),
		array("\u2C7A"),
		array("@"),
		array("\u020D"),
		array("x"),
		array("-"),
		array("$"),
		array("\u020C"),
		array("\u0298"),
		array("\uA74A"),
		array("\u2E1F"),
		array("\u0E4F"),
		array("\u2D32"),
		array("\u25D5"),
		array("\u25D4"),
		array("\u2727"),
		array("\u25A0"),
		array("\u2665"),
		array(" \u0361\u00B0"),
		array("\u00AC"),
		array(" \u00BA "),
		array("\u2A36"),
		array("\u2A31"),
		array("\u23D3"),
		array("\u23D2"),
		array("\u235C"),
		array("\u2364"),
		array("\u1696"),
		array("\u1D17"),
		array("\u0CA0"),
		array("\u03C3"),
		array("\u262F")
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
