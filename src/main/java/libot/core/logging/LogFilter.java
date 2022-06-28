package libot.core.logging;

import static ch.qos.logback.classic.Level.*;
import static ch.qos.logback.core.spi.FilterReply.*;

import org.reflections.Reflections;

import com.sedmelluq.discord.lavaplayer.container.ogg.OggContainerProbe;
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.FilterReply;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.internal.requests.WebSocketClient;

public class LogFilter extends Filter<ILoggingEvent> {

	private static final String OGG_CONTAINER_PROBE = OggContainerProbe.class.getName();
	private static final String LATE = LocalAudioTrackExecutor.class.getName();
	private static final String WS_CLIENT = WebSocketClient.class.getName();
	private static final String REFLECTIONS = Reflections.class.getName();
	private static final String JDA = JDA.class.getName();

	private static final String MESSAGE_CHUNK_ERROR = "Encountered exception trying to handle member chunk response";
	private static final String MESSAGE_CANT_PARSE_OGG = "Failed to collect additional information on OGG stream.";
	private static final String MESSAGE_PLAYBACK_ERROR = "Error in playback of ";
	private static final String MESSAGE_REFLECTIONS_TOOK = "Reflections took ";
	private static final String MESSAGE_CONNECTED = "Connected to WebSocket";
	private static final String MESSAGE_LOADED = "Finished Loading!";
	private static final String MESSAGE_LOGIN = "Login Successful!";

	@Override
	public FilterReply decide(ILoggingEvent e) {
		if (loggerIs(e, WS_CLIENT) && messageIs(e, MESSAGE_CHUNK_ERROR) && levelIs(e, ERROR))
			return DENY;
		else if (loggerIs(e, WS_CLIENT) && messageIs(e, MESSAGE_CONNECTED) && levelIs(e, INFO))
			return DENY;
		else if (loggerIs(e, JDA) && messageIs(e, MESSAGE_LOGIN) && levelIs(e, INFO))
			return DENY;
		else if (loggerIs(e, JDA) && messageIs(e, MESSAGE_LOADED) && levelIs(e, INFO))
			return DENY;
		else if (loggerIs(e, OGG_CONTAINER_PROBE) && messageIs(e, MESSAGE_CANT_PARSE_OGG) && levelIs(e, WARN))
			return DENY;
		else if (loggerIs(e, LATE) && messageStartsWith(e, MESSAGE_PLAYBACK_ERROR) && levelIs(e, ERROR))
			return DENY;
		else if (loggerIs(e, REFLECTIONS) && e.getMessage().startsWith(MESSAGE_REFLECTIONS_TOOK) && levelIs(e, INFO))
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
