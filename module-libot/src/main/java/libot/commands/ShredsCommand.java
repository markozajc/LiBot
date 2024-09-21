package libot.commands;

import static de.vandermeer.skb.interfaces.transformers.textformat.TextAlignment.CENTER;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.LIBOT;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;

import de.vandermeer.asciitable.AsciiTable;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.shred.Shredder.Shred;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.utils.cache.SnowflakeCacheView;

public class ShredsCommand extends Command {

	public ShredsCommand() {
		super(CommandMetadata.builder(LIBOT, "shreds").aliases("shredder").description("""
			Displays status information on LiBot's shreds, \
			sub-units of the bot that allow it to exceed the 100 hard-limit on guilds imposed by Discord through the \
			bot verification and privileged intents requirements."""));
	}

	@Override
	public void execute(CommandContext c) throws Exception {
		var t = new AsciiTable();
		t.getRenderer().setCWC(TABLE_CWC);
		t.getContext().setGrid(TABLE_GRID);
		t.addRule();
		t.addRow("Name", "ID", "Status", "Capacity").setTextAlignment(CENTER).setPaddingLeftRight(1);
		t.addStrongRule();
		c.getShredder().getShreds().forEach(s -> {
			var name = s.name();
			if (c.getShredder().getCurrentShred() == s)
				name = "> %s <".formatted(s.name());
			t.addRow(name, s.jda().getSelfUser().getId(), s.jda().getStatus().toString(),
					 "%d / 100".formatted(s.jda().getGuildCache().size()))
				.setPaddingLeftRight(1);
		});
		t.addRule();

		long totalGuilds = c.getShredder()
			.getShreds()
			.stream()
			.map(Shred::jda)
			.map(JDA::getGuildCache)
			.mapToLong(SnowflakeCacheView::size)
			.sum();
		t.addRow(null, null, "", "%d / %d".formatted(totalGuilds, c.getShredder().getShreds().size() * 100))
			.setPaddingLeftRight(1);
		t.addRule();
		c.reply(codeblock("Registered shreds\n" + t.render()));
	}

}
