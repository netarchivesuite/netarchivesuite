package dk.netarkivet;

import org.apache.log4j.Logger;

/**
 * This Logger should be used by all the test code to enable seperation of test logs from the applications logs
 */
public class TestLogger extends Logger {

	protected TestLogger(String name) {
		super("test" + name);
	}
}
