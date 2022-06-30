package libot.runner;

public class Main {

	// Due to how Eclipse handles Maven projects, and because I can't simply import
	// modules with runtime scope in libot-core (it would still cause a cyclic dependency
	// in Maven), a top-level project importing all projects with runtime scope (except
	// libot-core, which the main function calls) is required for execution from within
	// the IDE itself. Deal with it.
	public static void main(String[] argv) throws Exception {
		libot.Main.main(argv);
	}

}
