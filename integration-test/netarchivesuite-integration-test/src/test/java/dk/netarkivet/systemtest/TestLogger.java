package dk.netarkivet.systemtest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*dk.netarkivet.systemtestger should be used by all the test code to enable separation of test
 * logs from the applications logs.
 */
public class TestLogger {   
    private Logger log;

    public TestLogger(Class<?> logHandle) {
        log = LoggerFactory.getLogger(logHandle);
    }

    public void error(String msg) {
        log.error(msg);
    }

    public void debug(String string) {
        log.debug(string);
    }

    public void warn(String msg) {
        log.warn(msg);
    }

    public void info(String msg) {
        log.info(msg);
    }

    public void debug(StringBuffer sb) {
        log.debug(sb + "");
    }
}
