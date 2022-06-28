package libot.core.data.providers.impl;

import static java.lang.Math.max;
import static libot.core.processes.ProcessManager.getProcesses;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.commands.bettable.BettableGame.BettableProcessData;
import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;

public class MoneyProvider extends SnowflakeProvider<Long> {

	public static final long DEFAULT_BALANCE = 50L;

	public MoneyProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "money");
	}

	public long getBalance(long userId) {
		return this.data.getOrDefault(userId, DEFAULT_BALANCE);
	}

	public void setBalance(long userId, long balance) {
		this.data.put(userId, max(0, balance));
	}

	public long addMoney(long userId, long amount) {
		long newBalance = getBalance(userId) + amount;
		setBalance(userId, newBalance);
		return newBalance;
	}

	public long takeMoney(long userId, long amount) {
		long newBalance = getBalance(userId) - amount;
		setBalance(userId, newBalance);
		return newBalance;
	}

	@Override
	public void shutdown() {
		super.shutdown();
		getProcesses().stream().filter(p -> p.getData() instanceof BettableProcessData).forEach(p -> {
			var data = (BettableProcessData) p.getData();
			addMoney(p.getUserId(), data.getBet());
			data.markReturned();
		});
	}

}
