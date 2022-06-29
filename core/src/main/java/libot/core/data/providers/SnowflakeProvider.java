package libot.core.data.providers;

import java.util.Map;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public abstract class SnowflakeProvider<V> extends MapProvider<Long, V> {

	protected SnowflakeProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager,
								@Nonnull TypeToken<Map<Long, V>> typeToken, @Nonnull String dataKey) {
		super(shredder, dataManager, typeToken, dataKey);
	}

}
