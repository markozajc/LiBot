package libot.providers;

import static java.util.Collections.synchronizedSet;

import java.util.*;

import javax.annotation.Nonnull;

import com.google.gson.reflect.TypeToken;

import libot.core.commands.Command;
import libot.core.data.DataManager;
import libot.core.data.providers.Provider;
import libot.core.shred.Shredder;
import libot.providers.ConfigurationProvider.BotConfiguration;

public class ConfigurationProvider extends Provider<BotConfiguration> {

	public static class BotConfiguration {

		private final Set<String> disabledCommands;

		public BotConfiguration() {
			this(new HashSet<>());
		}

		public BotConfiguration(Set<String> disabledCommands) {
			this.disabledCommands = synchronizedSet(disabledCommands);
		}

		public Set<String> getDisabledCommands() {
			return this.disabledCommands;
		}

	}

	public ConfigurationProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "configuration");
	}

	public boolean isDisabled(Command command) {
		return this.data.getDisabledCommands().contains(command.getId());
	}

	public boolean disable(Command command) {
		if (isDisabled(command))
			return false;
		this.data.getDisabledCommands().add(command.getId());
		return true;
	}

	public boolean enable(Command command) {
		if (!isDisabled(command))
			return false;
		this.data.getDisabledCommands().remove(command.getId());
		return true;
	}

	@Override
	protected BotConfiguration createEmptyData() {
		return new BotConfiguration();
	}

}
