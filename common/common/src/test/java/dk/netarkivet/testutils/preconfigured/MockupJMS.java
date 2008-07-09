package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionTestMQ;
import dk.netarkivet.common.utils.Settings;

public class MockupJMS implements TestConfigurationIF {
    private String originalClass;

    public void setUp() {
        originalClass = Settings.get(CommonSettings.JMS_BROKER_CLASS);
        Settings.set(CommonSettings.JMS_BROKER_CLASS, JMSConnectionTestMQ.class.getName());
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
        Settings.set(CommonSettings.JMS_BROKER_CLASS, originalClass);
    }

}
