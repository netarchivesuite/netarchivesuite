package dk.netarkivet.common.tools;

import dk.netarkivet.common.distribute.JMSConnectionFactory;

/**
 * Used to check if firewall ports are open and
 * if the JMS broker is up and responding.
 *
 */
public class JMSBroker {

    /**
     * Initializes a JMSConnection. If everything is fine it prints
     * "success" to the console.
     *
     * @param args Takes no arguments
     */
    public static void main(final String[] args) {
        JMSConnectionFactory.getInstance().cleanup();
        System.out.println("success");
        Runtime.getRuntime().exit(0);
    }
}
