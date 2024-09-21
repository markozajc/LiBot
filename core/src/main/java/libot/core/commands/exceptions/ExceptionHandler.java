package libot.core.commands.exceptions;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.ratelimits.CommandRatelimitManager.getRatelimits;
import static net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.exception.ExceptionUtils.*;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.function.*;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.argument.UsageException;
import libot.core.commands.Command;
import libot.core.commands.exceptions.runtime.*;
import libot.core.commands.exceptions.startup.*;
import libot.core.entities.*;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.*;

public class ExceptionHandler {

	private static final Logger LOG = getLogger(ExceptionHandler.class);

	public static void handle(@Nonnull Throwable throwable, @Nonnull CommandContext ctx) {
		handle(throwable, ctx.getCommand(), ctx);
	}

	public static void handle(@Nonnull Throwable throwable, @Nonnull Command command, @Nonnull EventContext ctx) {
		var unpacked = unpackThrowable(throwable);
		try {
			var e = new ExceptionContext<>(throwable, command, ctx);
			boolean handled = ThrowableHandler.handleThrowable(e);
			if (!handled) {
				reportException(e);

				if (ctx.canTalk()) {
					ctx.replyf("// FAILURE //", """
						LiBot ran into an unknown error. If the issue persists, please report it via `%sfeedback`.""",
							   FAILURE, ctx.getEffectivePrefix());
				}
			} else {
				LOG.debug("Automatically handled an exception in {}", command.getName());
			}

			if (command.getRatelimit() != 0 && shouldRatelimit(unpacked))
				getRatelimits(command).register(ctx.getUserIdLong());

		} catch (Exception e) {
			LOG.error("Got an exception while handling an error", e);
		}
	}

	private static final Throwable unpackThrowable(Throwable t) {
		Throwable unpacked = getRootCause(t);
		if (unpacked == null)
			return t;

		return unpacked;
	}

	private static record ExceptionContext<T extends Throwable>(@Nonnull T ex, @Nonnull Command command,
																@Nonnull EventContext ctx) {

		@Nonnull
		public String commandName() {
			return this.command.getName();
		}

		public boolean canTalk() {
			return this.ctx.canTalk();
		}

		public <E extends T> boolean runSpecialized(@Nonnull E specialized,
													@Nonnull Predicate<ExceptionContext<E>> action) {
			return action.test(specialize(specialized));
		}

		public <E extends T> boolean runSpecialized(@Nonnull E specialized,
													@Nonnull Consumer<ExceptionContext<E>> action) {
			action.accept(specialize(specialized));
			return true;
		}

		@Nonnull
		@SuppressWarnings("null")
		private <E extends T> ExceptionContext<E> specialize(@Nonnull E specialized) {
			if (specialized != this.ex)
				throw new IllegalArgumentException("Can only specialize for the same throwable object");

			return new ExceptionContext<>(specialized, this.command, this.ctx);
		}

	}

	@SuppressWarnings("null")
	private static void reportException(@Nonnull ExceptionContext<Throwable> e) {
		int maxTraceLength = Message.MAX_CONTENT_LENGTH - e.commandName().length() - 26;
		var report = """
			Failed to execute %s:```
			%s```""".formatted(e.commandName(), abbreviate(getStackTrace(e.ex()), maxTraceLength));

		e.ctx().messageSysadmins(report);

		if (LOG.isErrorEnabled()) {
			LOG.error("Unhandled exception in {}", e.commandName());
			LOG.error("", e.ex());
		}
	}

	private static class ThrowableHandler {

		private ThrowableHandler() {}

		public static boolean handleThrowable(ExceptionContext<Throwable> e) {
			if (e.ex() instanceof Error s)
				return e.runSpecialized(s, ErrorHandler::handleError);

			else if (e.ex() instanceof Exception s)
				return e.runSpecialized(s, PlainHandler::handlePlain);

			else
				return false;
		}

		private static class ErrorHandler {

			public static boolean handleError(ExceptionContext<Error> e) {
				if (e.ex() instanceof VirtualMachineError s)
					return e.runSpecialized(s, VirtualMachineErrorHandler::handleVirtualMachineError);

				return false;
			}

			private static class VirtualMachineErrorHandler {

				public static boolean handleVirtualMachineError(ExceptionContext<VirtualMachineError> e) {
					if (e.ex() instanceof OutOfMemoryError s)
						return e.runSpecialized(s, VirtualMachineErrorHandler::handleOutOfMemoryError);

					else
						return false;
				}

				private static void handleOutOfMemoryError(ExceptionContext<OutOfMemoryError> e) {
					if (e.canTalk()) {
						e.ctx().reply("// MEM LOW //", """
							LiBot was unable to launch this command because it ran out of memory.""", DISABLED);
					}
				}

			}

		}

		private static class PlainHandler {

			public static boolean handlePlain(ExceptionContext<Exception> e) {
				if (e.ex() instanceof InterruptedException s)
					return e.runSpecialized(s, PlainHandler::handleInterrupted);

				else if (e.ex() instanceof RuntimeException s)
					return e.runSpecialized(s, RuntimeHandler::handleRuntime);

				else
					return false;
			}

			private static void handleInterrupted(ExceptionContext<InterruptedException> e) {
				if (e.canTalk())
					e.ctx().replyf("%s has been killed.", DISABLED, capitalize(e.commandName()));
			}

			private static class RuntimeHandler {

				public static boolean handleRuntime(ExceptionContext<RuntimeException> e) {
					if (e.ex() instanceof UsageException s)
						return e.runSpecialized(s, RuntimeHandler::handleArgumentParse);

					else if (e.ex() instanceof CommandException s)
						return e.runSpecialized(s, CommandExceptionHandler::handleCommand);

					else if (e.ex() instanceof ErrorResponseException s)
						return e.runSpecialized(s, RuntimeHandler::handleErrorResponse);

					else if (e.ex() instanceof IllegalArgumentException s)
						return e.runSpecialized(s, IllegalArgumentHandler::handleIllegalArgument);

					else if (e.ex() instanceof PermissionException s)
						return e.runSpecialized(s, PermissionExceptionHandler::handlePermission);

					else
						return false;
				}

				@SuppressWarnings("null")
				private static void handleArgumentParse(ExceptionContext<UsageException> e) {
					if (e.canTalk()) {
						e.ctx().replyf("// USAGE INCORRECT //", """
							%s
							**Correct usage:** %s""", WARN, e.ex().getMessage(), e.command().getUsage(e.ctx()));
					}
				}

				private static class CommandExceptionHandler {

					public static void handleCommand(ExceptionContext<CommandException> e) {
						boolean handled = false;
						if (e.ex() instanceof CanceledException)
							handled = true;

						else if (e.ex() instanceof NumberOverflowException s)
							handled = e.runSpecialized(s, CommandExceptionHandler::handleNumberOverflow);

						else if (e.ex() instanceof CommandStartupException s)
							handled = e.runSpecialized(s, CommandStartupExceptionHandler::handleCommandStartup);

						else if (e.ex() instanceof TimeoutException s)
							handled = e.runSpecialized(s, CommandExceptionHandler::handleTimeout);

						else if (e.ex() instanceof TimeParseException s)
							handled = e.runSpecialized(s, CommandExceptionHandler::handleTimeParse);

						if (!handled)
							e.ex().sendMessage(e.ctx().getChannel());
					}

					private static void handleNumberOverflow(ExceptionContext<NumberOverflowException> e) {
						if (e.canTalk()) {
							e.ctx().reply("// INTEGER OVERFLOW //", """
								Looks like you provided a pretty large number. Don't do that.""", WARN);
						}
					}

					private static class CommandStartupExceptionHandler {

						public static boolean handleCommandStartup(ExceptionContext<CommandStartupException> e) {
							if (e.ex() instanceof CommandDisabledException s)
								return e.runSpecialized(s, CommandStartupExceptionHandler::handleCommandDisabled);

							else if (e.ex() instanceof CommandPermissionsException s)
								return e.runSpecialized(s, CommandStartupExceptionHandler::handleCommandPermissions);

							else if (e.ex() instanceof NotDjException s)
								return e.runSpecialized(s, CommandStartupExceptionHandler::handleNotDj);

							else if (e.ex() instanceof NotSysadminException s)
								return e.runSpecialized(s, CommandStartupExceptionHandler::handleNotSysadmin);

							else if (e.ex() instanceof RatelimitedException s)
								return e.runSpecialized(s, CommandStartupExceptionHandler::handleRatelimited);

							else
								return false;
						}

						private static void handleCommandDisabled(ExceptionContext<CommandDisabledException> e) {
							if (e.canTalk()) {
								var b = new EmbedPrebuilder(DISABLED);
								b.setTitle("// DISABLED //");
								if (e.ex().isGlobal()) {
									b.setDescriptionf("""
										%s has been globally disabled by LiBot developers. Please try again later""",
													  e.commandName());

								} else {
									b.setDescriptionf("%s has been disabled by this guild's moderators.",
													  e.commandName());
								}
								e.ctx().reply(b);
							}
						}

						private static void handleCommandPermissions(ExceptionContext<CommandPermissionsException> e) {
							var missing = e.ex().getPermissions().stream().map(Permission::getName).toList();

							if (e.canTalk()) {
								e.ctx()
									.replyf("// ACCESS DENIED //", """
										This action needs the following permission%s:
										%s""", WARN, missing.size() == 1 ? "" : "s",
											missing.stream().collect(joining("\n", "- ", "")));
							}
						}

						private static void handleNotDj(ExceptionContext<NotDjException> e) {
							if (e.canTalk()) {
								e.ctx()
									.replyf("// DJ-ONLY //",
											"Only members of the DJ role (<@&%d>) can perform this action.", WARN,
											e.ex().getDjRoleId());
							}
						}

						private static void handleNotSysadmin(ExceptionContext<NotSysadminException> e) {
							if (e.canTalk()) {
								e.ctx()
									.replyf("// AUTHENTICATION REQUIRED //", """
										This command is reserved for LiBot administrators and is probably not what you \
										were looking for. To see the full list of commands, run `%shelp`""", DISABLED,
											e.ctx().getEffectivePrefix());
							}
						}

						private static void handleRatelimited(ExceptionContext<RatelimitedException> e) {
							if (e.canTalk()) {
								long seconds = e.ex().getRemaining() / 1000;
								var time = new StringBuilder();
								if (seconds == 0) {
									time.append("less than a second");
								} else {
									time.append(seconds);
									time.append(" second");
									if (seconds != 1)
										time.append("s");
								}
								e.ctx().replyf("// RATELIMITED //", """
									Not so fast! Please wait %s before running %s.""", DISABLED, time, e.commandName());
							}
						}

					}

					private static void handleTimeout(ExceptionContext<TimeoutException> e) {
						if (e.canTalk())
							e.ctx().reply("Response time has run out.", DISABLED);
					}

					private static void handleTimeParse(ExceptionContext<TimeParseException> e) {
						if (e.canTalk()) {
							e.ctx().replyf("// TIME EXCEPTED //", """
								LiBot was expecting a \
								[timestamp](https://libot.eu.org/doc/commands/parameter-types.html#time), but got \
								something else.""");
						}
					}

				}

				private static void handleErrorResponse(ExceptionContext<ErrorResponseException> e) {
					if (e.canTalk()) {
						e.ctx().replyf("// DISCORD FAILED US //", """
							Looks like Discord didn't like that for some reason.
							Error: %s""", FAILURE, e.ex().getMeaning());
					}
				}

				private static class IllegalArgumentHandler {

					public static boolean handleIllegalArgument(ExceptionContext<IllegalArgumentException> e) {
						if (e.ex() instanceof NumberFormatException s)
							return e.runSpecialized(s, IllegalArgumentHandler::handleNumberFormat);

						else
							return false;
					}

					private static void handleNumberFormat(ExceptionContext<NumberFormatException> e) {
						if (e.canTalk()) {
							e.ctx().reply("// NOT AN INTEGER //", """
								LiBot was expecting an integer input, but got text.""", WARN);
						}
					}
				}

				private static class PermissionExceptionHandler {

					public static boolean handlePermission(ExceptionContext<PermissionException> e) {
						if (e.ex() instanceof HierarchyException s)
							return e.runSpecialized(s, PermissionExceptionHandler::handleHierarchy);

						else if (e.ex() instanceof InsufficientPermissionException s)
							return e.runSpecialized(s, PermissionExceptionHandler::handleInsufficientPermission);

						else
							return false;
					}

					private static void handleInsufficientPermission(ExceptionContext<InsufficientPermissionException> e) {
						if (e.ex().getPermission() == MESSAGE_EMBED_LINKS) {
							e.ctx().reply("""
								```
								// EMBED REQUIRED //
								| You must grant LiBot the 'Embed Links' permission to perform this action.
								```""");

						} else if (e.canTalk()) {
							e.ctx()
								.replyf("// ACCESS DENIED //", """
									You must grant LiBot the '%s' permission to perform this action.""", WARN,
										e.ex().getPermission().getName());
						}
					}

					private static void handleHierarchy(ExceptionContext<HierarchyException> e) {
						if (e.canTalk()) {
							e.ctx().reply("// HIERARCHY ERROR //", """
								Looks like you tried to perform an audit action on a user that is in a role higher \
								than LiBot, which you can't. Please move LiBot's role up or demote that user!""", WARN);
						}
					}

				}
			}
		}

	}

	private static boolean shouldRatelimit(Throwable t) {
		if (t instanceof CommandException ce)
			return ce.doesRegisterRatelimit();
		return true;
	}

	private ExceptionHandler() {}

}
