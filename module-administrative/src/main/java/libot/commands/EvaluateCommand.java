package libot.commands;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.ADMINISTRATIVE;
import static libot.utils.ResourceUtils.resourceAsString;
import static net.dv8tion.jda.api.utils.MarkdownUtil.codeblock;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.*;

import javax.annotation.*;
import javax.script.*;

import org.codehaus.groovy.jsr223.GroovyScriptEngineImpl;
import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;

public class EvaluateCommand extends Command {

	public record EvalResult(@Nonnull String stdout, @Nonnull String stderr, @Nullable Object returned) {}

	private static final Logger LOG = getLogger(EvaluateCommand.class);

	private static final ScriptEngine ENGINE = new GroovyScriptEngineImpl();
	private static final String IMPORTS;
	static {
		var imports = new StringBuilder();
		try {
			imports.append(resourceAsString("imports-static"));
			imports.append(resourceAsString("imports-dynamic"));
		} catch (IOException e) {
			LOG.warn("Could not load default imports", e);
		}
		IMPORTS = imports.toString();
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		c.typing();
		try {
			var result = eval(c);

			var b = new StringBuilder();
			if (!result.stdout().isEmpty()) {
				b.append("**stdout**");
				b.append(codeblock(result.stdout()));
			}

			if (!result.stderr().isEmpty()) {
				b.append("**stderr**");
				b.append(codeblock(result.stderr()));
			}

			if (result.returned() != null && !result.returned().toString().isBlank()) {
				b.append("**return**");
				b.append(codeblock(result.returned().toString()));
			}

			if (b.isEmpty())
				c.react(ACCEPT_EMOJI);
			else
				c.reply(b.toString());

		} catch (ScriptException ex) {
			var e = new EmbedPrebuilder(FAILURE);
			e.setTitle("Failure");
			e.setDescription(codeblock(ex.getCause().toString()));
			c.reply(e);
		}
	}

	@SuppressWarnings("null")
	@SuppressFBWarnings(value = "SCRIPT_ENGINE_INJECTION", justification = "access is restricted")
	public static synchronized EvalResult eval(@Nonnull CommandContext c) throws ScriptException {
		var stdout = new StringWriter();
		var stderr = new StringWriter();
		ENGINE.getContext().setWriter(new PrintWriter(stdout));
		ENGINE.getContext().setErrorWriter(new PrintWriter(stderr));

		var bindings = new SimpleBindings();
		bindings.put("c", c);
		ENGINE.setBindings(bindings, ENGINE_SCOPE);

		var result = ENGINE.eval(IMPORTS + c.params().get(0));
		return new EvalResult(stdout.toString(), stderr.toString(), result);
	}

	@Override
	public String getName() {
		return "evaluate";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "eval" };
	}

	@Override
	public String getInfo() {
		return "Runs a Groovy script.";
	}

	@Override
	public String[] getParameters() {
		return new String[] { "script" };
	}

	@Override
	public void startupCheck(CommandContext c) {
		super.startupCheck(c);
		c.requireSysadmin();
	}

	@Override
	public CommandCategory getCategory() {
		return ADMINISTRATIVE;
	}

}
