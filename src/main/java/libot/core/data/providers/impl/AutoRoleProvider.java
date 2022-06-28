package libot.core.data.providers.impl;

import java.util.OptionalLong;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.shred.Shredder;

public class AutoRoleProvider extends SnowflakeProvider<Long> {

	public AutoRoleProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "autorole");
	}

	public void set(long guildId, long roleId) {
		this.data.put(guildId, roleId);
	}

	public void remove(long guildId) {
		this.data.remove(guildId);
	}

	@Nonnull
	@SuppressWarnings("null")
	public OptionalLong get(long guildId) {
		var value = this.data.get(guildId);
		if (value != null)
			return OptionalLong.of(value);
		else
			return OptionalLong.empty();
	}

}
