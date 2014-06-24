package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.Settings;

public class MockupJMS implements TestConfigurationIF {
    private String originalClass;

    @Override
    public void setUp() {
        originalClass = Settings.get(CommonSettings.JMS_BROKER_CLASS);
        Settings.set(CommonSettings.JMS_BROKER_CLASS, JMSConnectionMockupMQ.class.getName());
        JMSConnectionFactory.getInstance().cleanup();
        JMSConnectionMockupMQ.clearTestQueues();
    }

    @Override
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
        try {
            JMSConnectionFactory.getInstance().cleanup();
        } catch (Exception e) {
            //just ignore it
        }
        Settings.set(CommonSettings.JMS_BROKER_CLASS, originalClass);
    }

    public JMSConnection getJMSConnection() {
        return JMSConnectionFactory.getInstance();
    }
}
