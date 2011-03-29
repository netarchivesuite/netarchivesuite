/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2010 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package dk.netarkivet.testutils.preconfigured;

import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.common.utils.Settings;

public class MockupJMS implements TestConfigurationIF {
    private String originalClass;

    public void setUp() {
        originalClass = Settings.get(CommonSettings.JMS_BROKER_CLASS);
        Settings.set(CommonSettings.JMS_BROKER_CLASS, JMSConnectionMockupMQ.class.getName());
        JMSConnectionFactory.getInstance().cleanup();
        JMSConnectionMockupMQ.clearTestQueues();
    }

    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
        try {
            JMSConnectionFactory.getInstance().cleanup();
        } catch (Exception e) {
            //just ignore it
        }
        Settings.set(CommonSettings.JMS_BROKER_CLASS, originalClass);
    }

}
