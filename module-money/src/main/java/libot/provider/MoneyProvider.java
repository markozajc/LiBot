//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.provider;

import static java.lang.Math.max;
import static libot.core.process.ProcessManager.getProcesses;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.provider.SnowflakeProvider;
import libot.core.shred.Shredder;
import libot.module.money.BettableGame.BettableProcessData;

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
			data.markBetRefunded();
		});
	}

}
