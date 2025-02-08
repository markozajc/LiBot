//SPDX-License-Identifier: AGPL-3.0-only
/*
 * Copyright (C) 2017-2025 Marko Zajc
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
package libot.management;

import static java.lang.Integer.toUnsignedString;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.joining;
import static libot.core.Constants.VERSION;
import static org.slf4j.LoggerFactory.getLogger;

import java.io.IOException;
import java.net.*;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import org.slf4j.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import libot.core.shred.Shredder;
import libot.core.shred.Shredder.Shred;

public class ManagementServer {

	private static final Logger LOG = getLogger(ManagementServer.class);
	private static final AtomicInteger THREAD_NAME_COUNTER = new AtomicInteger();

	@Nonnull private final Shredder shredder;
	private final int port;

	public ManagementServer(@Nonnull Shredder shredder, int port) {
		this.shredder = shredder;
		this.port = port;
	}

	@SuppressFBWarnings(value = "UNENCRYPTED_SERVER_SOCKET", justification = "for local transport only")
	public void start() throws IOException {
		@SuppressWarnings("resource")
		var server = new ServerSocket(this.port);
		new Thread(() -> {
			try {
				startAccepting(server);
			} catch (IOException e) {
				LOG.error("The management server crashed", e);
			}
		}, "management-server").start();
	}

	@SuppressWarnings("null")
	private void startAccepting(ServerSocket server) throws IOException {
		try (server) {
			LOG.info("Management server is running on port {}", this.port);
			while (!Thread.interrupted()) {
				@SuppressWarnings("resource")
				var socket = server.accept();
				new Thread(() -> {
					try (socket) {
						accept(socket);
					} catch (IOException e) {
						LOG.error("Couldn't handle a connection", e);
					}
				}, format("management-socket-%s", toUnsignedString(THREAD_NAME_COUNTER.getAndIncrement()))).start();
			}
		}
	}

	@SuppressWarnings("resource")
	private void accept(@Nonnull Socket socket) throws IOException {
		try (var s = new Scanner(socket.getInputStream(), UTF_8)) {
			if (!s.hasNextLine())
				return;
			var resp = stream(s.nextLine().split(",")).map(this::handleCommand).collect(joining(","));
			socket.getOutputStream().write(resp.getBytes(UTF_8));
		}
	}

	@Nonnull
	@SuppressWarnings("null")
	private String handleCommand(@Nonnull String command) {
		return switch (command) {
			case "BESTID" -> this.shredder.getShreds()
				.stream()
				.map(Shred::jda)
				.sorted((j1, j2) -> Long.compare(j1.getGuildCache().size(), j2.getGuildCache().size()))
				.findFirst()
				.map(j -> {
					if (j == null)
						return "0";
					else
						return j.getSelfUser().getId();
				})
				.orElse("null");
			case "GUILDS" -> Long.toString(this.shredder.getGuildCount());
			case "VERSION" -> VERSION;
			default -> "ERR";
		};
	}

}
