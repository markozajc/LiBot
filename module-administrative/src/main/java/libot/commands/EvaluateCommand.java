package libot.commands;

import static javax.script.ScriptContext.ENGINE_SCOPE;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
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
import libot.core.argument.ParameterList.Parameter;
import libot.core.commands.*;
import libot.core.entities.*;
import libot.core.extensions.EmbedPrebuilder;

public class EvaluateCommand extends Command {

	private static final Parameter SCRIPT = mandatory(POSITIONAL, "script");

	public EvaluateCommand() {
		super(CommandMetadata.builder(ADMINISTRATIVE, "evaluate")
			.aliases("eval")
			.parameters(SCRIPT)
			.description("Runs a Groovy script.")
			.build());
	}

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

		var result = ENGINE.eval(IMPORTS + c.arg(SCRIPT));
		return new EvalResult(stdout.toString(), stderr.toString(), result);
	}

	@Override
	public void startupCheck(EventContext ec) {
		super.startupCheck(ec);
		ec.requireSysadmin();
	}

	public record EvalResult(@Nonnull String stdout, @Nonnull String stderr, @Nullable Object returned) {}

}
