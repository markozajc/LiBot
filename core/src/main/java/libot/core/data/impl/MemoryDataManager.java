package libot.core.data.impl;

import java.io.IOException;
import java.util.Properties;

import libot.core.data.DataManager;

public class MemoryDataManager implements DataManager {

	protected final Properties props;

	public MemoryDataManager() {
		this.props = new Properties(10);
	}

	@Override
	public void set(String key, String value) throws IOException {
		if (value != null)
			this.props.setProperty(key, value);
		else
			this.props.remove(key);
	}

	@Override
	public String get(String key) {
		return this.props.getProperty(key);
	}

}
