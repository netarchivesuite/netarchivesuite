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
package dk.netarkivet.archive.bitarchive.distribute;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.testutils.Serial;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by IntelliJ IDEA.
 * User: csr
 * Date: Apr 11, 2005
 * Time: 12:51:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class BatchEndedMessageTester extends TestCase{
    private static BatchEndedMessage bem;

    public void setUp(){
        bem = new BatchEndedMessage(Channels.getTheBamon(), "BAId", "MsgId", null);
        bem.setNoOfFilesProcessed(42);
        bem.setFilesFailed(Arrays.asList(new File[]{new File("failed")}));
    }

    public void tearDown(){
        bem = null;
    }

    public void testSerializability() throws IOException, ClassNotFoundException {
        BatchEndedMessage bem2 = (BatchEndedMessage) Serial.serial(bem);
        assertEquals("Serialization error", relevantState(bem), relevantState(bem2));
    }

    private String relevantState(BatchEndedMessage b) {
        return b.toString();
    }

}
