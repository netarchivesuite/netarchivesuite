/*
 * #%L
 * Netarchivesuite - archive - test
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
package dk.netarkivet.archive.bitarchive;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.CommonSettings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.common.utils.Settings;
import dk.netarkivet.common.utils.batch.FileBatchJob;
import dk.netarkivet.testutils.Serial;

/**
 * Created by IntelliJ IDEA. User: csr Date: Mar 3, 2005 Time: 10:29:56 AM To change this template use File | Settings |
 * File Templates.
 */
@SuppressWarnings({"serial"})
public class BatchMessageTester {
    // Need a couple of queues for the constructors for the messages
    //private ChannelID q1 = TestInfo.QUEUE_1;
    private static FileBatchJob job;

    @Before
    public void setUp() throws Exception {
        job = new TestBatchJob();
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @Test
    public void testBatchMessageSerializable() throws IOException, ClassNotFoundException {
    	ChannelID q1 = Channels.getAnyBa();
        BatchMessage bm = new BatchMessage(q1, job, Settings.get(CommonSettings.USE_REPLICA_ID));
        BatchMessage bm2 = (BatchMessage) Serial.serial(bm);
        assertEquals("Serializability failure for BatchMessage", relevantState(bm), relevantState(bm2));
    }

    private String relevantState(BatchMessage bm) {
        return bm.toString();
    }

    private static class TestBatchJob extends FileBatchJob {

        public void initialize(OutputStream os) {
        }

        public void finish(OutputStream os) {
        }

        public boolean processFile(File file, OutputStream os) {
            return true;
        }

        public String toString() {
            return "a string";
        }
    }

}
