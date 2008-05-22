package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;

public class MockupJMS implements TestConfigurationIF {
    private String originalClass;

    public void setUp() {
        originalClass = Settings.get(Settings.JMS_BROKER_CLASS);
        Settings.set(Settings.JMS_BROKER_CLASS,
                     JMSConnectionTestMQ.class.getName());
        JMSConnectionFactory.getInstance().cleanup();
        JMSConnectionTestMQ.clearTestQueues();
    }

    public void tearDown() {
        JMSConnectionTestMQ.clearTestQueues();
        try {
            JMSConnectionFactory.getInstance().cleanup();
        } catch (Exception e) {
            //just ignore it
        }
        Settings.set(Settings.JMS_BROKER_CLASS, originalClass);
    }

}
