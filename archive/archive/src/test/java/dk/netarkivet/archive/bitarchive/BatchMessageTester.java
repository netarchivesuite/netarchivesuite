/* File:    $Id$
* Version: $Revision$
* Date:    $Date$
* Author:  $Author$
*
* The Netarchive Suite - Software to harvest and preserve websites
* Copyright 2004-2007 Det Kongelige Bibliotek and Statsbiblioteket, Denmark
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
package dk.netarkivet.archive.bitarchive;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import junit.framework.TestCase;

import dk.netarkivet.archive.bitarchive.distribute.BatchMessage;
import dk.netarkivet.common.Settings;
import dk.netarkivet.common.distribute.ChannelID;
import dk.netarkivet.common.utils.arc.FileBatchJob;
import dk.netarkivet.testutils.Serial;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Mar 3, 2005
 * Time: 10:29:56 AM
 * To change this template use File | Settings | File Templates.
 *
 */
public class BatchMessageTester extends TestCase {
    // Need a couple of queues for the constructors for the messages
    private ChannelID q1 = TestInfo.QUEUE_1;
    private static FileBatchJob job;

    /**
     *
     */
    public void setUp() throws Exception {
        job = new TestBatchJob();
        super.setUp();
    }

    /**
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public void testBatchMessageSerializable() throws IOException, ClassNotFoundException {
        BatchMessage bm = new BatchMessage(q1, job, Settings.get(Settings.ENVIRONMENT_THIS_LOCATION));
        BatchMessage bm2 = (BatchMessage) Serial.serial(bm);
        assertEquals("Serializability failure for BatchMessage", relevantState(bm), relevantState(bm2));
    }

    private String relevantState(BatchMessage bm){
        return bm.toString();
    }

    private static class TestBatchJob extends FileBatchJob{

        public void initialize(OutputStream os) {
        }
        public void finish(OutputStream os) {
        }
        public boolean processFile(File file, OutputStream os) {
            return true;
        }
        public String toString(){return "a string";}
    }



}
