package libot.core.data;

import java.io.IOException;

import javax.annotation.*;

public interface DataManager {

	void set(@Nonnull String key, @Nullable String value) throws IOException;

	@Nullable
	String get(@Nonnull String key) throws IOException;

	default String getOrDefault(@Nonnull String key, @Nullable String value) throws IOException {
		var returned = get(key);
		if (returned == null)
			return value;
		return returned;
	}

}
