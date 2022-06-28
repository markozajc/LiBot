package libot.core.entities;

import javax.annotation.Nonnull;

import libot.core.commands.exceptions.startup.UsageException;
import libot.utils.ParseUtils;

public class Parameters {

	@Nonnull
	private final String[] parameters;

	public Parameters(@Nonnull String[] parameters) {
		this.parameters = parameters;
	}

	@Nonnull
	@SuppressWarnings("null")
	public String get(int index) {
		if (size() <= index)
			throw new UsageException();
		return this.parameters[index];
	}

	public String getOrDefault(int index, String value) {
		if (check(index))
			return this.parameters[index];
		else
			return value;
	}

	public int getInt(int index) {
		return ParseUtils.parseInt(this.get(index));
	}

	public int getIntOrDefault(int index, int value) {
		if (check(index))
			return getInt(index);
		else
			return value;
	}

	public long getLong(int index) {
		return ParseUtils.parseLong(this.get(index));
	}

	public boolean check(int min) {
		return min < size();
	}

	public int size() {
		return this.parameters.length;
	}

	@Nonnull
	public String[] getArray() {
		return this.parameters;
	}

}
