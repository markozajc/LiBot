package libot.commands;

import static java.lang.Integer.toUnsignedString;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static libot.utils.Utilities.array;

import javax.annotation.Nonnull;

import de.vandermeer.asciitable.AsciiTable;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.processes.ProcessManager;
import libot.core.processes.ProcessManager.CommandProcess;
import libot.utils.ParseUtils;

public class KillProcessCommand extends Command {

	private static final String FORMAT_NOT_FOUND = """
		PID %d not found""";
	private static final String FORMAT_TABLE = """
		```
		Running processes
		%s
		```""";

	@Override
	public void execute(CommandContext c) {
		if (c.params().check(0)) {
			c.requireSysadmin();
			killProcesses(c);

		} else {
			listProcesses(c);
		}
	}

	@SuppressWarnings("null")
	private static void killProcesses(@Nonnull CommandContext c) {
		var missing = stream(c.params().getArray()).mapToInt(ParseUtils::parseInt)
			.sorted()
			.distinct()
			.filter(KillProcessCommand::killProcess)
			.toArray();

		if (missing.length == 0)
			c.react(ACCEPT_EMOJI);
		else
			c.reply(stream(missing).mapToObj(FORMAT_NOT_FOUND::formatted).collect(joining("\n")), DISABLED);
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

		c.replyf(FORMAT_TABLE, t.render());
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

	@Override
	public String getName() {
		return "killprocess";
	}

	@Override
	public String[] getAliases() {
		return array("kill", "ps");
	}

	@Override
	public String getInfo() {
		return """
			Kills a running process.

			If no PID is provided, a list of running processes is shown.
			Process flags:
			- `C`hannel/`G`uild matches
			- `U`ser matches
			- Has custom `D`ata
			- Thread state: `N`ew, `R`unnable, `W`aiting, `B`locked, `T`imed waiting, terminate`D`
			""";
	}

	@Override
	public String[] getParameters() {
		return array("[pid]");
	}

	@Override
	public String[] getParameterInfo() {
		return array("PID of the process to kill");
	}

	@Override
	public int getMinParameters() {
		return 0;
	}

	@Override
	public int getMaxParameters() {
		return 0;
	}

	@Override
	public CommandCategory getCategory() {
		return ADMINISTRATIVE;
	}

}
