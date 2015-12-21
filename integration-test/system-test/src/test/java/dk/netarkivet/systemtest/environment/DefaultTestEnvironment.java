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
    private String deployconf;

    public DefaultTestEnvironment(String testX, String mailreceivers, String timestamp, int guiPort,
            String guiHost, String deployconf) {
        this.testX = testX;
        this.mailreceivers = mailreceivers;
        this.timestamp = timestamp;
        this.guiPort = guiPort;
        this.guiHost = guiHost;
        this.deployconf = deployconf;
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

    @Override public String getDeployConfig() {
        return System.getProperty("systemtest.deployconf", deployconf);
    }

    @Override public String getH3Zip() {
        return System.getProperty("systemtest.h3zip");
    }

}
