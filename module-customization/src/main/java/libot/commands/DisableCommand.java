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

public class DisableCommand extends Command {

	private static final MandatoryParameter NAME = mandatory(POSITIONAL, "name", "command or category to disable");

	public DisableCommand() {
		super(CommandMetadata.builder(CUSTOMIZATION, "disable")
			.aliases("disablecommand")
			.permissions(MANAGE_SERVER)
			.parameters(NAME)
			.description("""
				Disables a command or category. \
				It will not be possible to use it until it is reenabled with the `enable` command."""));
	}

	@Override
	@SuppressWarnings("null")
	public void execute(CommandContext c) {
		var name = c.arg(NAME).value();
		var cust = c.getGuildCustomization();
		c.getCommands().get(name).ifPresentOrElse(cmd -> {
			disableSingle(c, cust, cmd);

		}, () -> {
			CommandCategory.getCategory(name).ifPresentOrElse(cat -> {
				disableCategory(c, cust, cat);

			}, () -> {
				throw c.errorf("%s is not a command or a category.", FAILURE, name);
			});
		});
	}

	@SuppressWarnings("null")
	private static void disableCategory(@Nonnull CommandContext c, @Nonnull Customization cust,
										@Nonnull CommandCategory category) {
		var disabled = c.getCommands()
			.commands()
			.filter(cmd -> category == cmd.getCategory())
			.filter(cmd -> !(cmd instanceof EnableCommand))
			.filter(cust::disable)
			.map(Command::getName)
			.toList();

		c.reply("Successfully disabled %d commands".formatted(disabled.size()), join(", ", disabled), SUCCESS);
	}

	private static void disableSingle(@Nonnull CommandContext c, @Nonnull Customization cust, @Nonnull Command cmd) {
		if (cmd instanceof EnableCommand)
			throw c.errorf("You may not disable that", DISABLED);

		if (!cust.disable(cmd))
			throw c.errorf("`%s` is already disabled", DISABLED, cmd.getName());

		c.replyf("Successfully disabled `%s`", SUCCESS, cmd.getName());
	}

}
