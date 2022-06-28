package libot.core.logging;

import static ch.qos.logback.classic.Level.*;
import static ch.qos.logback.core.pattern.color.ANSIConstants.*;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.pattern.color.ForegroundCompositeConverterBase;

public class LevelHighlight extends ForegroundCompositeConverterBase<ILoggingEvent> {

	@Override
	protected String getForegroundColorCode(ILoggingEvent event) {
		return switch (event.getLevel().toInt()) {
			case ERROR_INT -> BOLD + RED_FG;
			case WARN_INT -> BOLD + YELLOW_FG;
			case INFO_INT -> BLUE_FG;
			case DEBUG_INT -> BOLD + BLACK_FG;
			default -> DEFAULT_FG;
		};
	}

}
