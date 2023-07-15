package libot.core.data.providers;

import static java.lang.String.format;
import static libot.utils.ReflectionUtils.scanClasspath;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.*;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

public final class ProviderManager {

	private static final Logger LOG = getLogger(ProviderManager.class);

	private final Map<Class<?>, Provider<?>> providers;

	public ProviderManager(@SuppressWarnings("rawtypes") @Nonnull Set<Provider> providers) {
		this.providers = new HashMap<>();
		providers.forEach(p -> this.providers.put(p.getClass(), p));
	}

	@Nonnull
	public static ProviderManager fromClasspath(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		return new ProviderManager(scanClasspath(Provider.class, libot.providers.Anchor.class,
												 new Class<?>[] { Shredder.class, DataManager.class }, shredder,
												 dataManager));
	}

	public void loadAll() {
		this.providers.values().forEach(p -> {
			LOG.trace("Loading {}", p.getClass().getSimpleName());
			p.load();
		});
	}

	public void storeAll() {
		LOG.debug("Storing providers");
		this.providers.values().forEach(Provider::store);
	}

	public void shutdownAll() {
		this.providers.values().forEach(Provider::shutdown);
	}

	public void onShredderReady() {
		this.providers.values().forEach(Provider::onShredderReady);
	}

	@Nonnull
	@SuppressWarnings("unchecked")
	public <T extends Provider<?>> T get(@Nonnull Class<T> clazz) {
		T result = (T) this.providers.get(clazz);
		if (result == null)
			throw new IllegalStateException(format("Provider of class %s is not registered", clazz.getSimpleName()));
		return result;
	}

	public int size() {
		return this.providers.size();
	}

}
