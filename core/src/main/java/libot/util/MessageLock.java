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
package libot.util;

import java.util.concurrent.TimeUnit;

import libot.core.command.exception.runtime.TimeoutException;

/**
 * A concurrent lock that blocks until another thread sends a message.
 *
 * @author Marko Zajc
 *
 * @param <M>
 *            message (value) type
 */
public class MessageLock<M> {

	private final Object lock;
	private boolean sent;

	private M message;
	private Throwable throwable;
	private boolean overrideTimeout;

	/**
	 * Creates a new instance if MessageLatch. After {@link #send(Object)} has been
	 * called, this object can not be reused.
	 */
	public MessageLock() {
		this.lock = new Object();
	}

	/**
	 * Sends the message.
	 *
	 * @param message
	 *            The message to send to the receiver.
	 */
	public void send(M message) {
		this.message = message;

		synchronized (this.lock) {
			this.lock.notifyAll();
			this.sent = true;
		}
	}

	/**
	 * Throws a new exception on the {@link #receive()} part.
	 *
	 * @param t
	 *            The {@link Throwable} to throw to the receiver.
	 */
	public void throwException(Throwable t) {
		this.throwable = t;

		synchronized (this.lock) {
			this.lock.notifyAll();
			this.sent = true;
		}
	}

	/**
	 * Awaits {@link #send(Object)} to be called or timeout to expire.
	 *
	 * @param timeout
	 *            timeout
	 * @param unit
	 *            timeout time unit
	 *
	 * @return message provided in {@link #send(Object)}
	 *
	 * @throws RuntimeException
	 *             if the sender has encountered a problem and has resorted to
	 *             {@link #throwException(Throwable)}
	 * @throws TimeoutException
	 *             if the time ran out
	 * @throws IllegalArgumentException
	 *             if timeout is less than 0
	 */
	public M receive(int timeout, TimeUnit unit) throws TimeoutException { // NOSONAR
		if (timeout < 0)
			throw new IllegalArgumentException("Timeout can't be less than 0!");

		long timeoutMillis;
		long targetMillis;

		if (timeout == 0) {
			timeoutMillis = -1;
			targetMillis = -1;

		} else {
			timeoutMillis = unit.toMillis(timeout);
			targetMillis = timeoutMillis == 0 ? -1 : System.currentTimeMillis() + timeoutMillis - 50;
		}

		if (this.sent || this.message != null)
			return this.message;

		synchronized (this.lock) {
			try {
				if (targetMillis < 0) {
					while (!this.sent)
						this.lock.wait();
				} else {
					while ((this.overrideTimeout || System.currentTimeMillis() < targetMillis) && !this.sent)
						this.lock.wait(this.overrideTimeout ? 0 : timeoutMillis);
				}
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
		}

		if (!this.sent)
			throw new TimeoutException();
		// If the message was actually sent before the timeout

		if (this.throwable != null)
			throw new RuntimeException(this.throwable);
		// If an exception has occurred

		return this.message;
	}

	/**
	 * Awaits {@link #send(Object)} to be called.
	 *
	 * @return message provided in <code>notify()</code>
	 *
	 * @throws RuntimeException
	 *             if the sender has encountered a problem and has resorted to
	 *             {@link #throwException(Throwable)}
	 */
	public M receive() {
		try {
			return this.receive(0, TimeUnit.MILLISECONDS);
		} catch (TimeoutException e) {
			throw new RuntimeException(e);
			// Can not occur
		}
	}

	/**
	 * Removes the timeout on all waiting threads. The threads in question will not be
	 * notified and will proceed to await the message without a timeout.
	 */
	public void overrideTimeout() {
		this.overrideTimeout = true;
	}

}
