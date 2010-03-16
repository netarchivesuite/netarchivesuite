/* File:        $Id$
 * Revision:    $Revision$
 * Author:      $Author$
 * Date:        $Date$
 *
 * The Netarchive Suite - Software to harvest and preserve websites
 * Copyright 2004-2009 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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

import javax.jms.Message;
import javax.jms.MessageListener;
import java.util.ArrayList;
import java.util.List;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;

public class MockupArcRepositoryClient implements TestConfigurationIF, MessageListener {
    /** Fail on all attempts to store these files */
    private List<String> failOnFiles;
    private int msgCount;
    private List<StoreMessage> storeMsgs;

    public void setUp() {
        failOnFiles = new ArrayList<String>();
        msgCount = 0;
        storeMsgs = new ArrayList<StoreMessage>();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), this);
    }

    public void tearDown() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), this);
    }

    public void failOnFile(String file) {
        failOnFiles.add(file);
    }

    public void onMessage(Message message) {
        msgCount++;
        StoreMessage sm = (StoreMessage) JMSConnection.unpack(message);
        storeMsgs.add(sm);
        if(failOnFiles.contains(sm.getArcfileName())) {
            sm.setNotOk("Simulating store failed.");
        }
        JMSConnectionFactory.getInstance().resend(sm, sm.getReplyTo());
    }

    public int getMsgCount() {
        return msgCount;
    }

    public List<StoreMessage> getStoreMsgs() {
        return storeMsgs;
    }
}

