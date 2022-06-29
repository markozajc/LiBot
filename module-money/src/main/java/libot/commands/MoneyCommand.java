package libot.commands;

import static java.lang.Math.*;
import static java.lang.System.currentTimeMillis;
import static java.util.concurrent.TimeUnit.HOURS;
import static libot.core.Constants.LITHIUM;
import static libot.core.commands.CommandCategory.MONEY;
import static libot.utils.Utilities.array;

import libot.core.commands.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import libot.core.ratelimits.Ratelimits;
import libot.providers.MoneyProvider;
import net.dv8tion.jda.api.utils.TimeFormat;

public class MoneyCommand extends Command {

	private static final String FORMAT_CURRENT_BALANCE = """
		Your current balance is: **%dŁ**""";
	private static final String FORMAT_FOOTER = """

		You can claim your next hourly reward %s.""";

	private static final long REWARD_SPACING = HOURS.toMillis(1);
	private static final Ratelimits REWARD_RATELIMIT = new Ratelimits(REWARD_SPACING);
	private static final int MIN_REWARD = 5;

	@Override
	public void execute(CommandContext c) {
		var provider = c.provider(MoneyProvider.class);
		long balance = provider.getBalance(c.getUserIdLong());

		var b = new EmbedPrebuilder(LITHIUM);
		b.setDescriptionf(FORMAT_CURRENT_BALANCE, balance);

		long time = REWARD_RATELIMIT.check(c.getUserIdLong());
		if (time == -1) {
			long earned = computeReward(balance);
			provider.addMoney(c.getUserIdLong(), earned);
			REWARD_RATELIMIT.register(c.getUserIdLong());
			b.appendDescriptionf(" + %d (hourly reward)", earned);
			time = REWARD_SPACING;
		}

		b.appendDescriptionf(FORMAT_FOOTER, TimeFormat.RELATIVE.format(currentTimeMillis() + time));
		c.reply(b);
	}

	private static long computeReward(long balance) {
		return max(round(pow(log(balance), 2) * 2), MIN_REWARD);
	}

	@Override
	public String getName() {
		return "money";
	}

	@Override
	public String[] getAliases() {
		return array("balance", "work", "reward");
	}

	@Override
	public String getInfo() {
		return "Displays your current balance and claims your hourly reward.";
	}

	@Override
	public CommandCategory getCategory() {
		return MONEY;
	}

}
