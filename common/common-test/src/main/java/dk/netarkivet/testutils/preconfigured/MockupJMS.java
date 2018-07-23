/*
 * #%L
 * Netarchivesuite - common - test
 * %%
 * Copyright (C) 2005 - 2018 The Royal Danish Library, 
 *             the National Library of France and the Austrian National Library.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 2.1 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */
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
            // just ignore it
        }
        Settings.set(CommonSettings.JMS_BROKER_CLASS, originalClass);
    }

    public JMSConnection getJMSConnection() {
        return JMSConnectionFactory.getInstance();
    }
}
