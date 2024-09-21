package libot.commands;

import static java.lang.String.join;
import static libot.core.Constants.*;
import static libot.core.argument.ParameterList.Parameter.mandatory;
import static libot.core.argument.ParameterList.Parameter.ParameterType.POSITIONAL;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

import javax.annotation.Nonnull;

import libot.core.argument.ParameterList.MandatoryParameter;
import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider.Customization;

public class EnableCommand extends Command {

	private static final MandatoryParameter NAME = mandatory(POSITIONAL, "name", "command or category to disable");

	public EnableCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "disable")
			.aliases("disablecommand")
			.permissions(MANAGE_SERVER)
			.parameters(NAME)
			.description("""
				Enables a previously disabled command or category."""));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var name = c.arg(NAME).value();
		var cust = c.getGuildCustomization();
		c.getCommands().get(name).ifPresentOrElse(cmd -> {
			enableSingle(c, cust, cmd);

		}, () -> {
			CommandCategory.getCategory(name).ifPresentOrElse(cat -> {
				enableCategory(c, cust, cat);

			}, () -> {
				throw c.errorf("%s is not a command or a category.", FAILURE, name);
			});
		});
	}

	@SuppressWarnings("null")
	private static void enableCategory(@Nonnull CommandContext c, @Nonnull Customization cust,
									   @Nonnull CommandCategory category) {
		var enabled =
			c.getCommands().getInCategory(category).stream().filter(cust::enable).map(Command::getName).toList();

		c.replyf("Successfully enabled %d commands".formatted(enabled.size()), join(", ", enabled), SUCCESS);
	}

	private static void enableSingle(@Nonnull CommandContext c, @Nonnull Customization cust, @Nonnull Command cmd) {
		if (!cust.enable(cmd))
			throw c.errorf("`%s` is already enabled", DISABLED, cmd.getName());

		c.replyf("Successfully enabled `%s`", SUCCESS, cmd.getName());
	}

}
