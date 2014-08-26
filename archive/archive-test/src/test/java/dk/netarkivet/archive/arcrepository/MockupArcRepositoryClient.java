/*
 * #%L
 * Netarchivesuite - archive - test
 * %%
 * Copyright (C) 2005 - 2014 The Royal Danish Library, the Danish State and University Library,
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
package dk.netarkivet.archive.arcrepository;

import java.util.ArrayList;
import java.util.List;

import javax.jms.Message;
import javax.jms.MessageListener;

import dk.netarkivet.archive.arcrepository.distribute.StoreMessage;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.distribute.JMSConnection;
import dk.netarkivet.common.distribute.JMSConnectionFactory;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.testutils.preconfigured.TestConfigurationIF;

/**
 *
 * @author tra
 */
public class MockupArcRepositoryClient implements TestConfigurationIF, MessageListener {
    /** Fail on all attempts to store these files */
    private List<String> failOnFiles;
    private int msgCount;
    private List<StoreMessage> storeMsgs;

    /**
     *
     */
    public void setUp() {
        failOnFiles = new ArrayList<String>();
        msgCount = 0;
        storeMsgs = new ArrayList<StoreMessage>();
        JMSConnectionFactory.getInstance().setListener(Channels.getTheRepos(), this);
    }

    /**
     *
     */
    public void tearDown() {
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
        JMSConnectionFactory.getInstance().removeListener(Channels.getTheRepos(), this);
    }

    /**
     *
     * @param file
     */
    public void failOnFile(String file) {
        failOnFiles.add(file);
    }

    /**
     *
     * @param message
     */
    public void onMessage(Message message) {
        msgCount++;
        StoreMessage sm = (StoreMessage) JMSConnection.unpack(message);
        storeMsgs.add(sm);
        if (failOnFiles.contains(sm.getArcfileName())) {
            sm.setNotOk("Simulating store failed.");
        }
        JMSConnectionFactory.getInstance().resend(sm, sm.getReplyTo());
    }

    /**
     *
     * @return
     */
    public int getMsgCount() {
        return msgCount;
    }

    /**
     *
     * @return
     */
    public List<StoreMessage> getStoreMsgs() {
        return storeMsgs;
    }
}
