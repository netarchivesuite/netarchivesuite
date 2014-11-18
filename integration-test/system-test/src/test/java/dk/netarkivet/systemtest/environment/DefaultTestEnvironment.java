package dk.netarkivet.systemtest.environment;

/**
 * A partial implementation of the test environment which reads the GUI Host + Port,
 * Timestamp and Mailreceivers from system variables.
 */
public class DefaultTestEnvironment implements TestEnvironment {

    private String testX;
    private String guiHost;
    private int guiPort;
    private String timestamp;
    private String mailreceivers;

    public DefaultTestEnvironment(String testX, String mailreceivers, String timestamp, int guiPort,
            String guiHost) {
        this.testX = testX;
        this.mailreceivers = mailreceivers;
        this.timestamp = timestamp;
        this.guiPort = guiPort;
        this.guiHost = guiHost;
    }

    @Override public String getTESTX() {
        return testX;
    }

    @Override public String getGuiHost() {
        return System.getProperty("systemtest.host", guiHost);
    }

    @Override public int getGuiPort() {
        return Integer.parseInt(System.getProperty("systemtest.port", guiPort+""));
    }

    @Override public String getTimestamp() {
        return System.getProperty("systemtest.timestamp", timestamp);
    }

    @Override public String getMailreceivers() {
        return System.getProperty("systemtest.mailreceivers", mailreceivers);
    }
}
