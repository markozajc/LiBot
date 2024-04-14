package libot.commands;

import static java.lang.String.format;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.providers.CustomizationsProvider.Customization;
import net.dv8tion.jda.api.Permission;

public class DisableCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var name = c.params().get(0);
		var cust = c.getGuildCustomization();
		Command cmd;
		CommandCategory category;
		if ((cmd = c.getCommands().get(name)) != null) {
			disableSingle(c, cust, cmd);

		} else if ((category = CommandCategory.getCategory(name)) != null) {
			disableCategory(c, cust, category);

		} else {
			c.replyf("%s is not a command or a category.", FAILURE, name);
		}
	}

	@SuppressWarnings("null")
	private static void disableCategory(@Nonnull CommandContext c, @Nonnull Customization cust,
										@Nonnull CommandCategory category) {
		var disabled = c.getCommands()
			.getInCategory(category)
			.stream()
			.filter(cmd -> !(cmd instanceof EnableCommand))
			.filter(cust::disable)
			.map(Command::getName)
			.toList();
		c.replyf(format("Successfully disabled %d commands", disabled.size()), String.join(", ", disabled), SUCCESS);
	}

	private static void disableSingle(@Nonnull CommandContext c, @Nonnull Customization cust, @Nonnull Command cmd) {
		if (cmd instanceof EnableCommand)
			c.replyf("You may not disable that", DISABLED);
		else if (cust.disable(cmd))
			c.replyf("Successfully disabled `%s`", SUCCESS, cmd.getName());
		else
			c.replyf("`%s` is already disabled", DISABLED, cmd.getName());
	}

	@Override
	public String getName() {
		return "disablecommand";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "disable" };
	}

	@Override
	public String getInfo() {
		return """
			Disables a command or a category. \
			It will not be possible to use it until it is reenabled with the `enable` command.""";
	}

	@Override
	public Permission[] getPermissions() {
		return new Permission[] { MANAGE_SERVER };
	}

	@Override
	public String[] getParameters() {
		return new String[] { "name" };
	}

	@Override
	public String[] getParameterInfo() {
		return new String[] { "name/alias of command or category" };
	}

	@Override
	public CommandCategory getCategory() {
		return CUSTOMIZATION;
	}

}
