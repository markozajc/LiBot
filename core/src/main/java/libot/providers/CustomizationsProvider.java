package libot.providers;

import static net.dv8tion.jda.api.Permission.*;

import java.util.*;

import javax.annotation.*;

import com.google.gson.reflect.TypeToken;

import libot.core.commands.Command;
import libot.core.data.DataManager;
import libot.core.data.providers.SnowflakeProvider;
import libot.core.entities.CommandContext;
import libot.core.shred.Shredder;
import libot.providers.CustomizationsProvider.Customization;
import net.dv8tion.jda.api.entities.*;

public class CustomizationsProvider extends SnowflakeProvider<Customization> {

	public static class Customization {

		private List<Integer> disabledCommands;
		private String commandPrefix;
		private long djRoleId;

		public Customization() {
			this.disabledCommands = new ArrayList<>(5);
			this.djRoleId = -1;
		}

		public boolean disable(@Nonnull Command command) {
			if (isDisabled(command))
				return false;

			this.disabledCommands.add(command.getId());
			return true;
		}

		public boolean enable(@Nonnull Command command) {
			if (!isDisabled(command))
				return false;

			this.disabledCommands.remove((Integer) command.getId());
			return true;
		}

		public boolean isDisabled(@Nonnull Command command) {
			return this.disabledCommands.contains(command.getId());
		}

		@Nonnull
		@SuppressWarnings("null")
		public Optional<String> getCustomPrefix() {
			return Optional.ofNullable(this.commandPrefix);
		}

		@Nonnull
		public Customization setCommandPrefix(@Nullable String commandPrefix) {
			this.commandPrefix = commandPrefix;
			return this;
		}

		@Nonnull
		@SuppressWarnings("null")
		public OptionalLong getDjRoleId() {
			if (this.djRoleId == -1)
				return OptionalLong.empty();
			else
				return OptionalLong.of(this.djRoleId);
		}

		public boolean isDj(@Nonnull Member member) {
			var id = this.getDjRoleId();
			if (id.isPresent()) {
				return member.hasPermission(MANAGE_SERVER, VOICE_CONNECT, VOICE_SPEAK)
					|| member.getRoles().stream().anyMatch(r -> r.getIdLong() == id.getAsLong());
			} else {
				return true;
			}
		}

		public void setDjRole(@Nullable Role djRole) {
			if (djRole == null)
				this.djRoleId = -1;
			else
				this.djRoleId = djRole.getIdLong();
		}

	}

	public CustomizationsProvider(@Nonnull Shredder shredder, @Nonnull DataManager dataManager) {
		super(shredder, dataManager, new TypeToken<>() {}, "custconfig");
	}

	@Nonnull
	@SuppressWarnings("null")
	public Customization get(long guildId) {
		return this.data.computeIfAbsent(guildId, i -> new Customization());
	}

	@Nonnull
	public Customization get(CommandContext c) {
		return get(c.getGuildIdLong());
	}

}
