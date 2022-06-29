package libot.core.data;

import static libot.core.Constants.ENV_DATA_TYPE;
import static libot.utils.Utilities.getenvOrThrow;

import org.reflections.Reflections;
import org.reflections.scanners.Scanners;

public class DataManagerFactory {

	public static DataManager fromEnvironment() throws ReflectiveOperationException {
		var className = getenvOrThrow(ENV_DATA_TYPE).toLowerCase() + "datamanager";
		var reflections = new Reflections(libot.core.data.impl.Anchor.class.getPackageName());

		var clazz = reflections.get(Scanners.SubTypes.of(DataManager.class).asClass())
			.stream()
			.filter(c -> className.equals(c.getSimpleName().toLowerCase()))
			.findAny()
			.orElseThrow(() -> new IllegalStateException("Nonexistent requested data manager: " + className));

		return (DataManager) clazz.getDeclaredConstructor().newInstance();
	}

	private DataManagerFactory() {}

}
