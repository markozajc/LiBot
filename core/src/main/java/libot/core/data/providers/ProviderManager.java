package libot.core.data.providers;

import static com.google.common.base.Functions.identity;
import static java.lang.String.format;
import static java.util.Collections.unmodifiableMap;
import static libot.utils.ReflectionUtils.scanClasspath;
import static libot.utils.Utilities.toModifiableMap;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Map;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.data.DataManager;
import libot.core.shred.Shredder;

@SuppressWarnings("rawtypes") // ecj doesn't complain if I use Provider<?> but javac does, so I'm opting to use raw
							  // types instead
public final class ProviderManager {

	private static final Logger LOG = getLogger(ProviderManager.class);

	@Nonnull private final Map<Class<?>, Provider> providers;

	private ProviderManager(@Nonnull Map<Class<?>, Provider> providers) {
		this.providers = providers;
	}

	@Nonnull
	@SuppressWarnings("null")
	public static ProviderManager fromClasspath(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		return new ProviderManager(scanClasspath(Provider.class, libot.providers.Anchor.class,
												 new Class<?>[] { Shredder.class, DataManager.class }, shredder,
												 dataManager).collect(toModifiableMap(Object::getClass, identity())));
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
	public <T extends Provider<?>> T get(@Nonnull Class<T> clazz) {
		T result = (T) this.providers.get(clazz);
		if (result == null)
			throw new IllegalStateException(format("Provider of class %s is not registered", clazz.getSimpleName()));
		return result;
	}

	@Nonnull
	@SuppressWarnings("null")
	public Map<Class<?>, Provider> getAll() {
		return unmodifiableMap(this.providers);
	}

	@Nonnull
	public Map<Class<?>, Provider> getAllDirect() {
		return this.providers;
	}

	public int size() {
		return this.providers.size();
	}

}
