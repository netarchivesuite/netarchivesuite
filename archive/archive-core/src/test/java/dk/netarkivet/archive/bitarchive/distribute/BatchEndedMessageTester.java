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
package dk.netarkivet.archive.bitarchive.distribute;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import dk.netarkivet.common.distribute.Channels;
import dk.netarkivet.testutils.Serial;

public class BatchEndedMessageTester {
    private static BatchEndedMessage bem;

    @Before
    public void setUp() {
        bem = new BatchEndedMessage(Channels.getTheBamon(), "BAId", "MsgId", null);
        bem.setNoOfFilesProcessed(42);
        bem.setFilesFailed(Arrays.asList(new File[] {new File("failed")}));
    }

    @After
    public void tearDown() {
        bem = null;
    }

    @Test
    public void testSerializability() throws IOException, ClassNotFoundException {
        BatchEndedMessage bem2 = (BatchEndedMessage) Serial.serial(bem);
        assertEquals("Serialization error", relevantState(bem), relevantState(bem2));
    }

    private String relevantState(BatchEndedMessage b) {
        return b.toString();
    }

}
