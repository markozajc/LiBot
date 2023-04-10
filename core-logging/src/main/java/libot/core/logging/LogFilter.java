package libot.core.logging;

import static ch.qos.logback.classic.Level.*;
import static ch.qos.logback.core.spi.FilterReply.*;

import org.reflections.Reflections;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe;
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAccessTokenTracker;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;
import com.sedmelluq.lava.common.natives.NativeLibraryLoader;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class LogFilter extends Filter<ILoggingEvent> {

	private static final String LOG_OGG_CONTAINER_PROBE = OggContainerProbe.class.getName();
	private static final String LOG_LATE = LocalAudioTrackExecutor.class.getName();
	private static final String LOG_WS_CLIENT = WebSocketClient.class.getName();
	private static final String LOG_REFLECTIONS = Reflections.class.getName();
	private static final String LOG_JDA = JDA.class.getName();
	private static final String LOG_YATT = YoutubeAccessTokenTracker.class.getName();
	private static final String LOG_NLL = NativeLibraryLoader.class.getName();

	private static final String MSG_CHUNK_ERROR = "Encountered exception trying to handle member chunk response";
	private static final String MSG_CANT_PARSE_OGG = "Failed to collect additional information on OGG stream.";
	private static final String MSG_PLAYBACK_ERROR = "Error in playback of ";
	private static final String MSG_REFLECTIONS_TOOK = "Reflections took ";
	private static final String MSG_CONNECTED = "Connected to WebSocket";
	private static final String MSG_LOADED = "Finished Loading!";
	private static final String MSG_LOGIN = "Login Successful!";
	private static final String MSG_UPDATING_YAT = "Updating YouTube visitor id";
	private static final String MSG_LOADING_LIBMPG = "Native library libmpg123-0: loading with filter";
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
		else if (levelIs(e, INFO) && loggerIs(e, LOG_NLL) && messageStartsWith(e, MSG_LOADING_LIBMPG))
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
