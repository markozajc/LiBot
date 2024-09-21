package libot.commands;

import static java.lang.Integer.toUnsignedString;
import static java.util.Arrays.stream;
import static java.util.regex.Pattern.UNICODE_CHARACTER_CLASS;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;

import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import de.vandermeer.asciitable.AsciiTable;
import libot.core.argument.ArgumentList.Argument;
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.processes.ProcessManager;
import libot.core.processes.ProcessManager.CommandProcess;
import libot.utils.ParseUtils;

public class KillProcessCommand extends Command {

	@Nonnull private static final Parameter PIDS =
		Parameter.optional(POSITIONAL, "pids", "PIDs of the processes to kill");
	@Nonnull private static final CommandMetadata META =
		new CommandMetadata.Builder(ADMINISTRATIVE, "killprocess").description("""
			Kills a running process.

			If no PID is provided, a list of running processes is shown.
			Process flags:
			- `C`hannel/`G`uild matches
			- `U`ser matches
			- Has custom `D`ata
			- Thread state: `N`ew, `R`unnable, `W`aiting, `B`locked, `T`imed waiting, terminate`D`
			""").aliases("kill", "ps").parameters(PIDS).build();

	public KillProcessCommand() {
		super(META);
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.arg(PIDS).ifPresentOrElse(pids -> {
			c.requireSysadmin();
			killProcesses(c, pids);

		}, () -> {
			listProcesses(c);
		});
	}

	private static final Pattern SPLIT = Pattern.compile("[\\s,]+", UNICODE_CHARACTER_CLASS);

	@SuppressWarnings("null")
	private static void killProcesses(@Nonnull CommandContext c, @Nonnull Argument pids) {
		var missing = SPLIT.splitAsStream(pids.value())
			.mapToInt(ParseUtils::parseInt)
			.sorted()
			.distinct()
			.filter(KillProcessCommand::killProcess)
			.toArray();

		if (missing.length == 0)
			c.react(ACCEPT_EMOJI);
		else
			c.reply(stream(missing).mapToObj("PID %d not found"::formatted).collect(joining("\n")), DISABLED);
	}

	private static boolean killProcess(int pid) {
		var proc = ProcessManager.getProcess(pid);
		if (proc == null)
			return true;

		ProcessManager.interrupt(proc);
		return false;
	}

	private static void listProcesses(@Nonnull CommandContext c) {
		var processes = ProcessManager.getProcesses();
		AsciiTable t = new AsciiTable();
		t.getRenderer().setCWC(TABLE_CWC);
		t.getContext().setGrid(TABLE_GRID);
		t.addRule();
		t.addRow("PID", "FLGS", "COMMAND").setPaddingLeftRight(1);
		t.addStrongRule();
		for (var proc : processes) {
			t.addRow(toUnsignedString(proc.getPid()), getFlags(c, proc), proc.getCommand().getName())
				.setPaddingLeftRight(1);
		}
		t.addRule();

		c.replyf(codeblock("Running processes\n" + t.render()));
	}

	@Nonnull
	@SuppressWarnings("null")
	private static String getFlags(@Nonnull CommandContext c, @Nonnull CommandProcess process) {
		var flags = new StringBuilder("----");
		if (process.getChannelId() == c.getChannelIdLong())
			flags.setCharAt(0, 'C');
		else if (process.getGuildId() == c.getGuildIdLong())
			flags.setCharAt(0, 'G');

		if (process.getUserId() == c.getUserIdLong())
			flags.setCharAt(1, 'U');

		if (process.getData() != null)
			flags.setCharAt(2, 'D');

		var thread = ProcessManager.getThread(process);
		if (thread != null) {
			switch (thread.getState()) {
				case NEW -> flags.setCharAt(3, 'N');
				case RUNNABLE -> flags.setCharAt(3, 'R');
				case WAITING -> flags.setCharAt(3, 'W');
				case BLOCKED -> flags.setCharAt(3, 'B');
				case TIMED_WAITING -> flags.setCharAt(3, 'T');
				case TERMINATED -> flags.setCharAt(3, 'D');
			}
		}
		return flags.toString();
	}

}
