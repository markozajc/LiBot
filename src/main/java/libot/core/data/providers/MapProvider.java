package libot.core.data.providers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public abstract class MapProvider<K, V> extends Provider<Map<K, V>> {

	protected MapProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager,
						  @Nonnull TypeToken<Map<K, V>> typeToken, @Nonnull String dataKey) {
		super(shredder, dataManager, typeToken, dataKey);
	}

	@Override
	protected Map<K, V> createEmptyData() {
		return new ConcurrentHashMap<>();
	}

	@Override
	protected Map<K, V> constructData(String json) {
		return new ConcurrentHashMap<>(super.constructData(json));
	}

}
