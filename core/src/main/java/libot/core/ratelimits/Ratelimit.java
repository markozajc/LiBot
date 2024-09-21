package libot.core.ratelimits;

import static java.lang.System.currentTimeMillis;

import org.eclipse.collections.api.factory.primitive.LongLongMaps;
import org.eclipse.collections.api.map.primitive.MutableLongLongMap;

public class Ratelimit {

	private final long delay;
	private final MutableLongLongMap registered;

	public Ratelimit(long millis) {
		this.registered = LongLongMaps.mutable.empty();
		this.delay = millis;
	}

	public void register(long id) {
		this.registered.put(id, currentTimeMillis());
	}

	public long check(long id) {
		if (!this.registered.containsKey(id))
			return -1;

		long remaining = this.registered.get(id) + this.delay - currentTimeMillis();
		return remaining > 0 ? remaining : -1;
	}

}
