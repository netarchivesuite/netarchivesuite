/*
 * #%L
 * Netarchivesuite - harvester - test
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
package dk.netarkivet.harvester.indexserver;

import dk.netarkivet.common.distribute.ChannelsTester;
import dk.netarkivet.common.distribute.JMSConnectionMockupMQ;
import dk.netarkivet.harvester.indexserver.IndexServerApplication;
import dk.netarkivet.testutils.ReflectUtils;
import dk.netarkivet.testutils.preconfigured.PreserveStdStreams;
import dk.netarkivet.testutils.preconfigured.PreventSystemExit;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;


public class IndexServerTester {
    
    @Before
    public void setUp() {
        ChannelsTester.resetChannels();
        JMSConnectionMockupMQ.useJMSConnectionMockupMQ();
    }
    
    @After
    public void tearDown() {
        JMSConnectionMockupMQ.clearTestQueues();
        ChannelsTester.resetChannels();
    }

    /**
     * Ensure, that the application dies if given the wrong input.
     */
    @Test
    public void testApplication() {
        ReflectUtils.testUtilityConstructor(IndexServerApplication.class);

        PreventSystemExit pse = new PreventSystemExit();
        PreserveStdStreams pss = new PreserveStdStreams(true);
        pse.setUp();
        pss.setUp();
        
        try {
            IndexServerApplication.main(new String[]{"ERROR"});
            fail("It should throw an exception ");
        } catch (SecurityException e) {
            // expected !
        }

        pss.tearDown();
        pse.tearDown();
        
        assertEquals("Should give exit code 1", 1, pse.getExitValue());
        assertTrue("Should tell that no arguments are expected.", 
                pss.getOut().contains("This application takes no arguments"));
    }
        
//    /**
//     * Test the basic class.
//     * TODO IT DOES SOMETHING SO THE PREVIOUS UNIT TESTS DOES NOT WORK 
//     */
//    public void testIndexServer() {
//        IndexServer.getInstance().cleanup();
//    }
}
