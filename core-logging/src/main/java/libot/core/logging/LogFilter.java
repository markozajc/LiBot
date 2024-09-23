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
package libot.core.logging;

import static ch.qos.logback.classic.Level.*;
import static ch.qos.logback.core.spi.FilterReply.*;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;

public class LogFilter extends Filter<ILoggingEvent> {

	// javadoc links are used as a hack to give me a warning if a class name changes
	// while avoiding runtime dependencies introduced by reflection (using
	// X.class.getName())
	// note that these fields are public because eclipse won't check the links'
	// correctness by default otherwise

	/**
	 * {@link com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe}
	 */
	public static final String LOG_OGG_CONTAINER_PROBE =
		"com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe";
	/**
	 * {@link com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor}
	 */
	public static final String LOG_LATE = "com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor";
	/**
	 * {@link net.dv8tion.jda.internal.requests.WebSocketClient}
	 */
	public static final String LOG_WS_CLIENT = "net.dv8tion.jda.internal.requests.WebSocketClient";
	/**
	 * {@link org.reflections.Reflections}
	 */
	public static final String LOG_REFLECTIONS = "org.reflections.Reflections";
	/**
	 * {@link net.dv8tion.jda.api.JDA}
	 */
	public static final String LOG_JDA = "net.dv8tion.jda.api.JDA";
	/**
	 * {@link com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAccessTokenTracker}
	 */
	public static final String LOG_YATT = "com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAccessTokenTracker";
	/**
	 * {@link com.sedmelluq.lava.common.natives.NativeLibraryLoader}
	 */
	public static final String LOG_NLL = "com.sedmelluq.lava.common.natives.NativeLibraryLoader";

	private static final String MSG_CHUNK_ERROR = "Encountered exception trying to handle member chunk response";
	private static final String MSG_CANT_PARSE_OGG = "Failed to collect additional information on OGG stream.";
	private static final String MSG_PLAYBACK_ERROR = "Error in playback of ";
	private static final String MSG_REFLECTIONS_TOOK = "Reflections took ";
	private static final String MSG_CONNECTED = "Connected to WebSocket";
	private static final String MSG_LOADED = "Finished Loading!";
	private static final String MSG_LOGIN = "Login Successful!";
	private static final String MSG_UPDATING_YAT = "Updating YouTube visitor id";
	private static final String MSG_NATIVE_LIBRARY = "Native library";
	private static final String MSG_LOADING_CONNECTOR = "Native library connector: ";

	@Override
	public FilterReply decide(ILoggingEvent e) {
		if (levelIs(e, ERROR) && loggerIs(e, LOG_WS_CLIENT) && messageIs(e, MSG_CHUNK_ERROR))
			return DENY;
		else if (levelIs(e, ERROR) && loggerIs(e, LOG_LATE) && messageStartsWith(e, MSG_PLAYBACK_ERROR))
			return DENY;
		else if (levelIs(e, WARN) && loggerIs(e, LOG_OGG_CONTAINER_PROBE) && messageIs(e, MSG_CANT_PARSE_OGG))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_WS_CLIENT) && messageIs(e, MSG_CONNECTED))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_JDA) && messageIs(e, MSG_LOGIN))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_JDA) && messageIs(e, MSG_LOADED))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_REFLECTIONS) && messageStartsWith(e, MSG_REFLECTIONS_TOOK))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_YATT) && messageStartsWith(e, MSG_UPDATING_YAT))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_NLL) && messageStartsWith(e, MSG_NATIVE_LIBRARY))
			return DENY;
		else if (levelIs(e, INFO) && loggerIs(e, LOG_NLL) && messageStartsWith(e, MSG_LOADING_CONNECTOR))
			return DENY;
		else
			return NEUTRAL;
	}

	private static boolean messageStartsWith(ILoggingEvent e, String start) {
		return e.getFormattedMessage().startsWith(start);
	}

	private static boolean messageIs(ILoggingEvent e, String message) {
		return e.getFormattedMessage().equals(message);
	}

	private static boolean loggerIs(ILoggingEvent e, String name) {
		return name.equals(e.getLoggerName());
	}

	private static boolean levelIs(ILoggingEvent event, Level level) {
		return event.getLevel() == level;
	}

}
