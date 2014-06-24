package dk.netarkivet.common.distribute;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.utils.SettingsFactory;

/** Factory for JMS connection. */
public class JMSConnectionFactory {

    /** Get the JMS Connection singleton instance defined by
     * Settings.JMS_BROKER_CLASS.
     * @return The class defined by Settings.JMS_BROKER_CLASS implementing
     * JMSConnection. 
     */
    public static JMSConnection getInstance() {
        return SettingsFactory.getInstance(CommonSettings.JMS_BROKER_CLASS);
    }
}
