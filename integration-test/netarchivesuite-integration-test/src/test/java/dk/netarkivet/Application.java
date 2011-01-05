package dk.netarkivet;

public class Application {
	private final String machine;
	private final String application;
	private final String priority;
	
	public Application(String machine, String application, String priority) {
		super();
		this.machine = machine;
		this.application = application;
		this.priority = priority;
	}

	@Override
	public String toString() {
		return "Application [machine=" + machine + ", application="
				+ application + ", priority=" + priority + "]";
	}
}
