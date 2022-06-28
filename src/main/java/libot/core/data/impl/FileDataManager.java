package libot.core.data.impl;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.*;
import static java.nio.file.StandardOpenOption.*;
import static libot.core.Constants.ENV_DATA_PATH;
import static libot.utils.Utilities.getenvOrThrow;

import java.io.IOException;
import java.nio.file.Path;

import javax.annotation.Nonnull;

import libot.core.data.DataManager;

public class FileDataManager implements DataManager {

	@Nonnull
	private final Path root;

	@SuppressWarnings("null")
	public FileDataManager() throws IOException {
		this.root = Path.of(getenvOrThrow(ENV_DATA_PATH));
		if (!isDirectory(this.root))
			throw new IOException(this.root.toString() + " does not exist or is not a directory.");
		if (!isReadable(this.root) || !isWritable(this.root))
			throw new IOException("Permission denied: " + this.root);
	}

	@Override
	public void set(String key, String value) throws IOException {
		var file = this.root.resolve(key);
		if (value == null)
			delete(file);
		else
			write(file, value.getBytes(UTF_8), TRUNCATE_EXISTING, CREATE, WRITE);
	}

	@Override
	public String get(String key) throws IOException {
		var file = this.root.resolve(key);
		if (exists(file))
			return new String(readAllBytes(file), UTF_8);
		else
			return null;
	}
}
