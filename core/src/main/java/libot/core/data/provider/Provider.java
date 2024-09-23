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
package libot.core.data.provider;

import static libot.core.Constants.GSON;
import static org.slf4j.LoggerFactory.getLogger;

import java.lang.reflect.Type;

import javax.annotation.*;

import org.slf4j.Logger;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public abstract class Provider<T> {

	private static final Logger LOG = getLogger(Provider.class);

	protected T data;
	@Nonnull private final Type type;
	@Nonnull private final DataManager dataManager;
	@Nonnull private final Shredder shredder;
	@Nonnull private final String dataKey;

	@SuppressWarnings("null")
	protected Provider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager, @Nonnull TypeToken<T> typeToken,
					   @Nonnull String dataKey) {
		this.type = typeToken.getType();
		this.shredder = shredder;
		this.dataManager = dataManager;
		this.dataKey = dataKey;
	}

	public T getData() {
		return this.data;
	}

	protected void onDataLoaded() {}

	protected void onShredderReady() {}

	protected abstract T createEmptyData();

	@Nullable
	protected T constructData(@Nonnull String json) {
		return GSON.<T>fromJson(json, this.type);
	}

	@Nonnull
	@SuppressWarnings("null")
	protected String constructJson() {
		return GSON.toJson(this.data);
	}

	public void shutdown() {
		store();
	}

	protected void onStoreFail(Exception e) {
		if (LOG.isErrorEnabled())
			LOG.error("Failed to store {}: {}", this.getClass().getSimpleName(), e.getMessage());
		LOG.debug("", e);
	}

	protected void onLoadFail(Exception e) {
		if (LOG.isErrorEnabled())
			LOG.error("Failed to load {}: {}", this.getClass().getSimpleName(), e.toString());
		LOG.debug("", e);
	}

	@Nonnull
	public final DataManager getDataManager() {
		return this.dataManager;
	}

	@Nonnull
	public final Shredder getShredder() {
		return this.shredder;
	}

	public final void load() {
		try {
			var json = getDataManager().get(this.dataKey);
			if (json != null && !json.equals("null") && (this.data = constructData(json)) != null)
				onDataLoaded();
			else
				this.data = createEmptyData();
		} catch (Exception e) {
			onLoadFail(e);
		}

	}

	public final void store() {
		try {
			getDataManager().set(this.dataKey, constructJson());
		} catch (Exception e) {
			onStoreFail(e);
		}
	}

}
