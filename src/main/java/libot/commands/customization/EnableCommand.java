package libot.commands.customization;

import static java.lang.String.*;
import static libot.core.Constants.*;
import static libot.core.commands.CommandCategory.CUSTOMIZATION;
import static libot.utils.Utilities.array;
import static net.dv8tion.jda.api.Permission.MANAGE_SERVER;

import javax.annotation.Nonnull;

import libot.core.commands.*;
import libot.core.data.providers.impl.CustomizationsProvider.Customization;
import libot.core.entities.CommandContext;
import net.dv8tion.jda.api.Permission;

public class EnableCommand extends Command {

	@Override
	public void execute(CommandContext c) {
		var name = c.params().get(0);
		var cust = c.getGuildCustomization();
		Command cmd;
		CommandCategory category;
		if ((cmd = c.getCommands().get(name)) != null) {
			enableSingle(c, cust, cmd);

		} else if ((category = CommandCategory.getCategory(name)) != null) {
			enableCategory(c, cust, category);

		} else {
			c.replyf("%s is not a command or a category.", FAILURE, name);
		}
	}

	@SuppressWarnings("null")
	private static void enableCategory(@Nonnull CommandContext c, @Nonnull Customization cust,
									   @Nonnull CommandCategory category) {
		var enabled =
			c.getCommands().getInCategory(category).stream().filter(cust::enable).map(Command::getName).toList();
		c.replyf(format("Successfully enabled %d commands", enabled.size()), join(", ", enabled), SUCCESS);
	}

	private static void enableSingle(@Nonnull CommandContext c, @Nonnull Customization cust, @Nonnull Command cmd) {
		if (cust.enable(cmd))
			c.replyf("Successfully enabled `%s`", SUCCESS, cmd.getName());
		else
			c.replyf("`%s` is already enabled", DISABLED, cmd.getName());
	}

	@Override
	public String getName() {
		return "enablecommand";
	}

	@Override
	public String[] getAliases() {
		return array("enable");
	}

	@Override
	public String getInfo() {
		return "Enables a previously-disabled command or a category.";
	}

	@Override
	public Permission[] getPermissions() {
		return array(MANAGE_SERVER);
	}

	@Override
	public String[] getParameters() {
		return array("name");
	}

	@Override
	public String[] getParameterInfo() {
		return array("name/alias of command or category");
	}

	@Override
	public CommandCategory getCategory() {
		return CUSTOMIZATION;
	}

}
