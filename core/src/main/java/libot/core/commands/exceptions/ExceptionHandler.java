package libot.core.commands.exceptions;

import static java.util.stream.Collectors.joining;
import static libot.core.Constants.*;
import static libot.core.commands.exceptions.ExceptionHandler.ThrowableHandler.handleThrowable;
import static libot.core.ratelimits.RatelimitsManager.getRatelimits;
import static net.dv8tion.jda.api.Permission.MESSAGE_EMBED_LINKS;
import static org.apache.commons.lang3.StringUtils.abbreviate;
import static org.apache.commons.lang3.exception.ExceptionUtils.*;
import static org.apache.commons.text.WordUtils.capitalize;
import static org.slf4j.LoggerFactory.getLogger;

import java.util.Arrays;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import libot.core.commands.exceptions.runtime.*;
import libot.core.commands.exceptions.startup.*;
import libot.core.entities.CommandContext;
import libot.core.extensions.EmbedPrebuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.exceptions.*;

public class ExceptionHandler {

	private static final Logger LOG = getLogger(ExceptionHandler.class);

	public static final Throwable unpackThrowable(Throwable t) {
		Throwable unpacked = getRootCause(t);
		if (unpacked == null)
			return t;

		return unpacked;
	}

	@SuppressWarnings("null")
	public static void handle(CommandContext c, Throwable t) {
		var unpacked = unpackThrowable(t);
		try {
			boolean handled = handleThrowable(c, unpacked);
			if (!handled) {
				reportException(c, t);

				if (c.canTalk())
					c.replyf("// FAILURE //", """
						LiBot ran into an unknown error. If the issue persists, please report it via `%sfeedback`.""",
							 FAILURE, c.getEffectivePrefix());
			} else {
				LOG.debug("Automatically handled an exception in {}", c.getCommandName());
			}

			if (c.getCommandRatelimit() != 0 && shouldRatelimit(unpacked))
				getRatelimits(c.getCommand()).register(c.getUserIdLong());
		} catch (Exception e) {
			LOG.error("Got an exception while handling an error", e);
		}
	}

	@SuppressWarnings("null")
	public static void reportException(@Nonnull CommandContext c, @Nonnull Throwable throwable) {
		int maxTraceLength = Message.MAX_CONTENT_LENGTH - c.getCommandName().length() - 26;
		var report = """
			Failed to execute %s:```
			%s```""".formatted(c.getCommandName(), abbreviate(getStackTrace(throwable), maxTraceLength));

		c.messageSysadmins(p -> p.sendMessage(report));

		LOG.error("Unhandled exception in {}", c.getCommandName());
		LOG.error("", throwable);
	}

	public static class ThrowableHandler {

		private ThrowableHandler() {}

		public static boolean handleThrowable(CommandContext c, Throwable t) {
			if (t instanceof Error e) {
				return ErrorHandler.handleError(c, e);

			} else if (t instanceof Exception ex) {
				return PlainHandler.handleException(c, ex);

			} else {
				return true;
			}
		}

		private static class ErrorHandler {

			public static boolean handleError(CommandContext c, Error e) {
				if (e instanceof VirtualMachineError vme) {
					return VirtualMachineErrorHandler.handleVirtualMachineError(c, vme);

				} else {
					return false;
				}
			}

			private static class VirtualMachineErrorHandler {

				public static boolean handleVirtualMachineError(CommandContext c, VirtualMachineError e) {
					if (e instanceof OutOfMemoryError) {
						handleOutOfMemoryError(c);
						return true;

					} else {
						return false;
					}
				}

				private static void handleOutOfMemoryError(CommandContext c) {
					if (c.canTalk())
						c.reply("// MEM LOW //",
								"LiBot was unable to launch this command because it ran out of memory.", DISABLED);
				}

			}

		}

		private static class PlainHandler {

			public static boolean handleException(CommandContext c, Exception e) {
				if (e instanceof InterruptedException) {
					handleInterruptedException(c);
					return true;

				} else if (e instanceof RuntimeException re) {
					return RuntimeHandler.handleRuntimeException(c, re);

				} else {
					return false;
				}
			}

			private static void handleInterruptedException(CommandContext c) {
				if (c.canTalk())
					c.replyf("%s has been killed.", DISABLED, capitalize(c.getCommandName()));
			}

			private static class RuntimeHandler {

				public static boolean handleRuntimeException(CommandContext c, RuntimeException e) {
					if (e instanceof CommandException ce) {
						CommandExceptionHandler.handleCommandException(c, ce);
						return true;

					} else if (e instanceof ContinuumException ce) {
						handleContinuumException(c, ce);
						return false;

					} else if (e instanceof ErrorResponseException ere) {
						handleErrorResponseException(c, ere);
						return true;

					} else if (e instanceof IllegalArgumentException iae) {
						return IllegalArgumentHandler.handleIllegalArgumentException(c, iae);

					} else if (e instanceof PermissionException pe) {
						return PermissionExceptionHandler.handlePermissionException(c, pe);

					} else {
						return false;
					}
				}

				private static class CommandExceptionHandler {

					public static void handleCommandException(CommandContext c, CommandException e) {
						if (e instanceof CanceledException) {
							// Nothing to do

						} else if (e instanceof NumberOverflowException) {
							handleNumberOverflowException(c);

						} else if (e instanceof CommandStartupException cse) {
							CommandStartupExceptionHandler.handleCommandStartupException(c, cse);

						} else if (e instanceof TimeoutException) {
							handleTimeoutException(c);

						} else if (e instanceof TimeParseException) {
							handleTimeParseException(c);

						} else {
							e.sendMessage(c.getChannel());
						}
					}

					private static void handleNumberOverflowException(CommandContext c) {
						if (c.canTalk())
							c.reply("// INTEGER OVERFLOW //", """
								Looks like you provided a pretty large number. Don't do that.""", WARN);
					}

					private static class CommandStartupExceptionHandler {

						public static void handleCommandStartupException(CommandContext c, CommandStartupException e) {
							if (e instanceof CommandDisabledException cde) {
								handleCommandDisabledException(c, cde);

							} else if (e instanceof CommandPermissionsException cpe) {
								handleCommandPermissionsException(c, cpe);

							} else if (e instanceof NotDjException nde) {
								handleNotDjException(c, nde);

							} else if (e instanceof NotSysadminException) {
								handleNotSysadminException(c);

							} else if (e instanceof RatelimitedException re) {
								handleRatelimitedException(c, re);

							} else if (e instanceof UsageException) {
								handleUsageException(c);
							}
						}

						private static void handleCommandDisabledException(CommandContext c,
																		   CommandDisabledException e) {
							if (c.canTalk()) {
								var b = new EmbedPrebuilder(DISABLED);
								b.setTitle("// DISABLED //");
								if (e.isGlobal()) {
									b.setDescriptionf("""
										Command %s is currently globally disabled. \
										Please try again later.""", c.getCommand().getName());

								} else {
									b.setDescriptionf("""
										Command %s is currently disabled for this guild. \
										Please contact a moderator for further information.""",
													  c.getCommand().getName());
								}
								c.reply(b);
							}
						}

						private static void handleCommandPermissionsException(CommandContext c,
																			  CommandPermissionsException e) {
							var missing = e.getPermissions().stream().map(Permission::getName).toList();

							if (c.canTalk())
								c.replyf("// ACCESS DENIED //", """
									You need %s in order to be able to execute this command:
									%s""", missing.stream().collect(joining("\n", "- ", "")), WARN,
										 missing.size() == 1 ? "this permission" : "these permissions");
						}

						private static void handleNotDjException(CommandContext c, NotDjException e) {
							if (c.canTalk())
								c.replyf("// DJ-ONLY //", """
									Looks like this guild has set up a DJ role to be <@&%d> and it appears you do not \
									have that role. This means that you do not have permission to use music \
									commands.""", WARN, e.getDjRoleId());
						}

						private static void handleNotSysadminException(CommandContext c) {
							if (c.canTalk())
								c.replyf("// AUTHENTICATION REQUIRED //", """
									This command is reserved for LiBot administrators and is probably not what you \
									were looking for. To see the full list of commands, run `%shelp`""", DISABLED,
										 c.getEffectivePrefix());
						}

						private static void handleRatelimitedException(CommandContext c, RatelimitedException e) {
							if (c.canTalk()) {
								long seconds = e.getRemaining() / 1000;
								var time = new StringBuilder();
								if (seconds == 0) {
									time.append("less than a second");
								} else {
									time.append(seconds);
									time.append(" second");
									if (seconds != 1)
										time.append("s");
								}
								c.replyf("// RATELIMITED //", """
									Not so fast! Please wait %s before running %s.""", DISABLED, time,
										 c.getCommand().getName());
							}
						}

						private static void handleUsageException(CommandContext c) {
							if (c.canTalk())
								c.replyf("// USAGE INCORRECT //", """
									**Correct usage:** %s""", WARN, c.getCommandUsage());
						}

					}

					private static void handleTimeoutException(CommandContext c) {
						if (c.canTalk())
							c.reply("Response time has run out.", DISABLED);
					}

					private static void handleTimeParseException(CommandContext c) {
						if (c.canTalk())
							c.reply("// TIME EXCEPTED //", """
								LiBot expected a \
								[timestamp](https://libot.eu.org/doc/commands/parameter-types.html#time), but you \
								input something else.""", WARN);
					}

				}

				private static void handleContinuumException(CommandContext c, ContinuumException e) {
					if (c.canTalk())
						c.reply("""
							**// ANOMALY DETECTED //**,
							LiBot has run into a non-fatal unpredictable state. Command execution can not proceed.""");
					if (LOG.isErrorEnabled()) {
						LOG.error("Unpredictable state: {}", Arrays.toString(e.getDebug()));
						LOG.error("", e);
					}
				}

				private static void handleErrorResponseException(CommandContext c, ErrorResponseException e) {
					if (c.canTalk())
						c.replyf("// DISCORD FAILED US //", """
							Looks like Discord didn't like that for some reason.
							Error: %s""", FAILURE, e.getMeaning());
				}

				private static class IllegalArgumentHandler {

					public static boolean handleIllegalArgumentException(CommandContext c, IllegalArgumentException e) {
						if (e instanceof NumberFormatException) {
							handleNumberFormatException(c);
							return true;

						} else {
							return false;
						}
					}

					private static void handleNumberFormatException(CommandContext c) {
						if (c.canTalk())
							c.replyf("// Not a Number //", """
								Looks like you have provided text in a place where a number would fit best.""", WARN);
					}
				}

				private static class PermissionExceptionHandler {

					public static boolean handlePermissionException(CommandContext c, PermissionException e) {
						if (e instanceof HierarchyException) {
							handleHierarchyException(c);
							return true;

						} else if (e instanceof InsufficientPermissionException ipe) {
							handleInsufficientPermissionException(c, ipe);
							return true;

						} else {
							return false;
						}
					}

					private static void handleInsufficientPermissionException(CommandContext c,
																			  InsufficientPermissionException e) {
						if (e.getPermission() == MESSAGE_EMBED_LINKS) {
							c.reply("""
								```
								// EMBED REQUIRED //
								| You must grant LiBot the 'Embed Links' permission in order to be able to execute \
								| this command.```""");

						} else if (c.canTalk()) {
							c.replyf("// ACCESS DENIED //", """
								You must grant LiBot the '%s' permission in order to be able to execute this \
								command!""", WARN, e.getPermission().getName());
						}
					}

					private static void handleHierarchyException(CommandContext c) {
						if (c.canTalk())
							c.reply("// HIERARCHY ERROR //", """
								Looks like you tried to perform an audit action on a user that is in a role higher \
								than LiBot, which you can't. Please move LiBot's role up or demote that user!""", WARN);
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
