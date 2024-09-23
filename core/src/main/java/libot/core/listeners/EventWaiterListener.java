//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2024 Marko Zajc
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free Software
 * Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package libot.core.listeners;

import static java.lang.String.format;
import static java.lang.Thread.interrupted;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import javax.annotation.*;

import org.apache.commons.lang3.tuple.*;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;

import libot.core.commands.exceptions.runtime.TimeoutException;
import libot.utils.MessageLock;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;

public class EventWaiterListener implements EventListener {

	private static final String FORMAT_WRONG_TYPE = "Something went wrong; onEvent() returned %s instead of %s";

	// @formatter:off 💀
	private final MutableIntObjectMap<
							Triple<
								Predicate<GenericEvent>,
								MessageLock<GenericEvent>,
								Predicate<Void>
							>
						> eventWaiters =
	// @formatter:on
		IntObjectMaps.mutable.<Triple<Predicate<GenericEvent>, MessageLock<GenericEvent>, Predicate<Void>>>empty(); // death

	private final AtomicInteger counter = new AtomicInteger();
	private final Object mutex = new Object();

	/**
	 * Pauses the current thread and awaits a certain event.<br>
	 * <strong>This will throw a {@link IllegalArgumentException} if you do not access it
	 * from a command thread</strong>
	 *
	 * @param <T>
	 *            type of event to await
	 *
	 * @param predicate
	 *            predicate that will be tested before the event is returned. The event
	 *            is ignored if testing the predicate returns false
	 * @param eventClass
	 *            the event class (eg. MessageReceivedEvent.class)
	 * @param nullableCleanupPredicate
	 *            a predicate that will also be tested upon cleanup asides from the
	 *            predicate that tests if command's author and channel still exist. No
	 *            predicate will take place if this is {@code null}
	 * @param timeout
	 *            timeout (0 for no timeout)
	 * @param timeoutUnit
	 *            timeout init (can be <code>null</code> if timeout is 0)
	 *
	 * @return the event
	 *
	 * @throws TimeoutException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public <T extends GenericEvent> T awaitEvent(@Nonnull Predicate<GenericEvent> predicate,
												 @Nullable Predicate<Void> nullableCleanupPredicate, int timeout,
												 @Nullable TimeUnit timeoutUnit,
												 @Nonnull Class<T> eventClass) throws TimeoutException,
																			   InterruptedException {
		var cleanupPredicate = nullableCleanupPredicate;
		if (cleanupPredicate == null)
			cleanupPredicate = p -> false;

		var lock = new MessageLock<GenericEvent>();
		int ticket = this.counter.getAndIncrement();
		Predicate<GenericEvent> isInstance = eventClass::isInstance;
		synchronized (this.mutex) {
			this.eventWaiters.put(ticket, new ImmutableTriple<>(isInstance.and(predicate), lock, cleanupPredicate));
		}
		GenericEvent awaited = lock.receive(timeout, timeoutUnit);
		synchronized (this.mutex) {
			this.eventWaiters.remove(ticket);
		}
		sanityCheck(eventClass, awaited);
		return (T) awaited;
	}

	private static void sanityCheck(@Nonnull Class<?> clazz,
									@Nullable GenericEvent awaited) throws InterruptedException {
		if (awaited == null) {
			if (interrupted())
				throw new InterruptedException();
			else
				throw new IllegalStateException("Received event was null.");
		}

		if (!clazz.getSuperclass().isInstance(awaited))
			throw new IllegalStateException(format(FORMAT_WRONG_TYPE, awaited.getClass().getName(), clazz.getName()));
	}

	@Override
	public void onEvent(GenericEvent event) {
		synchronized (this.mutex) {
			for (var e : this.eventWaiters.values()) {
				if (e.getLeft().test(event))
					e.getMiddle().send(event);
			}
		}
	}

}
